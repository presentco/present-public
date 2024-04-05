package co.present.present.model

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity
class Blocked(@PrimaryKey(autoGenerate = true) var id: Long = 0, var userId: String)