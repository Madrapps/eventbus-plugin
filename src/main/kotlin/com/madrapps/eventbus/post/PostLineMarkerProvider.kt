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
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.toUElement
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class PostLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val uElement = element.toUElement() ?: return null
        if (element !is PsiExpressionStatement && isPostMethod(uElement)) {
            val psiIdentifier = (uElement.selector as? UCallExpression)?.methodIdentifier?.sourcePsi ?: return null
            return PostLineMarkerInfo(psiIdentifier, "Subscribed by ---")
        }
        return null
    }
}

@UseExperimental(ExperimentalContracts::class)
fun isPostMethod(uElement: UElement): Boolean {
    contract {
        returns(true) implies (uElement is UQualifiedReferenceExpression)
    }
    if (uElement is UQualifiedReferenceExpression) {
        val uCallExpression = uElement.selector as? UCallExpression ?: return false
        if (uCallExpression.receiverType?.canonicalText == "org.greenrobot.eventbus.EventBus") {
            return uCallExpression.methodName == "post"
        }
    }
    return false
}

private class PostLineMarkerInfo(
    psiElement: PsiElement,
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
