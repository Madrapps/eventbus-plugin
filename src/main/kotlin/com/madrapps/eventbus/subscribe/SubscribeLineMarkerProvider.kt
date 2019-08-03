package com.madrapps.eventbus.subscribe

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement

class SubscribeLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        println(element)
        return null
    }
}

private class SubscribeLineMarkerInfo(
    private val psiElement: PsiElement,
    private val message: String,
    private val file: VirtualFile
) : LineMarkerInfo<PsiElement>(
    psiElement,
    psiElement.textRange,
    AllIcons.Icons.Ide.NextStep,
    { message },
    null,
    GutterIconRenderer.Alignment.RIGHT
) {

}