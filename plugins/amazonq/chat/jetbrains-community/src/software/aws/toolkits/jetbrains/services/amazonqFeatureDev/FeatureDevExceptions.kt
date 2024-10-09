// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.services.amazonqFeatureDev

import software.aws.toolkits.jetbrains.services.amazonq.RepoSizeError
import software.aws.toolkits.resources.AwsToolkitBundle.message

open class FeatureDevException(override val message: String?, override val cause: Throwable? = null) : RuntimeException()

class ContentLengthError(override val message: String, override val cause: Throwable?) : RepoSizeError, RuntimeException()

class ZipFileError(override val message: String, override val cause: Throwable?) : RuntimeException()

class CodeIterationLimitError(override val message: String, override val cause: Throwable?) : RuntimeException()

class MonthlyConversationLimitError(override val message: String, override val cause: Throwable?) : RuntimeException()

class GetCodeGenerationFailed(override val message: String?, override val cause: Throwable? = null) : FeatureDevException(message, cause)

class ExportResultArchiveFailed(override val message: String?, override val cause: Throwable? = null) : FeatureDevException(message, cause)

class StartCodeGenerationFailed(override val message: String?, override val cause: Throwable? = null) : FeatureDevException(message, cause)

class CodeGenFailed(override val message: String? = message("amazonqFeatureDev.code_generation.failed_generation"), override val cause: Throwable? = null) : FeatureDevException(message, cause)

class GuardrailsException(override val message: String? = message("amazonqFeatureDev.exception.guardrails"), override val cause: Throwable? = null) : FeatureDevException(message, cause)

class PromptRefusalException(override val message: String? = message("amazonqFeatureDev.exception.prompt_refusal"), override val cause: Throwable? = null) : FeatureDevException(message, cause)

class EmptyPatchException(override val message: String? = message("amazonqFeatureDev.code_generation.error_message"), override val cause: Throwable? = null) : FeatureDevException(message, cause)

class ThrottlingException(override val message: String? = message("amazonqFeatureDev.exception.throttling"), override val cause: Throwable? = null) : FeatureDevException(message, cause)

class UnknownCodeGenError(override val message: String? = message("amazonqFeatureDev.exception.throttling"), override val cause: Throwable? = null) : FeatureDevException(message, cause)

class UploadCodeError(override val message: String? = message("amazonqFeatureDev.exception.upload_code"), override val cause: Throwable? = null) : FeatureDevException(message, cause)

class ConversationIdNotFoundError(override val message: String? = message("amazonqFeatureDev.exception.conversation_not_found"), override val cause: Throwable? = null) : FeatureDevException(message, cause)

class ExportParseError(override val message: String? = message("amazonqFeatureDev.exception.export_parsing_error"), override val cause: Throwable? = null) : FeatureDevException(message, cause)


internal fun apiError(message: String?, cause: Throwable?): Nothing =
    throw FeatureDevException(message, cause)

val denyListedErrors = arrayOf("Deserialization error", "Inaccessible host", "UnknownHost")
fun createUserFacingErrorMessage(message: String?): String? =
    if (message != null && denyListedErrors.any { message.contains(it) }) "$FEATURE_NAME API request failed" else message
