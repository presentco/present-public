package co.present.present.feature.create

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import toLocalDateTime


class DatePickerFragment : DialogFragment(), DatePickerDialog.OnDateSetListener {

    val date: Long by lazy { arguments!!.getLong("date") }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dateTime = date.toLocalDateTime()

        // Create a new instance of DatePickerDialog and return it.
        // Note that LocalDateTime.getMonthValue() returns 1 to 12, but DatePickerDialog(...month ...)
        // requires a 0-indexed value 🙄
        return DatePickerDialog(activity,
                this,
                dateTime.year,
                dateTime.monthValue - 1, // getMonthValue() is 1 to 12
                dateTime.dayOfMonth).apply {
            datePicker.minDate = date
        }
    }

    override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int) {
        if (activity is OnDateSetListener) {
            // Add 1 to month to return expected date -- LocalDateTime.getMonthValue() returns 1 to 12
            // but DatePicker's internal month is 0-indexed   
            (activity as OnDateSetListener).onDateSet(tag, date, year, month + 1, day)
        } else {
            error("Activity hosting DatePickerFragment must implement DatePickerFragment.OnDateSetListener")
        }
    }

    interface OnDateSetListener {
        fun onDateSet(tag: String?, oldDate: Long, year: Int, month: Int, day: Int)
    }

    companion object {
        fun newInstance(timeMillis: Long): DatePickerFragment {

            val bundle = Bundle().apply {
                putLong("date", timeMillis)
            }
            return DatePickerFragment().apply { arguments = bundle }
        }
    }

}