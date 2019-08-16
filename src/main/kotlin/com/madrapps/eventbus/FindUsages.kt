package com.madrapps.eventbus

import com.intellij.psi.PsiElement
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.usageView.UsageInfo
import com.intellij.usages.Usage
import com.intellij.usages.UsageInfo2UsageAdapter
import org.jetbrains.uast.*
import org.jetbrains.uast.UastCallKind.Companion.METHOD_CALL

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
    val find = withContainingElements
        .filterIsInstance<UCallExpression>()
        .find {
            it.kind == METHOD_CALL && it.getParentOfType<UQualifiedReferenceExpression>() == null
        }
    val uCallExpression = withContainingElements
        .filterIsInstance<UQualifiedReferenceExpression>()
        .firstOrNull()
        ?.selector as? UCallExpression
    return find ?: uCallExpression
}