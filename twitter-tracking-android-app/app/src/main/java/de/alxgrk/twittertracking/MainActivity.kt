package de.alxgrk.twittertracking

import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.FileObserver
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import de.alxgrk.twittertracking.MainActivity.EventAdapter.EventViewHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.coroutines.CoroutineContext


class MainActivity : AppCompatActivity() {

    private lateinit var eventRepository: EventRepository
    private lateinit var fileObserver: FileObserver
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: EventAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!isAccessibilityServiceOn()) {
            Toast.makeText(
                this,
                "Please enable the accessibility settings in order to make tracking possible.",
                Toast.LENGTH_LONG
            ).show()
            val openSettingsIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(openSettingsIntent)
        }

        eventRepository = EventRepository(applicationContext.filesDir, null)

        viewManager = LinearLayoutManager(this)
        viewAdapter = EventAdapter(mutableListOf())
        GlobalScope.launch {
            val currentEvents = eventRepository.listAll()
            refreshEvents(currentEvents)
            Logger.i("Number of stored events: ${currentEvents.size}")
        }

        recyclerView = findViewById<RecyclerView>(R.id.rv_click_activity).apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
            addItemDecoration(DividerItemDecoration(context, VERTICAL))
        }
        fileObserver = createFileObserver(eventRepository)
        fileObserver.startWatching()
    }

    override fun onStart() {
        super.onStart()
        fileObserver.startWatching()
    }

    override fun onResume() {
        super.onResume()
        GlobalScope.launch {
            refreshEvents(eventRepository.listAll())
        }
        fileObserver.startWatching()
    }

    private suspend fun refreshEvents(events: List<Map<String, Any>>) = withContext(Dispatchers.Main) {
        viewAdapter.events.clear()
        viewAdapter.events.addAll(events)
        viewAdapter.notifyDataSetChanged()
    }

    override fun onPause() {
        super.onPause()
        fileObserver.stopWatching()
    }

    override fun onStop() {
        fileObserver.stopWatching()
        super.onStop()
    }

    @Suppress("DEPRECATION")
    private fun createFileObserver(eventRepository: EventRepository): FileObserver {
        return object :
            FileObserver(eventRepository.localJsonFile.absolutePath, MODIFY) {
            override fun onEvent(event: Int, path: String?) {
                Logger.d("Event repository was updated.")
                GlobalScope.launch {
                    val newEvents = eventRepository.listAll()
                        .filter { new ->
                            viewAdapter.events.none { old ->
                                old["action"] == new["action"] && old["timestamp"] == new["timestamp"]
                            }
                        }
                    if (newEvents.isNotEmpty()) {
                        viewAdapter.events.addAll(0, newEvents)
                        withContext(Dispatchers.Main) {
                            viewAdapter.notifyItemRangeInserted(
                                0,
                                newEvents.size
                            )
                        }
                    }
                }
            }
        }
    }

    class EventAdapter(val events: MutableList<Map<String, Any>>) : Adapter<EventViewHolder>() {

        class EventViewHolder(
            view: View,
            val tvTitle: TextView = view.findViewById(R.id.tv_title),
            val tvSubtitle: TextView = view.findViewById(R.id.tv_subtitle),
            val tvTime: TextView = view.findViewById(R.id.tv_time)
        ) : RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item, parent, false)
            return EventViewHolder(view)
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
            val event = events[position]

            val timestamp = LocalDateTime.parse(event["timestamp"].toString())
            val action = event["action"].toString()
            val target = event["target"]?.toString()
            val selector = event["selector"]?.toString()
            val estimatedTweetsScrolled =
                event["estimatedTweetsScrolled"]?.let { "estimatedTweetsScrolled: $it" }

            holder.tvTitle.text = action
            holder.tvSubtitle.text =
                target ?: selector ?: estimatedTweetsScrolled ?: "unknown origin"
            holder.tvTime.text =
                timestamp.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))
        }

        override fun getItemCount() = events.size
    }

    companion object {

        fun Context.isAccessibilityServiceOn(): Boolean {
            val am = (getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager?)
            require(am != null) { "AccessibilityManager must not be null" }

            return am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                .map { it.resolveInfo.serviceInfo }
                .any { it.packageName == packageName && it.name == TwitterTrackingAccessibilityService::class.java.name }
        }

    }
}
