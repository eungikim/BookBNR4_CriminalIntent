package me.eungi.criminalintent

import android.app.Application

class CriminalIntentApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrimeRepository.initialize(applicationContext)
        setDateTimeFormatString()
    }

    private fun setDateTimeFormatString() {
        Constants.dateFormat = getString(R.string.date_format)
        Constants.timeFormat = getString(R.string.time_format)
        Constants.dateTimeFormat = getString(R.string.date_time_format)
    }
}