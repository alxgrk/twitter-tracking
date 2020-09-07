package de.alxgrk.twittertracking

import android.content.SharedPreferences
import android.util.Log
import com.android.volley.Request.Method.POST
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.fasterxml.jackson.core.type.TypeReference
import de.alxgrk.twittertracking.BuildConfig.API_URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class SessionStore(private val sharedPreferences: SharedPreferences) {

    var userId: String?
        get() = sharedPreferences.getString("userId", null)
        set(value) = sharedPreferences.edit().putString("userId", value).apply()

    var sessionState
        get() = SessionState.valueOf(
            sharedPreferences.getString(
                "sessionState",
                SessionState.NEW.toString()
            )!!
        )
        set(value) = sharedPreferences.edit().putString("sessionState", value.toString()).apply()

    var currentActivity
        get() = ActivitiesOfInterest.parse(
            sharedPreferences.getString(
                "currentActivity",
                ActivitiesOfInterest.UNKNOWN_ACTIVITY.toString()
            )!!
        )
        set(value) = sharedPreferences.edit().putString("currentActivity", value.toString()).apply()

    var lastScrollY
        get() = sharedPreferences.getInt("lastScrollY", 0)
        set(value) = sharedPreferences.edit().putInt("lastScrollY", value).apply()

    var searchTextEnteredManually: String?
        get() = sharedPreferences.getString("searchTextEnteredManually", null)
        set(value) = sharedPreferences.edit().putString("searchTextEnteredManually", value).apply()
}

class EventRepository(private val filesDir: File, private val requestQueue: RequestQueue?) {

    val localJsonFile =
        File(filesDir, TAG).apply { Log.d(TAG, "Local JsonFile path is '${absolutePath}'") }

    fun clear() = localJsonFile.delete()

    suspend fun listAll(): List<Map<String, Any>> =
        withContext(Dispatchers.IO) {
            val file = File(filesDir, TAG)
            if (file.exists())
                file.useLines { seq ->
                    seq.mapIndexed { index, it ->
                        Log.d(TAG, "Line ${index + 1}: $it")
                        OBJECT_MAPPER.readValue(it, object : TypeReference<Map<String, Any>>() {})
                    }.toList().asReversed()
                }
            else
                listOf()
        }

    fun publish(event: Event) {
        val eventAsString = OBJECT_MAPPER.writeValueAsString(event)

        Log.i(TAG, "Publishing event $eventAsString")

        writeToFile(eventAsString)
        emitToServer(eventAsString)
    }

    private fun writeToFile(eventAsString: String) {
        localJsonFile.appendText(eventAsString + "\n")
    }

    private fun emitToServer(eventAsString: String) {
        val request = object : StringRequest(POST, API_URL,
            {
                Log.d(TAG, "Request acknowledged.")
            },
            { error ->
                Log.e(TAG, "Request failed: \"${error.message}\" (${error.cause})")
                Log.d(TAG, "Response was: ${error.networkResponse?.statusCode} - ${error.networkResponse?.data?.let { String(it) }}")
            }) {

            override fun getBodyContentType() = "application/json; charset=utf-8"

            override fun getBody() = eventAsString.toByteArray()
        }
        requestQueue?.add(request)
    }

    companion object {
        private val TAG = EventRepository::class.java.simpleName
    }
}