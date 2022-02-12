package co.selim.sns_handler.handler.message

import co.selim.sns_handler.model.SubscriptionConfirmation

@FunctionalInterface
fun interface SubscriptionConfirmationHandler {
  fun handle(subscriptionConfirmation: SubscriptionConfirmation): Result

  sealed interface Result
  object Acknowledge : Result
  object Ignore : Result

  companion object {
    /**
     * Creates a SubscriptionConfirmationHandler that accepts every request (i.e. always subscribes).
     */
    fun create(): SubscriptionConfirmationHandler {
      return SubscriptionConfirmationHandler { Acknowledge }
    }
  }
}
