package co.present.present.feature

import android.content.Context
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import androidx.collection.LongSparseArray
import co.present.present.feature.invite.Contact
import co.present.present.location.ContactPermissions
import co.present.present.model.User
import io.reactivex.Flowable
import present.proto.AddContactsRequest
import present.proto.ContactRequest
import present.proto.UserService

interface GetContacts {
    val localContacts: Flowable<List<Contact>>
    val usersWhoAreContacts: Flowable<out Map<User, Contact?>>
}

class ContactsImpl(val userService: UserService, val context: Context): GetContacts {

    /**
     * Get Present members by phone number lookup, so we can tell which of the phone contacts
     * are also Present users.
     */
    override val usersWhoAreContacts: Flowable<out Map<User, Contact?>> by lazy {
        localContacts.map { localContacts ->
            val contactsRequests = mutableListOf<ContactRequest>()
            localContacts.forEach { contact ->
                contact.phones.values.forEach { phoneNumber ->
                    contactsRequests += ContactRequest(phoneNumber, contact.displayName, contact.firstName, contact.lastName)
                }
            }
            val response = userService.addContacts(AddContactsRequest(contactsRequests))
            mutableMapOf<User, Contact?>().apply {
                response.results.forEach { phoneUserResponse ->
                    val contact = localContacts.first { it.phones.containsValue(phoneUserResponse.phoneNumber) }
                    put(User(phoneUserResponse.user), contact)
                }
            }
        }.replay(1).autoConnect()
    }

    override val localContacts: Flowable<List<Contact>> get() =
        if (ContactPermissions.isGranted(context)) {
            localContactsWithPermission
        } else {
            Flowable.just(listOf())
        }

    private val localContactsWithPermission: Flowable<List<Contact>> by lazy {
        Flowable.fromCallable {
            retrieveContactList().filter { it.phones.isNotEmpty() && it.displayName.isNotEmpty() }
                    .sortedBy { it.displayName }
        }.replay(1).autoConnect()
    }

    private fun retrieveContactList(): List<Contact> {

        val list = ArrayList<Contact>()
        val array = LongSparseArray<Contact>()

        val set = HashSet<String>()
        set.add(ContactsContract.Data.MIMETYPE)
        set.add(ContactsContract.Data.CONTACT_ID)
        set.add(ContactsContract.Data.PHOTO_ID)
        set.add(ContactsContract.CommonDataKinds.Phone.NUMBER)
        set.add(ContactsContract.CommonDataKinds.Phone.TYPE)
        set.add(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME)
        set.add(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME)
        set.add(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME)
        set.add(ContactsContract.Contacts.PHOTO_ID)
        set.add(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)

        val uri = ContactsContract.Data.CONTENT_URI
        val projection = set.toTypedArray()
        val selection = ContactsContract.Data.MIMETYPE + " in (?, ?) AND " + ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME + " IS NOT NULL"
        val selectionArgs = arrayOf(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
        val sortOrder = ContactsContract.Contacts.SORT_KEY_ALTERNATIVE

        val cursor = context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
        )

        val mimeTypeIdx = cursor.getColumnIndex(ContactsContract.Data.MIMETYPE)
        val idIdx = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID)
        val phoneIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val phoneTypeIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
        val givenNameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME)
        val familyNameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME)
        val displayNameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME)
        val photoIdIdx = cursor.getColumnIndex(ContactsContract.Data.PHOTO_ID)
        val thumbnailIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)

        while (cursor.moveToNext()) {

            val id = cursor.getLong(idIdx)
            var addressBookContact = array.get(id)

            if (addressBookContact == null) {
                val photoId = cursor.getLong(photoIdIdx)
                val thumbUri = cursor.getString(thumbnailIndex)
                addressBookContact = Contact(id, photoId, thumbUri)
                array.put(id, addressBookContact)
                list.add(addressBookContact)
            }

            when (cursor.getString(mimeTypeIdx)) {
                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> {
                    // row's data: see ContactsContract.CommonDataKinds.Phone
                    val rawPhoneNumber = cursor.getString(phoneIdx)
                    addressBookContact.addPhone(cursor.getInt(phoneTypeIdx), normalizePhoneNumber(rawPhoneNumber))
                }
                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE ->
                    // row's data: see ContactsContract.CommonDataKinds.StructuredName
                    addressBookContact.addName(cursor.getString(displayNameIndex), cursor.getString(givenNameIdx), cursor.getString(familyNameIdx))
            }
        }

        cursor.close()

        return list
    }

    /**
     * Takes a user-generated phone number from contacts, which might be in forms like:
     * +1(603)-770-8302 (with a plus)
     * (603)-770-8302
     * 6037708302 (no plus or country code)
     *
     * and gives back this:
     * 16037708302
     */
    private fun normalizePhoneNumber(rawPhoneNumber: String): String {
        val strippedNumber = PhoneNumberUtils.stripSeparators(rawPhoneNumber)
        val withoutPlus = strippedNumber.substring(if (strippedNumber.startsWith("+")) 1 else 0)
        return if (withoutPlus.length == 10) "1$withoutPlus" else withoutPlus
    }

//    private fun getHumanReadablePhoneType(phoneType: Int): String {
//        return context.getString(
//                when (phoneType) {
//                    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> R.string.type_mobile
//                    ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> R.string.type_home
//                    ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> R.string.type_work
//                    else -> R.string.empty
//                }
//        )
//    }


}