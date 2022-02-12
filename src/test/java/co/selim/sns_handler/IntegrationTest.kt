package co.selim.sns_handler

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
import io.vertx.core.Vertx
import io.vertx.core.http.HttpClient
import io.vertx.core.http.HttpServer
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.junit5.VertxExtension
import kong.unirest.Unirest
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.CountDownLatch

private const val PORT = 8745
private const val APPLICATION_JSON = "application/json"
private const val MESSAGE_TYPE_HEADER = "x-amz-sns-message-type"

@ExtendWith(VertxExtension::class)
class IntegrationTest {
  private var notification: Notification? = null
  private var subscriptionConfirmation: SubscriptionConfirmation? = null
  private var unsubscribeConfirmation: UnsubscribeConfirmation? = null
  private var urlVisited: Boolean = false
  private var onNotification: NotificationHandler? = null
  private var onSubscriptionConfirmation: SubscriptionConfirmationHandler? = null
  private var onUnsubscribeConfirmation: UnsubscribeConfirmationHandler? = null

  @BeforeEach
  fun setup(vertx: Vertx) {
    Unirest.config().defaultBaseUrl("http://localhost:$PORT")

    val router = Router.router(vertx)

    router.post("/sns").handler(BodyHandler.create(false))
      .handler(SNSHandler.create(vertx.createHttpClient())
        .setOnNotification { _notification ->
          notification = _notification
          onNotification?.handle(_notification) ?: NotificationHandler.Acknowledge
        }.setOnSubscriptionConfirmation { _subscriptionConfirmation ->
          subscriptionConfirmation = _subscriptionConfirmation
          onSubscriptionConfirmation?.handle(_subscriptionConfirmation) ?: SubscriptionConfirmationHandler.Acknowledge
        }.setOnUnsubscribeConfirmation { _unsubscribeNotification ->
          unsubscribeConfirmation = _unsubscribeNotification
          onUnsubscribeConfirmation?.handle(_unsubscribeNotification) ?: UnsubscribeConfirmationHandler.Acknowledge
        })

    router.get("/endpoint").handler { ctx ->
      urlVisited = true
      ctx.end()
    }

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(PORT)
      .block()
  }

  @AfterEach
  fun shutdown(vertx: Vertx) {
    Unirest.shutDown()
    vertx.close().block()
  }

  @Test
  fun `notifications are received`() {
    @Language("JSON") val notificationJson = """
      {
        "Type" : "Notification",
        "MessageId" : "22b80b92-fdea-4c2c-8f9d-bdfb0c7bf324",
        "TopicArn" : "arn:aws:sns:us-west-2:123456789012:MyTopic",
        "Subject" : "My First Message",
        "Message" : "Hello world!",
        "Timestamp" : "2012-05-02T00:54:06.655Z",
        "SignatureVersion" : "1",
        "Signature" : "EXAMPLEw6JRN...",
        "SigningCertURL" : "https://sns.us-west-2.amazonaws.com/SimpleNotificationService-f3ecfb7224c7233fe7bb5f59f96de52f.pem",
        "UnsubscribeURL" : "http://localhost:$PORT/endpoint"
      }
    """.trimIndent()

    val response = Unirest.post("/sns")
      .body(notificationJson).contentType(APPLICATION_JSON)
      .header(MESSAGE_TYPE_HEADER, SNSMessage.Type.NOTIFICATION.value)
      .asString()

    assertEquals(200, response.status)
    assertEquals(JsonObject(notificationJson).toNotification(), notification)
    assertFalse(urlVisited)
  }

