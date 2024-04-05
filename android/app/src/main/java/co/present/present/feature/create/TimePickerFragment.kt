package co.present.present.feature.create

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.text.format.DateFormat
import android.widget.TimePicker
import toLocalDateTime


class TimePickerFragment : DialogFragment(), TimePickerDialog.OnTimeSetListener {

    val date: Long by lazy { arguments!!.getLong("date") }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val local = date.toLocalDateTime()

        // Create a new instance of TimePickerDialog and return it
        return TimePickerDialog(activity, this, local.hour, local.minute,
                DateFormat.is24HourFormat(activity))
    }

    override fun onTimeSet(view: TimePicker, hourOfDay: Int, minute: Int) {
        if (activity is TimePickerFragment.OnTimeSetListener) {
            (activity as TimePickerFragment.OnTimeSetListener).onTimeSet(tag, date, hourOfDay, minute)
        } else {
            error("Activity hosting TimePickerFragment must implement TimePickerFragment.OnTimeSetListener")
        }
    }

    interface OnTimeSetListener {
        fun onTimeSet(tag: String?, oldDate: Long, hourOfDay: Int, minute: Int)
    }

    companion object {

        fun newInstance(timeMillis: Long): TimePickerFragment {
            val bundle = Bundle().apply {
                putLong("date", timeMillis)
            }
            return TimePickerFragment().apply { arguments = bundle }
        }
    }
}