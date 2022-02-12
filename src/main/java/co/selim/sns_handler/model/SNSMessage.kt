package co.selim.sns_handler.model

interface SNSMessage {
  val type: Type

  enum class Type(val value: String) {
    NOTIFICATION("Notification"),
    SUBSCRIPTION_CONFIRMATION("SubscriptionConfirmation"),
    UNSUBSCRIBE_CONFIRMATION("UnsubscribeConfirmation");

    override fun toString() = value

    companion object {
      fun fromTextForm(textForm: String): Type {
        val messageType = values().firstOrNull { textForm == it.value }
        return requireNotNull(messageType) {
          "Unknown message type $textForm. Must be one of [${values().map { it.value }.joinToString()}]."
        }
      }
    }
  }
}
