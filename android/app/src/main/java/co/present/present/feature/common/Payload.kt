package co.present.present.feature.common

sealed class Payload {
    object Joined
    object Checked
    object DontUpdate
    object FullBind
}