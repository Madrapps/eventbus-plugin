package com.madrapps.eventbus

import com.intellij.psi.PsiElement
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.usageView.UsageInfo
import com.intellij.usages.Usage
import com.intellij.usages.UsageInfo2UsageAdapter
import org.jetbrains.uast.*
import org.jetbrains.uast.UastCallKind.Companion.METHOD_CALL

internal fun search(elements: List<PsiElement>): Collection<UsageInfo> {
    val references = elements.flatMap {
        ReferencesSearch.search(it).findAll()
    }
    blog("Search - ${references.size} found")
    return references.map(::UsageInfo)
}

internal fun search(element: PsiElement): Collection<UsageInfo> {
    val references = ReferencesSearch.search(element).findAll()
    blog("Search - ${references.size} found")
    return references.map(::UsageInfo)
}

internal fun Usage.toUElement(): UElement? {
    return (this as? UsageInfo2UsageAdapter)?.usageInfo?.element?.toUElement()
}

internal inline fun <reified T : UElement> Usage.getType(): T? {
    return toUElement()?.getParentOfType<T>()
}

internal fun UElement.getCallExpression(): UCallExpression? {
    if (this is UCallExpression) {
        if (getParentOfType<UQualifiedReferenceExpression>() == null) {
            return this
        }
    } else if (this is UQualifiedReferenceExpression) {
        return selector as? UCallExpression
    }
    return null
}

internal fun UElement.getParentOfTypeCallExpression(): UCallExpression? {
    return withContainingElements
        .filterIsInstance<UCallExpression>()
        .find {
            it.kind == METHOD_CALL //&& it.getParentOfType<UQualifiedReferenceExpression>() == null
        } ?: withContainingElements
        .filterIsInstance<UQualifiedReferenceExpression>()
        .firstOrNull()
        ?.selector as? UCallExpression
}

internal fun Usage.getPostStatementSourcePsi() = toUElement()?.getParentOfTypeCallExpression()?.sourcePsi

internal fun Usage.getSubscribeMethodSourcePsi() = getType<UMethod>()?.uastAnchor?.sourcePsi

internal fun Usage.file() = toUElement()?.sourcePsi?.containingFile