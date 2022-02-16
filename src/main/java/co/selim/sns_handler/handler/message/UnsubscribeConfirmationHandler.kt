package co.selim.sns_handler.handler.message

import co.selim.sns_handler.model.UnsubscribeConfirmation

@FunctionalInterface
public fun interface UnsubscribeConfirmationHandler {
  public fun handle(subscriptionConfirmation: UnsubscribeConfirmation): Result

  public sealed interface Result
  public object Acknowledge : Result
  public object Resubscribe : Result

  public companion object {
    /**
     * Creates a UnsubscribeConfirmationHandler that only acknowledges requests (i.e. does not re-subscribe).
     */
    public fun create(): UnsubscribeConfirmationHandler {
      return UnsubscribeConfirmationHandler { Acknowledge }
    }
  }
}
