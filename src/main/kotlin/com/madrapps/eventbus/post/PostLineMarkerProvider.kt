package com.madrapps.eventbus.post

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpressionStatement
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.toUElement

class PostLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val uElement = element.toUElement()
        if (uElement is UQualifiedReferenceExpression && element !is PsiExpressionStatement) {
            val uCallExpression = uElement.selector as? UCallExpression ?: return null
            if (uCallExpression.receiverType?.canonicalText == "org.greenrobot.eventbus.EventBus") {
                if (uCallExpression.methodName == "post") {
                    val psiIdentifier = uCallExpression.methodIdentifier?.sourcePsi ?: return null
                    return PostLineMarkerInfo(psiIdentifier, "Subscribed by ---")
                }
            }
        }
        return null
    }
}


private class PostLineMarkerInfo(
    private val psiElement: PsiElement,
    private val message: String
) : LineMarkerInfo<PsiElement>(
    psiElement,
    psiElement.textRange,
    AllIcons.General.ArrowRight,
    { message },
    null,
    GutterIconRenderer.Alignment.RIGHT
) {
    override fun createGutterRenderer(): GutterIconRenderer? {
        return object : LineMarkerInfo.LineMarkerGutterIconRenderer<PsiElement>(this) {
            override fun getClickAction(): AnAction? = object : AnAction(message) {
                override fun actionPerformed(e: AnActionEvent) {
                    println("-- $element")
                }
            }
        }
    }
}