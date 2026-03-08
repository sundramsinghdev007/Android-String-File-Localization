package com.github.sundramsinghdev007.stringfilelocalization.action

import com.android.tools.idea.res.StudioResourceRepositoryManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlFile
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import java.util.*

class ExtractToAndroidStringsAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return

        // 1. Identify the String at cursor
        val element = psiFile.findElementAt(editor.caretModel.offset)
        val stringTemplate = PsiTreeUtil.getParentOfType(element, KtStringTemplateExpression::class.java)

        if (stringTemplate == null) return

        // 2. Automatic Key Generation (e.g., "Hello World" -> "hello_world")
        val rawText = stringTemplate.text.replace("\"", "")
        val autoKey = rawText.lowercase(Locale.getDefault())
            .replace(Regex("[^a-z0-9]"), "_")
            .take(30)
            .trim('_')

        // 3. Locate the Android Module and strings.xml
        val module = ModuleUtilCore.findModuleForPsiElement(psiFile) ?: return
        val facet = AndroidFacet.getInstance(module) ?: return
        val stringsXmlFile = findStringsXml(facet) ?: return

        // 4. Execute the Transactional Write
        WriteCommandAction.runWriteCommandAction(project) {
            // Add entry to XML
            val rootTag = stringsXmlFile.rootTag
            val newTag = rootTag?.createChildTag("string", "", rawText, false)
            newTag?.setAttribute("name", autoKey)
            rootTag?.addSubTag(newTag!!, false)

            // Replace Kotlin code with reference
            val document = editor.document
            document.replaceString(
                stringTemplate.textRange.startOffset,
                stringTemplate.textRange.endOffset,
                "context.getString(R.string.$autoKey)"
            )
        }
    }

    private fun findStringsXml(facet: AndroidFacet): XmlFile? {
        // Access the resource repository for the specific module
        val repository = StudioResourceRepositoryManager.getInstance(facet).moduleResources

        // Get the resource directories (replaces allResourceDirectories)
        val resourceDirs = repository.resourceDirs

        for (resDir in resourceDirs) {
            val valuesDir = resDir.findChild("values")
            val stringsFile = valuesDir?.findChild("strings.xml")
            if (stringsFile != null) {
                return PsiManager.getInstance(facet.module.project)
                    .findFile(stringsFile as VirtualFile) as? XmlFile
            }
        }
        return null
    }
}

/*

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlFile
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import com.android.tools.idea.res.StudioResourceRepositoryManager // Add this import
import com.intellij.openapi.vfs.VirtualFile


class ExtractToAndroidStringsAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return

        val element = psiFile.findElementAt(editor.caretModel.offset)
        val stringTemplate = PsiTreeUtil.getParentOfType(element, KtStringTemplateExpression::class.java)

        if (stringTemplate == null) {
            Messages.showErrorDialog(project, "Please place cursor on a hardcoded string.", "No String Found")
            return
        }

        val rawText = stringTemplate.text.replace("\"", "")
        val resourceKey = Messages.showInputDialog(
            project, "Enter resource name for: \"$rawText\"",
            "Extract String", Messages.getQuestionIcon()
        ) ?: return

        val module = ModuleUtilCore.findModuleForPsiElement(psiFile) ?: return
        val facet = AndroidFacet.getInstance(module) ?: return

        // Correctly find strings.xml using the modern resource API
        val stringsXmlFile = findStringsXml(facet)

        if (stringsXmlFile == null) {
            Messages.showErrorDialog(project, "Could not find strings.xml in this module.", "Error")
            return
        }

        WriteCommandAction.runWriteCommandAction(project) {
            val rootTag = stringsXmlFile.rootTag
            val newTag = rootTag?.createChildTag("string", "", rawText, false)
            newTag?.setAttribute("name", resourceKey)
            rootTag?.addSubTag(newTag!!, false)

            val document = editor.document
            document.replaceString(
                stringTemplate.textRange.startOffset,
                stringTemplate.textRange.endOffset,
                "context.getString(R.string.$resourceKey)"
            )
        }
    }

    private fun findStringsXml(facet: AndroidFacet): XmlFile? {
        // Access the resource repository for the specific module
        val repository = StudioResourceRepositoryManager.getInstance(facet).moduleResources

        // Get the resource directories (replaces allResourceDirectories)
        val resourceDirs = repository.resourceDirs

        for (resDir in resourceDirs) {
            val valuesDir = resDir.findChild("values")
            val stringsFile = valuesDir?.findChild("strings.xml")
            if (stringsFile != null) {
                return PsiManager.getInstance(facet.module.project)
                    .findFile(stringsFile as VirtualFile) as? XmlFile
            }
        }
        return null
    }
}*/
