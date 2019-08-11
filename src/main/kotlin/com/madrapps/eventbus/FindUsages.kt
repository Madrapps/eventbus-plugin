package com.madrapps.eventbus

import com.intellij.psi.PsiElement
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.usageView.UsageInfo
import com.intellij.usages.Usage
import com.intellij.usages.UsageInfo2UsageAdapter
import org.jetbrains.uast.UElement
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.toUElement

internal fun search(element: PsiElement): Collection<UsageInfo> {
    val references = ReferencesSearch.search(element).findAll()
    return references.map(::UsageInfo)
}

internal fun Usage.toUElement(): UElement? {
    return (this as? UsageInfo2UsageAdapter)?.usageInfo?.element?.toUElement()
}

internal inline fun <reified T : UElement> Usage.getType(): T? {
    return toUElement()?.getParentOfType<T>()
}