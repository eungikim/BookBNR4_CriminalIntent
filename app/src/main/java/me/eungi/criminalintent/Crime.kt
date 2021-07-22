package me.eungi.criminalintent

import java.util.*
import kotlin.random.Random

data class Crime(
    val id: UUID = UUID.randomUUID(),
    var title: String = "",
    var date: Date = Date(),
    var isSolved: Boolean = false,
    val callPolice: Boolean = Random.nextInt(10) < 3
)