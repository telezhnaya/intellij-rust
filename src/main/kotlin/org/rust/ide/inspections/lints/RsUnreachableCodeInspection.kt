/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints

import com.intellij.psi.PsiElement
import org.rust.ide.injected.isDoctestInjection
import org.rust.ide.inspections.RsProblemsHolder
import org.rust.ide.utils.isCfgUnknown
import org.rust.ide.utils.isEnabledByCfg
import org.rust.lang.core.dfa.isInTailPosition
import org.rust.lang.core.psi.*
import org.rust.lang.core.psi.ext.RsElement
import org.rust.lang.core.psi.ext.RsLabeledExpression
import org.rust.lang.core.psi.ext.ancestorStrict
import org.rust.lang.core.types.controlFlowGraph

class RsUnreachableCodeInspection : RsLintInspection() {
    override fun getDisplayName(): String = "Unreachable code"

    override fun getLint(element: PsiElement): RsLint = RsLint.UnreachableCode

    override fun buildVisitor(holder: RsProblemsHolder, isOnTheFly: Boolean) = object : RsVisitor() {
        override fun visitFunction(func: RsFunction) {
            // Disable inside doc tests
            if (func.isDoctestInjection) return

            val controlFlowGraph = func.controlFlowGraph ?: return

            // Parent PSI elements should be processed before their children
            // in order to annotate the entire statement/expression first and just ignore its children
            val unreachableElements = controlFlowGraph
                .collectUnreachableElements()
                .reversed()

            val unreachableExprs = unreachableElements
                .filterIsInstance<RsExpr>()
                .toSet()
            val unreachableStmts = unreachableElements
                .filterIsInstance<RsStmt>()
                .toSet()

            val reported = mutableSetOf<RsStmt>()

            // In terms of our control-flow graph, a PSI element is unreachable
            // if it cannot not be executed **completely**.
            // But partially executed elements should not be annotated as unreachable since the execution
            // actually reaches them; only some parts of such elements should be annotated instead
            fun isEntirelyUnreachable(expr: RsExpr): Boolean {
                if (expr !in unreachableExprs) {
                    return false
                }

                val firstBlockElement = when (expr) {
                    is RsLabeledExpression -> expr.block?.stmtList?.firstOrNull() ?: expr.block?.expr
                    is RsIfExpr -> expr.block?.stmtList?.firstOrNull() ?: expr.block?.expr
                    else -> null
                } ?: return true

                return firstBlockElement in unreachableStmts
            }

            for (stmt in unreachableStmts) {
                val parentStmt = stmt.ancestorStrict<RsStmt>()
                val parentExpr = stmt.ancestorStrict<RsExpr>()
                if (parentStmt in reported) {
                    continue
                }
                if (parentExpr != null && parentExpr.isInTailPosition && isEntirelyUnreachable(parentExpr)) {
                    // The entire expression will be annotated later
                    continue
                }

                val expr = when (stmt) {
                    is RsExprStmt -> stmt.expr
                    is RsLetDecl -> stmt.expr
                    else -> continue
                } ?: continue

                if (isEntirelyUnreachable(expr)) {
                    registerUnreachableProblem(holder, stmt)
                    reported.add(stmt)
                }
            }

            for (expr in unreachableExprs) {
                if (expr.isInTailPosition && isEntirelyUnreachable(expr)) {
                    registerUnreachableProblem(holder, expr)
                }
            }
        }
    }

    private fun registerUnreachableProblem(holder: RsProblemsHolder, element: RsElement) {
        if (!element.isPhysical) return
        if (!element.isEnabledByCfg || element.isCfgUnknown) return

        val message = when (element) {
            is RsStmt -> "Unreachable statement"
            is RsExpr -> "Unreachable expression"
            else -> "Unreachable code"
        }

        // TODO: add quick-fix to remove unreachable code
        holder.registerLintProblem(element, message)
    }
}
