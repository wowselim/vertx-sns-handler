package co.selim.sns_handler.handler.message

import co.selim.sns_handler.model.UnsubscribeConfirmation

@FunctionalInterface
fun interface UnsubscribeConfirmationHandler {
  fun handle(subscriptionConfirmation: UnsubscribeConfirmation): Result

  sealed interface Result
  object Acknowledge : Result
  object Resubscribe : Result

  companion object {
    /**
     * Creates a UnsubscribeConfirmationHandler that only acknowledges requests (i.e. does not re-subscribe).
     */
    fun create(): UnsubscribeConfirmationHandler {
      return UnsubscribeConfirmationHandler { Acknowledge }
    }
  }
}
