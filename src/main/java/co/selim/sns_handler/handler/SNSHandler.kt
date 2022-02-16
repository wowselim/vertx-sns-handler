package co.selim.sns_handler.handler

import co.selim.sns_handler.handler.impl.SNSHandlerImpl
import co.selim.sns_handler.handler.message.NotificationHandler
import co.selim.sns_handler.handler.message.SubscriptionConfirmationHandler
import co.selim.sns_handler.handler.message.UnsubscribeConfirmationHandler
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.http.HttpClient
import io.vertx.ext.web.RoutingContext

public interface SNSHandler : Handler<RoutingContext> {

  public fun setOnNotification(handler: NotificationHandler): SNSHandler
  public fun setOnSubscriptionConfirmation(handler: SubscriptionConfirmationHandler): SNSHandler
  public fun setOnUnsubscribeConfirmation(handler: UnsubscribeConfirmationHandler): SNSHandler

  public companion object {
    public fun create(httpClient: HttpClient): SNSHandler = SNSHandlerImpl(httpClient)
    public fun create(vertx: Vertx): SNSHandler = create(vertx.createHttpClient())
  }
}
