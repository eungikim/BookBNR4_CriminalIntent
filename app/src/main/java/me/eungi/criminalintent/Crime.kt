package me.eungi.criminalintent

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*
import kotlin.random.Random

@Entity
data class Crime(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    var title: String = "",
    var date: Date = Date(),
    var isSolved: Boolean = false,
    val callPolice: Boolean = Random.nextInt(10) < 3,
    var suspect: String = ""
)