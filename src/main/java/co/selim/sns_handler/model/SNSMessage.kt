package co.selim.sns_handler.model

public sealed interface SNSMessage {
  public val type: Type

  public enum class Type(internal val value: String) {
    NOTIFICATION("Notification"),
    SUBSCRIPTION_CONFIRMATION("SubscriptionConfirmation"),
    UNSUBSCRIBE_CONFIRMATION("UnsubscribeConfirmation");

    override fun toString(): String = value

    internal companion object {
      fun fromTextForm(textForm: String): Type {
        val messageType = values().firstOrNull { textForm == it.value }
        return requireNotNull(messageType) {
          "Unknown message type $textForm. Must be one of [${values().map { it.value }.joinToString()}]."
        }
      }
    }
  }
}
