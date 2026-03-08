package com.github.sundramsinghdev007.stringfilelocalization.action

import com.android.tools.idea.projectsystem.SourceProviderManager
import com.github.sundramsinghdev007.stringfilelocalization.MyBundle
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile
import org.jetbrains.android.facet.AndroidFacet
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.JBColor
import com.intellij.ui.SearchTextField
import java.awt.*
import javax.swing.*
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class TranslateStringsAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
//        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        // 1. Try to get file from current editor/selection
        var psiFile = e.getData(CommonDataKeys.PSI_FILE)

        // 2. Fallback: If no file open, find the first strings.xml in the project
        if (psiFile == null || !psiFile.name.endsWith(".xml")) {
            psiFile = findAnyStringsXml(project)
        }

        if (psiFile == null) {
            Messages.showErrorDialog(project, MyBundle.message("error.facet.missing"), "Error")
            return
        }
        val module = ModuleUtilCore.findModuleForPsiElement(psiFile) ?: return
// Check for Android Facet
        val facet = AndroidFacet.getInstance(module)
        if (facet == null) {
            Messages.showErrorDialog(project, MyBundle.message("error.facet.missing"), "Error")
            return
        }
        val mainStrings = findMainStringsXml(facet) ?: return
        val originalTags = mainStrings.rootTag?.findSubTags("string") ?: emptyArray()

        val langCode = Messages.showInputDialog(
            project,
            MyBundle.message("dialog.select.lang.message"),
            MyBundle.message("dialog.select.lang.title"),
            null
        ) ?: return

        val existingKeys = getExistingKeys(mainStrings, langCode)

        // 1. Data & Row Setup
        val rowMap = mutableMapOf<String, TranslationRow>()
        originalTags.forEach { tag ->
            val name = tag.getAttributeValue("name") ?: ""
            val englishValue = tag.value.text
            val isTranslated = existingKeys.contains(name)
            val isTranslatable = tag.getAttributeValue("translatable")?.toBoolean() ?: true
            rowMap[name] = TranslationRow(name, englishValue, isTranslated, isTranslatable)
        }

        // 2. UI Components
        val mainPanel = JPanel(BorderLayout(0, 5))
        val searchField = SearchTextField()
        val selectAllBtn = JButton(MyBundle.message("button.select.all"))
        val selectNoneBtn = JButton(MyBundle.message("button.deselect.all"))
        val forceUpdateCb = JBCheckBox(MyBundle.message("checkbox.force.update"), false).apply {
            toolTipText = MyBundle.message("checkbox.force.update.tooltip")
        }
        val statsLabel = JBLabel().apply { foreground = JBColor.GRAY; font = font.deriveFont(11f) }
        val listPanel = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS) }

        fun updateFooter() {
            val selected = rowMap.values.count { it.checkBox.isSelected }
            statsLabel.text = MyBundle.message("status.footer", selected, rowMap.size, existingKeys.size)
        }

        // 3. Filtering & Selection Logic
        fun updateList() {
            listPanel.removeAll()
            val query = searchField.text.lowercase()
            rowMap.filter { it.key.lowercase().contains(query) || it.value.englishText.lowercase().contains(query) }
                .forEach { listPanel.add(it.value.panel) }
            listPanel.revalidate(); listPanel.repaint()
        }

        selectAllBtn.addActionListener {
            val force = forceUpdateCb.isSelected
            rowMap.values.forEach { if (it.isTranslatable) it.checkBox.isSelected = (force || !it.isTranslated) }
            updateFooter()
        }

        selectNoneBtn.addActionListener {
            rowMap.values.forEach { it.checkBox.isSelected = false }
            forceUpdateCb.isSelected = false // Reset force update for safety
            updateFooter()
        }


        // Attach footer updates to every checkbox
        rowMap.values.forEach { it.checkBox.addActionListener { updateFooter() } }

        searchField.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = updateList()
            override fun removeUpdate(e: DocumentEvent) = updateList()
            override fun changedUpdate(e: DocumentEvent) = updateList()
        })

        // 4. Assembly
        val toolbar = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0)).apply {
            add(selectAllBtn); add(selectNoneBtn); add(forceUpdateCb)
        }
        val topPanel = JPanel(GridLayout(0, 1, 0, 5)).apply {
            add(searchField); add(toolbar)
        }
        val footerPanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(5, 5, 0, 0); add(statsLabel, BorderLayout.WEST)
        }

        mainPanel.add(topPanel, BorderLayout.NORTH)
        mainPanel.add(JBScrollPane(listPanel).apply { preferredSize = Dimension(600, 400) }, BorderLayout.CENTER)
        mainPanel.add(footerPanel, BorderLayout.SOUTH)

        updateList()
        updateFooter()

        val builder = DialogBuilder(project).apply {
            setCenterPanel(mainPanel); setTitle(
            MyBundle.message(
                "dialog.title",
                langCode
            )
        )
        }

        if (builder.showAndGet()) {
            val selectedToTranslate = rowMap.filter { it.value.checkBox.isSelected }
            if (selectedToTranslate.isEmpty()) {
                Messages.showErrorDialog(
                    project,
                    MyBundle.message("error.no.selection"),
                    MyBundle.message("dialog.select.lang.title") // Reusing the title key
                )
                return
            }
            ProgressManager.getInstance()
                .run(object : Task.Backgroundable(project, MyBundle.message("progress.translating", langCode), false) {
                    override fun run(indicator: ProgressIndicator) {
                        val newTranslations = mutableMapOf<String, String>()
                        val entries = selectedToTranslate.entries.toList()

                        entries.forEachIndexed { index, entry ->
                            indicator.fraction = (index + 1).toDouble() / entries.size
                            indicator.text = MyBundle.message("progress.current.key", entry.key)
                            newTranslations[entry.key] = translateText(entry.value.englishText, langCode)
                        }

                        WriteCommandAction.runWriteCommandAction(project) {
                            saveAndMergeTranslations(project, mainStrings, langCode, newTranslations)
                        }
                    }
                })
        }
    }

    private fun translateText(text: String, targetLang: String): String {
        return try {
            val urlString =
                "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$targetLang&dt=t&q=${
                    java.net.URLEncoder.encode(
                        text,
                        "UTF-8"
                    )
                }"
            val response =
                java.net.URL(urlString).openConnection().apply { setRequestProperty("User-Agent", "Mozilla/5.0") }
                    .getInputStream().bufferedReader().use { it.readText() }

            val startIndex = response.indexOf("\"") + 1
            val endIndex = response.indexOf("\"", startIndex)

            // SANITIZATION: Fix NBSP
            response.substring(startIndex, endIndex)
                .replace("\u00A0", " ")
                .replace("&nbsp;", " ")
        } catch (e: Exception) {
            text
        }
    }

    private fun saveAndMergeTranslations(
        project: Project,
        mainFile: XmlFile,
        lang: String,
        newBatch: Map<String, String>
    ) {
        val resDir = mainFile.virtualFile.parent.parent
        val targetDir = resDir.findChild("values-$lang") ?: resDir.createChildDirectory(this, "values-$lang")
        val stringsFile = targetDir.findChild("strings.xml")

        val mergedMap = mutableMapOf<String, String>()

        // 1. Read existing file if it exists to preserve manual keys
        if (stringsFile != null) {
            val existingPsi = PsiManager.getInstance(project).findFile(stringsFile) as? XmlFile
            existingPsi?.rootTag?.findSubTags("string")?.forEach { tag ->
                val name = tag.getAttributeValue("name") ?: return@forEach
                mergedMap[name] = tag.value.text
            }
        }

        // 2. Add new batch (overwrites if keys match)
        newBatch.forEach { (k, v) -> mergedMap[k] = v }

        // 3. Generate XML
        val content = buildString {
            appendLine("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
            appendLine("<resources>")
            mergedMap.forEach { (k, v) -> appendLine("    <string name=\"$k\">${escapeXml(v)}</string>") }
            appendLine("</resources>")
        }

        if (stringsFile != null) {
            stringsFile.setBinaryContent(content.toByteArray())
        } else {
            val newFile = PsiFileFactory.getInstance(project)
                .createFileFromText("strings.xml", com.intellij.ide.highlighter.XmlFileType.INSTANCE, content)
            PsiManager.getInstance(project).findDirectory(targetDir)?.add(newFile)
        }

        // Inside your background task's WriteCommandAction
        val notificationGroup = com.intellij.notification.NotificationGroupManager.getInstance()
            .getNotificationGroup("Translation Notifications") // Matches the ID in plugin.xml

        notificationGroup.createNotification(
            MyBundle.message("notification.success", newBatch.size, "strings"),
            com.intellij.notification.NotificationType.INFORMATION
        ).notify(project)
    }

    private fun escapeXml(text: String): String = text.replace("'", "\\'").replace("\"", "\\\"").replace("&", "&amp;")

    private fun getExistingKeys(mainFile: XmlFile, lang: String): Set<String> {
        val target =
            mainFile.virtualFile.parent.parent.findChild("values-$lang")?.findChild("strings.xml") ?: return emptySet()
        val psi = PsiManager.getInstance(mainFile.project).findFile(target) as? XmlFile
        return psi?.rootTag?.findSubTags("string")?.mapNotNull { it.getAttributeValue("name") }?.toSet() ?: emptySet()
    }

    private fun findMainStringsXml(facet: AndroidFacet): XmlFile? {
        SourceProviderManager.getInstance(facet).currentSourceProviders.forEach { provider ->
            provider.resDirectories.forEach { res ->
                res.findChild("values")?.findChild("strings.xml")?.let {
                    return PsiManager.getInstance(facet.module.project).findFile(it) as? XmlFile
                }
            }
        }
        return null
    }

    private class TranslationRow(
        val key: String,
        val englishText: String,
        val isTranslated: Boolean,
        val isTranslatable: Boolean
    ) {
        val checkBox = JBCheckBox(key, isTranslatable && !isTranslated).apply { isEnabled = isTranslatable }
        val panel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(2, 5); maximumSize = Dimension(Int.MAX_VALUE, 35)
            if (!isTranslatable) {
                // Use status.non.translatable
                checkBox.text = "$key (${MyBundle.message("status.non.translatable")})"
                checkBox.foreground = JBColor.GRAY
            } else if (isTranslated) {
                // Use status.already.translated
                checkBox.text = "$key (${MyBundle.message("status.already.translated")})"
                checkBox.foreground = JBColor.GREEN
            }
            add(checkBox, BorderLayout.WEST)
            add(JBLabel(if (englishText.length > 45) "${englishText.take(42)}..." else englishText).apply {
                foreground = JBColor.GRAY; font = font.deriveFont(Font.ITALIC)
            }, BorderLayout.EAST)
        }
    }

    private fun findAnyStringsXml(project: Project): XmlFile? {
        val projectScope = GlobalSearchScope.projectScope(project)
        val files = FilenameIndex.getFilesByName(project, "strings.xml", projectScope)

        // We want the one in "values", not "values-hi" or others
        return files.filterIsInstance<XmlFile>().firstOrNull { file ->
            file.virtualFile.parent.name == "values"
        }
    }
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
    override fun update(e: AnActionEvent) {
//        super.update(e)
        val project = e.project
        // The action is visible if a project is open
        e.presentation.isEnabledAndVisible = (project != null)
    }
}