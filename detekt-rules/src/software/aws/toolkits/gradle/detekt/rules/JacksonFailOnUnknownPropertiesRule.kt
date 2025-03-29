// Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.gradle.detekt.rules

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression

class JacksonFailOnUnknownPropertiesRule(config: Config = Config.empty) : Rule(config) {
    override val issue = Issue(
        id = "JacksonFailOnUnknownProperties",
        description = "ObjectMapper must explicitly configure FAIL_ON_UNKNOWN_PROPERTIES",
        severity = Severity.Warning,
        debt = Debt.FIVE_MINS
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val callName = expression.getCallNameExpression()?.text ?: return

        if (callName == "jacksonObjectMapper") {
            checkConfiguration(expression)
        }
    }

    private fun checkConfiguration(expression: KtCallExpression) {
        var current = expression.parent as? KtDotQualifiedExpression
        var maxChainDepth = 3
        var isConfigured = false

        while (current != null && maxChainDepth > 0) {
            val selector = current.selectorExpression as? KtCallExpression
            val selectorName = selector?.getCallNameExpression()?.text

            if (selectorName in CONFIGURATION_METHODS &&
                selector?.valueArguments?.any {
                    it.text.contains("FAIL_ON_UNKNOWN_PROPERTIES")
                } == true
            ) {
                isConfigured = true
                break
            }

            current = current.parent as? KtDotQualifiedExpression
            maxChainDepth--
        }

        if (!isConfigured) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    "ObjectMapper must explicitly configure FAIL_ON_UNKNOWN_PROPERTIES"
                )
            )
        }
    }

    companion object {
        private val CONFIGURATION_METHODS = setOf("configure", "disable", "enable")
    }
}
