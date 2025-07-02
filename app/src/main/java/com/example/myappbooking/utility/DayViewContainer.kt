package com.example.myappbooking.utility

import android.view.View
import com.example.myappbooking.databinding.CalendarDayLayoutBinding
import com.kizitonwose.calendar.view.ViewContainer

class DayViewContainer(view: View) : ViewContainer(view) {
//    val textView = view.findViewById<TextView>(R.id.calendarDayText)

    // With ViewBinding
        val textView = CalendarDayLayoutBinding.bind(view).calendarDayText


}