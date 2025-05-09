// Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.services.amazonqFeatureDev.clients

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import software.amazon.awssdk.services.codewhispererruntime.CodeWhispererRuntimeClient
import software.amazon.awssdk.services.codewhispererruntime.model.ArtifactType
import software.amazon.awssdk.services.codewhispererruntime.model.ContentChecksumType
import software.amazon.awssdk.services.codewhispererruntime.model.CreateTaskAssistConversationRequest
import software.amazon.awssdk.services.codewhispererruntime.model.CreateTaskAssistConversationResponse
import software.amazon.awssdk.services.codewhispererruntime.model.CreateUploadUrlResponse
import software.amazon.awssdk.services.codewhispererruntime.model.Dimension
import software.amazon.awssdk.services.codewhispererruntime.model.GetTaskAssistCodeGenerationResponse
import software.amazon.awssdk.services.codewhispererruntime.model.IdeCategory
import software.amazon.awssdk.services.codewhispererruntime.model.OperatingSystem
import software.amazon.awssdk.services.codewhispererruntime.model.OptOutPreference
import software.amazon.awssdk.services.codewhispererruntime.model.SendTelemetryEventResponse
import software.amazon.awssdk.services.codewhispererruntime.model.StartTaskAssistCodeGenerationResponse
import software.amazon.awssdk.services.codewhispererruntime.model.TaskAssistPlanningUploadContext
import software.amazon.awssdk.services.codewhispererruntime.model.UploadContext
import software.amazon.awssdk.services.codewhispererruntime.model.UploadIntent
import software.amazon.awssdk.services.codewhispererruntime.model.UserContext
import software.amazon.awssdk.services.codewhispererstreaming.model.ExportIntent
import software.aws.toolkits.core.utils.error
import software.aws.toolkits.core.utils.getLogger
import software.aws.toolkits.core.utils.info
import software.aws.toolkits.jetbrains.services.amazonq.clients.AmazonQStreamingClient
import software.aws.toolkits.jetbrains.services.amazonq.profile.QRegionProfileManager
import software.aws.toolkits.jetbrains.services.amazonqFeatureDev.FEATURE_EVALUATION_PRODUCT_NAME
import software.aws.toolkits.jetbrains.services.codemodernizer.utils.calculateTotalLatency
import software.aws.toolkits.jetbrains.services.telemetry.ClientMetadata
import software.aws.toolkits.jetbrains.settings.AwsSettings
import java.time.Instant
import software.amazon.awssdk.services.codewhispererruntime.model.ChatTriggerType as SyncChatTriggerType

