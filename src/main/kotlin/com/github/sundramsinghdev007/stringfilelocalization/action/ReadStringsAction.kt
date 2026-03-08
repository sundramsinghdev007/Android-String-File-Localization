package com.github.sundramsinghdev007.stringfilelocalization.action

import com.android.tools.idea.projectsystem.SourceProviderManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile
import org.jetbrains.android.facet.AndroidFacet
//import org.jetbrains.android.facet.SourceProviderManager

class ReadStringsAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return

        // 1. Get the module and Android Facet
        val module = ModuleUtilCore.findModuleForPsiElement(psiFile) ?: return
        val facet = AndroidFacet.getInstance(module) ?: return

        // 2. Use SourceProviderManager to get all relevant resource directories
        val sourceProviders = SourceProviderManager.getInstance(facet).currentSourceProviders
        val stringItems = mutableListOf<String>()

        for (provider in sourceProviders) {
            // provider.resDirectories gives us all 'res' folders for this variant
            for (resDir in provider.resDirectories) {
                val valuesDir = resDir.findChild("values")
                val stringsFile = valuesDir?.findChild("strings.xml")

                if (stringsFile != null) {
                    val xmlFile = PsiManager.getInstance(project).findFile(stringsFile) as? XmlFile
                    val rootTag = xmlFile?.rootTag
                    val tags = rootTag?.findSubTags("string") ?: emptyArray()

                    for (tag in tags) {
                        val key = tag.getAttributeValue("name")
                        val value = tag.value.text
                        if (key != null) {
                            stringItems.add("[$key]: $value")
                        }
                    }
                }
            }
        }

        // 3. Display the result
        if (stringItems.isEmpty()) {
            Messages.showInfoMessage(project, "No strings.xml items found in current source sets.", "Result")
        } else {
            Messages.showInfoMessage(project, stringItems.joinToString("\n"), "All Strings in Project")
        }
    }
}