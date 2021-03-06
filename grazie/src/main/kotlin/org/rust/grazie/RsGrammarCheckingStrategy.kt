/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.grazie

import com.intellij.grazie.grammar.strategy.GrammarCheckingStrategy
import com.intellij.grazie.grammar.strategy.GrammarCheckingStrategy.TextDomain
import com.intellij.grazie.grammar.strategy.StrategyUtils
import com.intellij.grazie.grammar.strategy.impl.RuleGroup
import com.intellij.grazie.utils.LinkedSet
import com.intellij.psi.PsiElement
import org.rust.ide.injected.findDoctestInjectableRanges
import org.rust.lang.core.psi.*
import org.rust.lang.core.psi.ext.elementType

class RsGrammarCheckingStrategy : GrammarCheckingStrategy {

    override fun isMyContextRoot(element: PsiElement): Boolean =
        getContextRootTextDomain(element) != TextDomain.NON_TEXT

    // BACKCOMPAT: 2020.2
    @Suppress("UnstableApiUsage", "OverridingDeprecatedMember")
    override fun isTypoAccepted(root: PsiElement, typoRange: IntRange, ruleRange: IntRange): Boolean {
        if (root !is RsDocCommentImpl) return true

        return findDoctestInjectableRanges(root)
            .flatten()
            .none { it.intersects(typoRange.first, typoRange.last) }
    }

    override fun getIgnoredRuleGroup(root: PsiElement, child: PsiElement): RuleGroup? = RuleGroup.LITERALS

    override fun getStealthyRanges(root: PsiElement, text: CharSequence): LinkedSet<IntRange> {
        val parent = root.parent
        return if (parent is RsLitExpr) {
            val valueTextRange = (parent.kind as? RsLiteralKind.String)?.offsets?.value ?: return linkedSetOf()
            linkedSetOf(0 until valueTextRange.startOffset, valueTextRange.endOffset until text.length)
        } else {
            StrategyUtils.indentIndexes(text, setOf(' ', '/', '!'))
        }
    }

    override fun getContextRootTextDomain(root: PsiElement): TextDomain {
        return when (root.elementType) {
            in RS_ALL_STRING_LITERALS -> TextDomain.LITERALS
            in RS_DOC_COMMENTS -> TextDomain.DOCS
            in RS_REGULAR_COMMENTS -> TextDomain.COMMENTS
            else -> TextDomain.NON_TEXT
        }
    }
}
