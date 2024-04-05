package co.present.present.model

import androidx.annotation.ColorRes
import co.present.present.R

enum class Interest(val background: Background, val canonicalString: String) {
    Attend(Background.Yellow, "Attend"),
    Organize(Background.Red, "Organize"),
    EatDrink(Background.Red, "Eat & Drink"),
    Exercise(Background.Blue, "Exercise"),
    Live(Background.Green, "Live"),
    Learn(Background.Green, "Learn"),
    Shop(Background.Yellow, "Shop"),
    Volunteer(Background.Blue, "Volunteer"),
    Work(Background.Purple, "Work");

    enum class Background(@ColorRes val colorResId: Int) {
        Purple(R.color.presentPurple),
        Red(R.color.pink),
        Green(R.color.turquoise),
        Blue(R.color.blue),
        Yellow(R.color.yellow);
    }
}

fun String.toInterest(): Interest? {
    Interest.values().forEach { interest ->
        if (interest.canonicalString == this) return interest
    }
    return null
}

fun List<String>.filterInterestsOnly(): List<String> {
    val interests = Interest.values().map { it.canonicalString }
    return filter { interests.contains(it) }
}

