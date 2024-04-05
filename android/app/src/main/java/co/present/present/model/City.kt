package co.present.present.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import present.proto.City

@Entity
data class City(@PrimaryKey(autoGenerate = true) var id: Int = 0,
                var name: String,
                var latitude: Double,
                var longitude: Double,
                var radius: Double) {

    @Ignore
    constructor(city: City) : this(
            name = city.name,
            latitude = city.location.latitude,
            longitude = city.location.longitude,
            radius = city.radius)

}