  @Test
  fun `unsubscribe on notification is possible`() {
    onNotification = NotificationHandler {
      NotificationHandler.Unsubscribe
    }
    @Language("JSON") val notificationJson = """
      {
        "Type" : "Notification",
        "MessageId" : "22b80b92-fdea-4c2c-8f9d-bdfb0c7bf324",
        "TopicArn" : "arn:aws:sns:us-west-2:123456789012:MyTopic",
        "Subject" : "My First Message",
        "Message" : "Hello world!",
        "Timestamp" : "2012-05-02T00:54:06.655Z",
        "SignatureVersion" : "1",
        "Signature" : "EXAMPLEw6JRN...",
        "SigningCertURL" : "https://sns.us-west-2.amazonaws.com/SimpleNotificationService-f3ecfb7224c7233fe7bb5f59f96de52f.pem",
        "UnsubscribeURL" : "http://localhost:$PORT/endpoint"
      }
    """.trimIndent()

    val response = Unirest.post("/sns")
      .body(notificationJson).contentType(APPLICATION_JSON)
      .header(MESSAGE_TYPE_HEADER, SNSMessage.Type.NOTIFICATION.value)
      .asString()

    assertEquals(200, response.status)
    assertEquals(JsonObject(notificationJson).toNotification(), notification)
    assertTrue(urlVisited)
  }

  @Test
  fun `subscription requests are confirmed`() {
    @Language("JSON") val subscriptionConfirmationJson = """
      {
        "Type" : "SubscriptionConfirmation",
        "MessageId" : "165545c9-2a5c-472c-8df2-7ff2be2b3b1b",
        "Token" : "2336412f37...",
        "TopicArn" : "arn:aws:sns:us-west-2:123456789012:MyTopic",
        "Message" : "You have chosen to subscribe to the topic arn:aws:sns:us-west-2:123456789012:MyTopic.\nTo confirm the subscription, visit the SubscribeURL included in this message.",
        "SubscribeURL" : "http://localhost:$PORT/endpoint",
        "Timestamp" : "2012-04-26T20:45:04.751Z",
        "SignatureVersion" : "1",
        "Signature" : "EXAMPLEpH+DcEwjAPg8O9mY8dReBSwksfg2S7WKQcikcNKWLQjwu6A4VbeS0QHVCkhRS7fUQvi2egU3N858fiTDN6bkkOxYDVrY0Ad8L10Hs3zH81mtnPk5uvvolIC1CXGu43obcgFxeL3khZl8IKvO61GWB6jI9b5+gLPoBc1Q=",
        "SigningCertURL" : "https://sns.us-west-2.amazonaws.com/SimpleNotificationService-f3ecfb7224c7233fe7bb5f59f96de52f.pem"
      }
    """.trimIndent()

    val response = Unirest.post("/sns")
      .body(subscriptionConfirmationJson)
      .contentType(APPLICATION_JSON)
      .header(MESSAGE_TYPE_HEADER, SNSMessage.Type.SUBSCRIPTION_CONFIRMATION.value)
      .asString()

    assertEquals(200, response.status)
    assertEquals(JsonObject(subscriptionConfirmationJson).toSubscriptionConfirmation(), subscriptionConfirmation)
    assertTrue(urlVisited)
  }

  @Test
  fun `subscription requests are ignorable`() {
    onSubscriptionConfirmation = SubscriptionConfirmationHandler {
      SubscriptionConfirmationHandler.Ignore
    }
    @Language("JSON") val subscriptionConfirmationJson = """
      {
        "Type" : "SubscriptionConfirmation",
        "MessageId" : "165545c9-2a5c-472c-8df2-7ff2be2b3b1b",
        "Token" : "2336412f37...",
        "TopicArn" : "arn:aws:sns:us-west-2:123456789012:MyTopic",
        "Message" : "You have chosen to subscribe to the topic arn:aws:sns:us-west-2:123456789012:MyTopic.\nTo confirm the subscription, visit the SubscribeURL included in this message.",
        "SubscribeURL" : "http://localhost:$PORT/endpoint",
        "Timestamp" : "2012-04-26T20:45:04.751Z",
        "SignatureVersion" : "1",
        "Signature" : "EXAMPLEpH+DcEwjAPg8O9mY8dReBSwksfg2S7WKQcikcNKWLQjwu6A4VbeS0QHVCkhRS7fUQvi2egU3N858fiTDN6bkkOxYDVrY0Ad8L10Hs3zH81mtnPk5uvvolIC1CXGu43obcgFxeL3khZl8IKvO61GWB6jI9b5+gLPoBc1Q=",
        "SigningCertURL" : "https://sns.us-west-2.amazonaws.com/SimpleNotificationService-f3ecfb7224c7233fe7bb5f59f96de52f.pem"
      }
    """.trimIndent()

    val response = Unirest.post("/sns")
      .body(subscriptionConfirmationJson)
      .contentType(APPLICATION_JSON)
      .header(MESSAGE_TYPE_HEADER, SNSMessage.Type.SUBSCRIPTION_CONFIRMATION.value)
      .asString()

    assertEquals(200, response.status)
    assertEquals(JsonObject(subscriptionConfirmationJson).toSubscriptionConfirmation(), subscriptionConfirmation)
    assertFalse(urlVisited)
  }

