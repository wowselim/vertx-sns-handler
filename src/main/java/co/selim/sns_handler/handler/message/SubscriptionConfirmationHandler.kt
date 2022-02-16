package co.selim.sns_handler.handler.message

import co.selim.sns_handler.model.SubscriptionConfirmation

@FunctionalInterface
public fun interface SubscriptionConfirmationHandler {
  public fun handle(subscriptionConfirmation: SubscriptionConfirmation): Result

  public sealed interface Result
  public object Acknowledge : Result
  public object Ignore : Result

  public companion object {
    /**
     * Creates a SubscriptionConfirmationHandler that accepts every request (i.e. always subscribes).
     */
    public fun create(): SubscriptionConfirmationHandler {
      return SubscriptionConfirmationHandler { Acknowledge }
    }
  }
}