@Service(Service.Level.PROJECT)
class FeatureDevClient(
    private val project: Project,
) {
    fun getTelemetryOptOutPreference() =
        if (AwsSettings.getInstance().isTelemetryEnabled) {
            OptOutPreference.OPTIN
        } else {
            OptOutPreference.OPTOUT
        }

    private val featureDevUserContext =
        ClientMetadata.getDefault().let {
            val osForFeatureDev: OperatingSystem =
                when {
                    SystemInfo.isWindows -> OperatingSystem.WINDOWS
                    SystemInfo.isMac -> OperatingSystem.MAC
                    // For now, categorize everything else as "Linux" (Linux/FreeBSD/Solaris/etc.)
                    else -> OperatingSystem.LINUX
                }

            UserContext
                .builder()
                .ideCategory(IdeCategory.JETBRAINS)
                .operatingSystem(osForFeatureDev)
                .product(FEATURE_EVALUATION_PRODUCT_NAME)
                .clientId(it.clientId)
                .ideVersion(it.awsVersion)
                .build()
        }

    private fun bearerClient() = QRegionProfileManager.getInstance().getQClient<CodeWhispererRuntimeClient>(project)

    private val amazonQStreamingClient
        get() = AmazonQStreamingClient.getInstance(project)

    fun sendFeatureDevTelemetryEvent(conversationId: String): SendTelemetryEventResponse =
        bearerClient().sendTelemetryEvent { requestBuilder ->
            requestBuilder.telemetryEvent { telemetryEventBuilder ->
                telemetryEventBuilder.featureDevEvent {
                    it.conversationId(conversationId)
                }
            }
            requestBuilder.optOutPreference(getTelemetryOptOutPreference())
            requestBuilder.userContext(featureDevUserContext)
            requestBuilder.profileArn(QRegionProfileManager.getInstance().activeProfile(project)?.arn)
        }

    fun sendFeatureDevMetricData(operationName: String, result: String): SendTelemetryEventResponse =
        bearerClient().sendTelemetryEvent { requestBuilder ->
            requestBuilder.telemetryEvent { telemetryEventBuilder ->
                telemetryEventBuilder.metricData {
                    it
                        .metricName("Operation")
                        .metricValue(1.0)
                        .timestamp(Instant.now())
                        .product("FeatureDev")
                        .dimensions(
                            listOf(
                                Dimension.builder()
                                    .name("operationName")
                                    .value(operationName)
                                    .build(),
                                Dimension.builder()
                                    .name("result")
                                    .value(result)
                                    .build()
                            )
                        )
                }
            }
            requestBuilder.optOutPreference(getTelemetryOptOutPreference())
            requestBuilder.userContext(featureDevUserContext)
            requestBuilder.profileArn(QRegionProfileManager.getInstance().activeProfile(project)?.arn)
        }

    fun sendFeatureDevCodeGenerationEvent(
        conversationId: String,
        linesOfCodeGenerated: Int,
        charactersOfCodeGenerated: Int,
    ): SendTelemetryEventResponse =
        bearerClient().sendTelemetryEvent { requestBuilder ->
            requestBuilder.telemetryEvent { telemetryEventBuilder ->
                telemetryEventBuilder.featureDevCodeGenerationEvent {
                    it
                        .conversationId(conversationId)
                        .linesOfCodeGenerated(linesOfCodeGenerated)
                        .charactersOfCodeGenerated(charactersOfCodeGenerated)
                }
            }
            requestBuilder.optOutPreference(getTelemetryOptOutPreference())
            requestBuilder.userContext(featureDevUserContext)
            requestBuilder.profileArn(QRegionProfileManager.getInstance().activeProfile(project)?.arn)
        }

    fun sendFeatureDevCodeAcceptanceEvent(
        conversationId: String,
        linesOfCodeAccepted: Int,
        charactersOfCodeAccepted: Int,
    ): SendTelemetryEventResponse =
        bearerClient().sendTelemetryEvent { requestBuilder ->
            requestBuilder.telemetryEvent { telemetryEventBuilder ->
                telemetryEventBuilder.featureDevCodeAcceptanceEvent {
                    it
                        .conversationId(conversationId)
                        .linesOfCodeAccepted(linesOfCodeAccepted)
                        .charactersOfCodeAccepted(charactersOfCodeAccepted)
                }
            }
            requestBuilder.optOutPreference(getTelemetryOptOutPreference())
            requestBuilder.userContext(featureDevUserContext)
            requestBuilder.profileArn(QRegionProfileManager.getInstance().activeProfile(project)?.arn)
        }

    fun createTaskAssistConversation(): CreateTaskAssistConversationResponse =
        bearerClient().createTaskAssistConversation(
            CreateTaskAssistConversationRequest.builder()
                .profileArn(QRegionProfileManager.getInstance().activeProfile(project)?.arn)
                .build(),
        )

    fun createTaskAssistUploadUrl(
        conversationId: String,
        contentChecksumSha256: String,
        contentLength: Long,
        uploadId: String,
    ): CreateUploadUrlResponse =
        bearerClient().createUploadUrl {
            it
                .contentChecksumType(ContentChecksumType.SHA_256)
                .uploadId(uploadId)
                .contentChecksum(contentChecksumSha256)
                .contentLength(contentLength)
                .artifactType(ArtifactType.SOURCE_CODE)
                .uploadIntent(UploadIntent.TASK_ASSIST_PLANNING)
                .uploadContext(
                    UploadContext
                        .builder()
                        .taskAssistPlanningUploadContext(
                            TaskAssistPlanningUploadContext
                                .builder()
                                .conversationId(conversationId)
                                .build(),
                        ).build(),
                )
                .profileArn(QRegionProfileManager.getInstance().activeProfile(project)?.arn)
        }

    fun startTaskAssistCodeGeneration(
        conversationId: String,
        uploadId: String,
        userMessage: String,
        codeGenerationId: String?,
        currentCodeGenerationId: String?,
    ): StartTaskAssistCodeGenerationResponse =
        bearerClient()
            .startTaskAssistCodeGeneration { request ->
                request
                    .conversationState {
                        it
                            .conversationId(conversationId)
                            .chatTriggerType(SyncChatTriggerType.MANUAL)
                            .currentMessage { cm -> cm.userInputMessage { um -> um.content(userMessage) } }
                    }.workspaceState {
                        it
                            .programmingLanguage { pl -> pl.languageName("javascript") } // This parameter is omitted by featureDev but required in the request
                            .uploadId(uploadId)
                    }.codeGenerationId(codeGenerationId.toString())
                    .currentCodeGenerationId(currentCodeGenerationId)
                    .profileArn(QRegionProfileManager.getInstance().activeProfile(project)?.arn)
            }

    fun getTaskAssistCodeGeneration(
        conversationId: String,
        codeGenerationId: String,
    ): GetTaskAssistCodeGenerationResponse =
        bearerClient()
            .getTaskAssistCodeGeneration {
                it
                    .conversationId(conversationId)
                    .codeGenerationId(codeGenerationId)
                    .profileArn(QRegionProfileManager.getInstance().activeProfile(project)?.arn)
            }

    suspend fun exportTaskAssistResultArchive(conversationId: String): MutableList<ByteArray> =
        amazonQStreamingClient.exportResultArchive(
            conversationId,
            ExportIntent.TASK_ASSIST,
            null,
            { e ->
                LOG.error(
                    e,
                ) { "TaskAssist - ExportResultArchive stream exportId=$conversationId exportIntent=${ExportIntent.TASK_ASSIST} Failed: ${e.message} " }
            },
            { startTime ->
                LOG.info { "TaskAssist - ExportResultArchive latency: ${calculateTotalLatency(startTime, Instant.now())}" }
            },
        )

    companion object {
        private val LOG = getLogger<FeatureDevClient>()

        fun getInstance(project: Project) = project.service<FeatureDevClient>()
    }
}
