package co.selim.sns_handler.model

import java.time.Instant
import java.util.*

public data class Notification(
  val message: String,
  val messageId: UUID,
  val signature: String,
  val signatureVersion: String,
  val signingCertURL: String,
  val subject: String?,
  val timestamp: Instant,
  val topicArn: String,
  override val type: SNSMessage.Type,
  val unsubscribeURL: String,
) : SNSMessage
