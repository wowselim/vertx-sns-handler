package co.selim.sns_handler.handler.impl

import co.selim.sns_handler.deserialization.toNotification
import co.selim.sns_handler.deserialization.toSubscriptionConfirmation
import co.selim.sns_handler.deserialization.toUnsubscribeConfirmation
import co.selim.sns_handler.handler.SNSHandler
import co.selim.sns_handler.handler.message.NotificationHandler
import co.selim.sns_handler.handler.message.SubscriptionConfirmationHandler
import co.selim.sns_handler.handler.message.UnsubscribeConfirmationHandler
import co.selim.sns_handler.model.Notification
import co.selim.sns_handler.model.SNSMessage
import co.selim.sns_handler.model.SubscriptionConfirmation
import co.selim.sns_handler.model.UnsubscribeConfirmation
import io.vertx.core.Future
import io.vertx.core.http.HttpClient
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.RequestOptions
import io.vertx.ext.web.RoutingContext
import org.slf4j.LoggerFactory


internal class SNSHandlerImpl(private val httpClient: HttpClient) : SNSHandler {

  companion object {
    private val logger = LoggerFactory.getLogger(SNSHandlerImpl::class.java)
    private const val MESSAGE_TYPE_HEADER = "x-amz-sns-message-type"
  }

  private var notificationHandler: NotificationHandler =
    NotificationHandler.create()
  private var subscriptionConfirmationHandler: SubscriptionConfirmationHandler =
    SubscriptionConfirmationHandler.create()
  private var unsubscribeConfirmationHandler: UnsubscribeConfirmationHandler =
    UnsubscribeConfirmationHandler.create()

  override fun setOnNotification(handler: NotificationHandler): SNSHandler = apply {
    notificationHandler = handler
  }

  override fun setOnSubscriptionConfirmation(handler: SubscriptionConfirmationHandler): SNSHandler = apply {
    subscriptionConfirmationHandler = handler
  }

  override fun setOnUnsubscribeConfirmation(handler: UnsubscribeConfirmationHandler): SNSHandler = apply {
    unsubscribeConfirmationHandler = handler
  }

  override fun handle(ctx: RoutingContext) {
    val messageTypeHeader = ctx.request().getHeader(MESSAGE_TYPE_HEADER)
    val messageType = SNSMessage.Type.fromTextForm(messageTypeHeader)

    logger.debug("Received message of type $messageType")

    val body = ctx.bodyAsJson
    val future = when (messageType) {
      SNSMessage.Type.NOTIFICATION -> handleNotification(body.toNotification())
      SNSMessage.Type.SUBSCRIPTION_CONFIRMATION -> handleSubscriptionConfirmation(body.toSubscriptionConfirmation())
      SNSMessage.Type.UNSUBSCRIBE_CONFIRMATION -> handleUnsubscribeConfirmation(body.toUnsubscribeConfirmation())
    }

    future.onSuccess { ctx.end() }.onFailure { t -> ctx.fail(t) }
  }

  private fun handleNotification(notification: Notification): Future<Unit> {
    return when (notificationHandler.handle(notification)) {
      NotificationHandler.Acknowledge -> {
        logger.debug("Acknowledged notification on topic ${notification.topicArn}")
        Future.succeededFuture(/* Nothing to do */)
      }
      NotificationHandler.Unsubscribe -> {
        logger.debug("Unsubscribing from topic ${notification.topicArn}")
        httpClient.visitURL(notification.unsubscribeURL)
      }
    }
  }

  private fun handleSubscriptionConfirmation(subscriptionConfirmation: SubscriptionConfirmation): Future<Unit> {
    return when (subscriptionConfirmationHandler.handle(subscriptionConfirmation)) {
      SubscriptionConfirmationHandler.Acknowledge -> {
        logger.debug("Confirming subscription to topic ${subscriptionConfirmation.topicArn}")
        httpClient.visitURL(subscriptionConfirmation.subscribeURL)
      }
      SubscriptionConfirmationHandler.Ignore -> {
        logger.debug("Ignoring subscription confirmation for topic ${subscriptionConfirmation.topicArn}")
        Future.succeededFuture(/* Nothing to do */)
      }
    }
  }

  private fun handleUnsubscribeConfirmation(unsubscribeConfirmation: UnsubscribeConfirmation): Future<Unit> {
    return when (unsubscribeConfirmationHandler.handle(unsubscribeConfirmation)) {
      UnsubscribeConfirmationHandler.Acknowledge -> {
        logger.debug("Confirming unsubscribe to topic ${unsubscribeConfirmation.topicArn}")
        Future.succeededFuture(/* Nothing to do */)
      }
      UnsubscribeConfirmationHandler.Resubscribe -> {
        logger.debug("Resubscribing to topic ${unsubscribeConfirmation.topicArn}")
        httpClient.visitURL(unsubscribeConfirmation.subscribeURL)
      }
    }
  }

  private fun HttpClient.visitURL(url: String): Future<Unit> {
    val options = RequestOptions().setMethod(HttpMethod.GET).setAbsoluteURI(url)
    return request(options)
      .flatMap { it.connect() }
      .onFailure { logger.error("Failed to open URL $url", it) }
      .map(Unit)
  }
}
