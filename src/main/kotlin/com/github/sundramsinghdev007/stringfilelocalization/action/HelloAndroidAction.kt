package com.github.sundramsinghdev007.stringfilelocalization.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages

class HelloAndroidAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        // This code runs when the user clicks your menu item
        Messages.showInfoMessage(
            "Hello from your new Android Studio Plugin!",
            "My First Plugin"
        )
    }
}