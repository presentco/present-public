package co.present.present.support

import co.present.present.model.User
import co.present.present.model.toInterest


fun getUser() = User(
        id = "id",
        bio = "bio",
        firstName = "Lisa",
        name = "Lisa Wray",
        photo = "http://present.co/photo",
        interests = listOf("Work", "Live").mapNotNull { it.toInterest() },
        link = "http://present.co/u/sdjfjkdsfj",
        member = true)