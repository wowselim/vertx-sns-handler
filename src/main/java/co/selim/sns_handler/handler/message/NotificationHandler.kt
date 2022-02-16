package co.selim.sns_handler.handler.message

import co.selim.sns_handler.model.Notification

@FunctionalInterface
public fun interface NotificationHandler {
  public fun handle(notification: Notification): Result

  public sealed interface Result
  public object Acknowledge : Result
  public object Unsubscribe : Result

  public companion object {
    /**
     * Creates a NotificationHandler that acknowledges every request.
     */
    public fun create(): NotificationHandler {
      return NotificationHandler { Acknowledge }
    }
  }
}
