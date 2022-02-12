package co.selim.sns_handler.handler.message

import co.selim.sns_handler.model.Notification

@FunctionalInterface
fun interface NotificationHandler {
  fun handle(notification: Notification): Result

  sealed interface Result
  object Acknowledge : Result
  object Unsubscribe : Result

  companion object {
    /**
     * Creates a NotificationHandler that acknowledges every request.
     */
    fun create(): NotificationHandler {
      return NotificationHandler { Acknowledge }
    }
  }
}
