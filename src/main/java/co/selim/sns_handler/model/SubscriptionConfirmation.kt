package co.selim.sns_handler.model

import java.time.Instant
import java.util.*

public data class SubscriptionConfirmation(
  val message: String,
  val messageId: UUID,
  val signature: String,
  val signatureVersion: String,
  val signingCertURL: String,
  val subscribeURL: String,
  val timestamp: Instant,
  val token: String,
  val topicArn: String,
  override val type: SNSMessage.Type,
) : SNSMessage