  @Test
  fun `unsubscribe requests are acknowledged`() {
    @Language("JSON") val unsubscribeConfirmationJson = """
      {
        "Type" : "UnsubscribeConfirmation",
        "MessageId" : "47138184-6831-46b8-8f7c-afc488602d7d",
        "Token" : "2336412f37...",
        "TopicArn" : "arn:aws:sns:us-west-2:123456789012:MyTopic",
        "Message" : "You have chosen to deactivate subscription arn:aws:sns:us-west-2:123456789012:MyTopic:2bcfbf39-05c3-41de-beaa-fcfcc21c8f55.\nTo cancel this operation and restore the subscription, visit the SubscribeURL included in this message.",
        "SubscribeURL" : "http://localhost:$PORT/endpoint",
        "Timestamp" : "2012-04-26T20:06:41.581Z",
        "SignatureVersion" : "1",
        "Signature" : "EXAMPLEHXgJm...",
        "SigningCertURL" : "https://sns.us-west-2.amazonaws.com/SimpleNotificationService-f3ecfb7224c7233fe7bb5f59f96de52f.pem"
      }
    """.trimIndent()

    val response = Unirest.post("/sns")
      .body(unsubscribeConfirmationJson)
      .contentType(APPLICATION_JSON)
      .header(MESSAGE_TYPE_HEADER, SNSMessage.Type.UNSUBSCRIBE_CONFIRMATION.value)
      .asString()

    assertEquals(200, response.status)
    assertEquals(JsonObject(unsubscribeConfirmationJson).toUnsubscribeConfirmation(), unsubscribeConfirmation)
    assertFalse(urlVisited)
  }

  @Test
  fun `resubscribe on unsubscribe confirmation is possible`() {
    onUnsubscribeConfirmation = UnsubscribeConfirmationHandler {
      UnsubscribeConfirmationHandler.Resubscribe
    }
    @Language("JSON") val unsubscribeConfirmationJson = """
      {
        "Type" : "UnsubscribeConfirmation",
        "MessageId" : "47138184-6831-46b8-8f7c-afc488602d7d",
        "Token" : "2336412f37...",
        "TopicArn" : "arn:aws:sns:us-west-2:123456789012:MyTopic",
        "Message" : "You have chosen to deactivate subscription arn:aws:sns:us-west-2:123456789012:MyTopic:2bcfbf39-05c3-41de-beaa-fcfcc21c8f55.\nTo cancel this operation and restore the subscription, visit the SubscribeURL included in this message.",
        "SubscribeURL" : "http://localhost:$PORT/endpoint",
        "Timestamp" : "2012-04-26T20:06:41.581Z",
        "SignatureVersion" : "1",
        "Signature" : "EXAMPLEHXgJm...",
        "SigningCertURL" : "https://sns.us-west-2.amazonaws.com/SimpleNotificationService-f3ecfb7224c7233fe7bb5f59f96de52f.pem"
      }
    """.trimIndent()

    val response = Unirest.post("/sns")
      .body(unsubscribeConfirmationJson)
      .contentType(APPLICATION_JSON)
      .header(MESSAGE_TYPE_HEADER, SNSMessage.Type.UNSUBSCRIBE_CONFIRMATION.value)
      .asString()

    assertEquals(200, response.status)
    assertEquals(JsonObject(unsubscribeConfirmationJson).toUnsubscribeConfirmation(), unsubscribeConfirmation)
    assertTrue(urlVisited)
  }

  private fun <T : Any?> Future<out T>.block(): T? {
    val latch = CountDownLatch(1)
    var value: T? = null
    onSuccess {
      value = it
      latch.countDown()
    }
    latch.await()
    return value
  }
}
