// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.services.amazonqDoc

import software.aws.toolkits.resources.message

open class DocException(
    override val message: String?,
    override val cause: Throwable? = null,
    val remainingIterations: Int? = null,
) : RuntimeException()

class ZipFileError(override val message: String, override val cause: Throwable?) : RuntimeException()

class CodeIterationLimitError(override val message: String, override val cause: Throwable?) : RuntimeException()

internal fun docServiceError(message: String?, cause: Throwable? = null, remainingIterations: Int? = null): Nothing =
    throw DocException(message, cause, remainingIterations)

internal fun conversationIdNotFound(): Nothing =
    throw DocException(message("amazonqFeatureDev.exception.conversation_not_found"))

val denyListedErrors = arrayOf("Deserialization error", "Inaccessible host", "UnknownHost")
fun createUserFacingErrorMessage(message: String?): String? =
    if (message != null && denyListedErrors.any { message.contains(it) }) "$FEATURE_NAME API request failed" else message
