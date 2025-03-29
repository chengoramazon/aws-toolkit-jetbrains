// Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.gradle.detekt.rules

import io.github.detekt.test.utils.createEnvironment
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class JacksonFailOnUnknownPropertiesRuleTest {
    private val rule = JacksonFailOnUnknownPropertiesRule()
    private val environment = createEnvironment().env

    @Test
    fun `reports unconfigured jacksonObjectMapper`() {
        val code = """
            import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
            
            fun foo() {
                val mapper = jacksonObjectMapper()
            }
        """
        val findings = rule.compileAndLintWithContext(environment, code)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `accepts configured jacksonObjectMapper using disable`() {
        val code = """
            import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
            import com.fasterxml.jackson.databind.DeserializationFeature
            
            fun foo() {
                val mapper = jacksonObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            }
        """
        val findings = rule.compileAndLintWithContext(environment, code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `accepts configured jacksonObjectMapper using configure`() {
        val code = """
            import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
            import com.fasterxml.jackson.databind.DeserializationFeature
            
            fun foo() {
                val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
        """
        val findings = rule.compileAndLintWithContext(environment, code)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `accepts configured jacksonObjectMapper using enable`() {
        val code = """
            import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
            import com.fasterxml.jackson.databind.DeserializationFeature
            
            fun foo() {
                val mapper = jacksonObjectMapper().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            }
        """
        val findings = rule.compileAndLintWithContext(environment, code)
        assertThat(findings).isEmpty()
    }
}
