package co.present.present.feature.invite

data class Contact(val id: Long,
                   val photoId: Long,
                   val thumbUri: String?,
                   val phones: MutableMap<Int, String> = mutableMapOf(),
                   var displayName: String = "",
                   var firstName: String? = null,
                   var lastName: String? = null) {
    fun addPhone(int: Int, string: String) {
        phones[int] = string
    }

    fun addName(display: String, first: String?, last: String?) {
        displayName = display
        firstName = first
        lastName = last
    }
}