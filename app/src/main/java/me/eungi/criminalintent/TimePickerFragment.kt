package me.eungi.criminalintent

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import java.util.*

private const val ARG_DATE = "time"

class TimePickerFragment : DialogFragment() {

    interface Callbacks {
        fun onTimeSelected(date: Date)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val timeListener =
            TimePickerDialog.OnTimeSetListener { _: TimePicker, hourOfDay: Int, minute: Int ->
                val resultTime: Date = GregorianCalendar(0, 0, 0, hourOfDay, minute).time
                targetFragment?.let { fragment ->
                    (fragment as Callbacks).onTimeSelected(resultTime)
                }
            }
        val date = arguments?.getSerializable(ARG_DATE) as Date
        val calendar = Calendar.getInstance()
        calendar.time = date
        val initialHourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
        val initialMinute = calendar.get(Calendar.MINUTE)

        return TimePickerDialog(
            requireContext(),
            timeListener,
            initialHourOfDay,
            initialMinute,
            false
        )
    }

    companion object {
        fun newInstance(date: Date): TimePickerFragment {
            val args = Bundle().apply {
                putSerializable(ARG_DATE, date)
            }
            return TimePickerFragment().apply {
                arguments = args
            }
        }
    }

}