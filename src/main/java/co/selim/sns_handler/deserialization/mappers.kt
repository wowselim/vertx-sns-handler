package co.selim.sns_handler.deserialization

import co.selim.sns_handler.model.Notification
import co.selim.sns_handler.model.SNSMessage
import co.selim.sns_handler.model.SubscriptionConfirmation
import co.selim.sns_handler.model.UnsubscribeConfirmation
import io.vertx.core.json.JsonObject
import java.time.Instant
import java.util.*

fun JsonObject.toNotification() = Notification(
  message = message,
  messageId = messageId,
  signature = signature,
  signatureVersion = signatureVersion,
  signingCertURL = signingCertURL,
  subject = subject,
  timestamp = timestamp,
  topicArn = topicArn,
  type = type,
  unsubscribeURL = unsubscribeURL,
)

fun JsonObject.toSubscriptionConfirmation() = SubscriptionConfirmation(
  message = message,
  messageId = messageId,
  signature = signature,
  signatureVersion = signatureVersion,
  signingCertURL = signingCertURL,
  subscribeURL = subscribeURL,
  timestamp = timestamp,
  token = token,
  topicArn = topicArn,
  type = type,
)

fun JsonObject.toUnsubscribeConfirmation() = UnsubscribeConfirmation(
  message = message,
  messageId = messageId,
  signature = signature,
  signatureVersion = signatureVersion,
  signingCertURL = signingCertURL,
  subscribeURL = subscribeURL,
  timestamp = timestamp,
  token = token,
  topicArn = topicArn,
  type = type,
)

private val JsonObject.message get(): String = requireString("Message")

private val JsonObject.messageId get(): UUID = UUID.fromString(requireString("MessageId"))

private val JsonObject.signature get(): String = requireString("Signature")

private val JsonObject.signatureVersion get(): String = requireString("SignatureVersion")

private val JsonObject.signingCertURL get(): String = requireString("SigningCertURL")

private val JsonObject.timestamp get(): Instant = Instant.parse(requireString("Timestamp"))

private val JsonObject.token get(): String = requireString("Token")

private val JsonObject.topicArn get(): String = requireString("TopicArn")

private val JsonObject.type get(): SNSMessage.Type = SNSMessage.Type.fromTextForm(requireString("Type"))

private val JsonObject.unsubscribeURL get(): String = requireString("UnsubscribeURL")

private val JsonObject.subscribeURL get(): String = requireString("SubscribeURL")

private val JsonObject.subject get(): String? = getString("Subject")

private fun JsonObject.requireString(key: String): String {
  return requireNotNull(getString(key)) { "Required field '$key' was missing from json object." }
}
