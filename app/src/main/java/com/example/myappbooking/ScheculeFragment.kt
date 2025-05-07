package com.example.myappbooking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.whenStarted
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myappbooking.databinding.FragmentScheculeBinding
import com.kizitonwose.calendar.core.*
import com.kizitonwose.calendar.view.CalendarView
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.ViewContainer
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.*

class ScheduleFragment : Fragment() {

    // ViewBinding property
    private var _binding: FragmentScheculeBinding? = null
    private val binding get() = _binding!!

    private lateinit var eventsAdapter: EventsAdapter
    private val eventsList = mutableListOf<BookingData>()

    private var selectedDate: LocalDate? = null
    private val today = LocalDate.now()
    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
    private val eventDates = mutableSetOf<LocalDate>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Initialize the binding
        _binding = FragmentScheculeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCategoryRecyclerView()

        binding.swipeRefreshLayout.setOnRefreshListener{
            selectedDate?.let { updateEventsForDate(it) }
            binding.swipeRefreshLayout.isRefreshing = false
        }

        // Set up navigation buttons
        binding.btnPrevMonth.setOnClickListener {
            binding.calendarView.findFirstVisibleMonth()?.let {
                binding.calendarView.smoothScrollToMonth(it.yearMonth.previousMonth)
            }
        }

        binding.btnNextMonth.setOnClickListener {
            binding.calendarView.findFirstVisibleMonth()?.let {
                binding.calendarView.smoothScrollToMonth(it.yearMonth.nextMonth)
            }
        }

        // Set up the calendar
        setupCalendar()
    }

    private fun setupCategoryRecyclerView() {
        // Initialize adapter
        eventsAdapter = EventsAdapter(eventsList)
        binding.rvEvents.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = eventsAdapter
        }
    }

    private fun setupCalendar() {
        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(12)
        val endMonth = currentMonth.plusMonths(12)
        val firstDayOfWeek = daysOfWeek().first() // Usually Monday for most locales

        binding.calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view) { date ->
                selectDate(date)
            }

            override fun bind(container: DayViewContainer, data: CalendarDay) {
                container.day = data
                val textView = container.textView
                textView.text = data.date.dayOfMonth.toString()

                if (data.position == DayPosition.MonthDate) {
                    // Show the month dates
                    textView.visibility = View.VISIBLE

//                    if (eventDates.contains(data.date)) {
//                        container.view.findViewById<View>(R.id.dotView).visibility = View.VISIBLE
//                    } else {
//                        container.view.findViewById<View>(R.id.dotView).visibility = View.GONE
//                    }

                    when {
                        selectedDate == data.date -> {
                            // Selected date
                            textView.setBackgroundResource(R.drawable.selected_day_background)
                            textView.setTextColor(resources.getColor(android.R.color.white, null))
                        }
                        today == data.date -> {
                            // Today
                            textView.background = null
//                            textView.setBackgroundResource(R.drawable.background_today)
                            textView.setTextColor(resources.getColor(android.R.color.holo_blue_dark, null))
                        }
                        else -> {
                            // Normal day
                            textView.background = null
                            textView.setTextColor(resources.getColor(android.R.color.black, null))
                        }
                    }

                    if (eventDates.contains(data.date)) {
                        container.view.findViewById<View>(R.id.dotView).visibility = View.VISIBLE
                    } else {
                        container.view.findViewById<View>(R.id.dotView).visibility = View.GONE
                    }
                } else {
                    // Make out-of-month dates more subtle
                    textView.background = null
                    textView.setTextColor(resources.getColor(android.R.color.darker_gray, null))
                container.view.findViewById<View>(R.id.dotView).visibility = View.GONE
                }
            }
        }

        // Setup month scroll listener
        binding.calendarView.monthScrollListener = { month ->
            binding.tvMonthYear.text = monthFormatter.format(month.yearMonth)
        }

        // Setup calendar with initial params
        binding.calendarView.setup(startMonth, endMonth, firstDayOfWeek)
        binding.calendarView.scrollToMonth(currentMonth)

        // Initial selection
        selectDate(today)
    }

    private fun selectDate(date: LocalDate) {
        if (selectedDate != date) {
            val oldDate = selectedDate
            selectedDate = date

            oldDate?.let { binding.calendarView.notifyDateChanged(it) }
            binding.calendarView.notifyDateChanged(date)

            // Update events for the selected date
            updateEventsForDate(date)
        }
    }

    private fun updateEventsForDate(date: LocalDate) {
            // Format the selected date to yyyy-MM-dd for comparison
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val selectedDateStr = date.format(formatter)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    try {
                        val prefMan = SharedPreferencesManager.getInstance(requireContext())
                        val token = prefMan.getAuthToken()

                        if (token != null) {
                            val authHeader = "Bearer $token"
                            val response = ApiClient.authService.getApprovedEvent(authHeader)

                            if (response.isSuccessful && response.body() != null) {
                                val allEvents = response.body()!!.data

                                eventDates.clear()
                                eventDates.addAll(
                                    allEvents.map {
                                        LocalDate.parse(it.start_datetime.substring(0, 10))
                                    }
                                )

                                binding.calendarView.notifyCalendarChanged()

                                // Filter events for the selected date
                                val filteredEvents = allEvents.filter { event ->
                                    // Extract date part from start_datetime (first 10 characters of yyyy-MM-dd HH:mm:ss)
                                    val eventDate = event.start_datetime.substring(0, 10)
                                    eventDate == selectedDateStr
                                }

                                // Update RecyclerView with filtered events
                                eventsList.clear()
                                eventsList.addAll(filteredEvents)
                                eventsAdapter.notifyDataSetChanged()

                                binding.notFoundCard.visibility = View.GONE

                                // Show a message if no events for this date
                                if (filteredEvents.isEmpty()) {
//                                    Toast.makeText(requireContext(), "No events for ${date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}", Toast.LENGTH_SHORT).show()
                                    binding.notFoundCard.visibility = View.VISIBLE
                                }
                            } else {
                                Toast.makeText(requireContext(), "Failed to fetch events", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
//                        binding.tvUpcomingEvents.text = "Failed to Load, please refresh or tap a date"
//                        Toast.makeText(requireContext(), "Failed to Load, please refresh or tap a date", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun daysOfWeek(): Array<DayOfWeek> {
        val firstDay = WeekFields.of(Locale.getDefault()).firstDayOfWeek
        var day = firstDay

        return Array(7) {
            val current = day
            day = if (day == DayOfWeek.SUNDAY) DayOfWeek.MONDAY else DayOfWeek.of(day.value + 1)
            current
        }
    }

    // Clean up binding when fragment is destroyed
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // View container for calendar day cells
    inner class DayViewContainer(view: View, private val onDateClick: (LocalDate) -> Unit) : ViewContainer(view) {
        val textView: TextView = view.findViewById(R.id.calendarDayText)
        lateinit var day: CalendarDay

        init {
            view.setOnClickListener {
                if (day.position == DayPosition.MonthDate) {
                    onDateClick(day.date)
                }
            }
        }
    }
}