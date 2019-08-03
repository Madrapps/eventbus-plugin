package com.madrapps.eventbus.subscribe

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer.Alignment.RIGHT
import com.intellij.psi.PsiElement
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElement

class SubscribeLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val uElement = element.toUElement()
        if (uElement is UMethod) {
            val annotation = uElement.annotations.find { it.qualifiedName == "org.greenrobot.eventbus.Subscribe" }
            if (annotation != null) {
                if (uElement.uastParameters.size == 1) {
                    val psiElement = uElement.uastAnchor?.sourcePsi
                    if (psiElement != null) {
                        return SubscribeLineMarkerInfo(psiElement, "post for ---")
                    }
                }
            }
        }
        return null
    }
}

private class SubscribeLineMarkerInfo(
    private val psiElement: PsiElement,
    private val message: String
) : LineMarkerInfo<PsiElement>(
    psiElement,
    psiElement.textRange,
    AllIcons.Icons.Ide.NextStep,
    { message },
    null,
    RIGHT
)