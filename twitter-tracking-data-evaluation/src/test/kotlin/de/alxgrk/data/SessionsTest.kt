package de.alxgrk.data

import io.inbot.eskotlinwrapper.AsyncIndexRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals

@ExtendWith(MockKExtension::class)
class SessionsTest {

    @MockK
    lateinit var mockRepo: AsyncIndexRepository<Event>

    private fun withAnalyse(block: suspend Analyse.() -> Unit) {
        runBlocking {
            mockk<Analyse>(relaxed = true) {
                every { debug } returns false
                every { refresh } returns true
            }
                .block()
        }
    }

    @Test
    fun `extraction with start and end works`() =
        withAnalyse {
            val userId = UserId("123")
            val start = Event(Event.EventType.ANDROID, "123", "session_start", "2020-11-03T14:00:05.603Z")
            val scroll = Event(Event.EventType.ANDROID, "123", "scroll", "2020-11-03T14:01:05.603Z")
            val click = Event(Event.EventType.ANDROID, "123", "click", "2020-11-03T14:02:05.603Z")
            val end = Event(Event.EventType.ANDROID, "123", "session_end", "2020-11-03T14:03:05.603Z")
            coEvery { mockRepo.search(scrolling = any(), block = any()) } returns mockk(relaxed = true) {
                every { mappedHits } returns flowOf(start, scroll, click, end)
            }

            val uut = Sessions(mockRepo)
            val (sessions) = with(uut) { extract(userId) }

            assertEquals(1, sessions.size)
            assertEquals(start, sessions[0].sessionStartEvent)
            assertEquals(end, sessions[0].sessionEndEvent)
            assertEquals(2, sessions[0].sessionEventsInChronologicalOrder.size)
            assertEquals(scroll, sessions[0].sessionEventsInChronologicalOrder[0])
            assertEquals(click, sessions[0].sessionEventsInChronologicalOrder[1])
        }

    @Test
    fun `extraction with start but without end works`() = withAnalyse {

        val userId = UserId("123")
        val start = Event(Event.EventType.ANDROID, "123", "session_start", "2020-11-03T14:00:05.603Z")
        val scroll = Event(Event.EventType.ANDROID, "123", "scroll", "2020-11-03T14:01:05.603Z")
        val click = Event(Event.EventType.ANDROID, "123", "click", "2020-11-03T14:02:05.603Z")
        coEvery { mockRepo.search(scrolling = any(), block = any()) } returns mockk(relaxed = true) {
            every { mappedHits } returns flowOf(start, scroll, click)
        }

        val uut = Sessions(mockRepo)
        val (sessions) = with(uut) { extract(userId) }

        assertEquals(1, sessions.size)
        assertEquals(start, sessions[0].sessionStartEvent)
        assertEquals(click, sessions[0].sessionEndEvent)
        assertEquals(2, sessions[0].sessionEventsInChronologicalOrder.size)
        assertEquals(scroll, sessions[0].sessionEventsInChronologicalOrder[0])
        assertEquals(click, sessions[0].sessionEventsInChronologicalOrder[1])
    }

    @Test
    fun `extraction without start but with end works`() = withAnalyse {

        val userId = UserId("123")
        val scroll = Event(Event.EventType.ANDROID, "123", "scroll", "2020-11-03T14:01:05.603Z")
        val click = Event(Event.EventType.ANDROID, "123", "click", "2020-11-03T14:02:05.603Z")
        val end = Event(Event.EventType.ANDROID, "123", "session_end", "2020-11-03T14:03:05.603Z")
        coEvery { mockRepo.search(scrolling = any(), block = any()) } returns mockk(relaxed = true) {
            every { mappedHits } returns flowOf(scroll, click, end)
        }

        val uut = Sessions(mockRepo)
        val (sessions) = with(uut) { extract(userId) }

        assertEquals(1, sessions.size)
        assertEquals(scroll, sessions[0].sessionStartEvent)
        assertEquals(end, sessions[0].sessionEndEvent)
        assertEquals(2, sessions[0].sessionEventsInChronologicalOrder.size)
        assertEquals(scroll, sessions[0].sessionEventsInChronologicalOrder[0])
        assertEquals(click, sessions[0].sessionEventsInChronologicalOrder[1])
    }

    @Test
    fun `extraction without start and without end also works`() = withAnalyse {

        val userId = UserId("123")
        val scroll = Event(Event.EventType.ANDROID, "123", "scroll", "2020-11-03T14:01:05.603Z")
        val click = Event(Event.EventType.ANDROID, "123", "click", "2020-11-03T14:02:05.603Z")
        coEvery { mockRepo.search(scrolling = any(), block = any()) } returns mockk(relaxed = true) {
            every { mappedHits } returns flowOf(scroll, click)
        }

        val uut = Sessions(mockRepo)
        val (sessions) = with(uut) { extract(userId) }

        assertEquals(1, sessions.size)
        assertEquals(scroll, sessions[0].sessionStartEvent)
        assertEquals(click, sessions[0].sessionEndEvent)
        assertEquals(2, sessions[0].sessionEventsInChronologicalOrder.size)
        assertEquals(scroll, sessions[0].sessionEventsInChronologicalOrder[0])
        assertEquals(click, sessions[0].sessionEventsInChronologicalOrder[1])
    }

    @Test
    fun `extraction with previous session and start and end works`() = withAnalyse {

        val userId = UserId("123")
        val start = Event(Event.EventType.ANDROID, "123", "session_start", "2020-11-03T14:00:05.603Z")
        val scroll = Event(Event.EventType.ANDROID, "123", "scroll", "2020-11-03T14:01:05.603Z")
        val click = Event(Event.EventType.ANDROID, "123", "click", "2020-11-03T14:02:05.603Z")
        val end = Event(Event.EventType.ANDROID, "123", "session_end", "2020-11-03T14:03:05.603Z")
        coEvery { mockRepo.search(scrolling = any(), block = any()) } returns mockk(relaxed = true) {
            every { mappedHits } returns flowOf(start, scroll, click, end, start, scroll, click, end)
        }

        val uut = Sessions(mockRepo)
        val (sessions) = with(uut) { extract(userId) }

        assertEquals(2, sessions.size)
        repeat(2) {
            assertEquals(start, sessions[it].sessionStartEvent)
            assertEquals(end, sessions[it].sessionEndEvent)
            assertEquals(2, sessions[it].sessionEventsInChronologicalOrder.size)
            assertEquals(scroll, sessions[it].sessionEventsInChronologicalOrder[0])
            assertEquals(click, sessions[it].sessionEventsInChronologicalOrder[1])
        }
    }

    @Test
    fun `extraction with previous session and with start but without end works`() = withAnalyse {

        val userId = UserId("123")
        val start = Event(Event.EventType.ANDROID, "123", "session_start", "2020-11-03T14:00:05.603Z")
        val scroll = Event(Event.EventType.ANDROID, "123", "scroll", "2020-11-03T14:01:05.603Z")
        val click = Event(Event.EventType.ANDROID, "123", "click", "2020-11-03T14:02:05.603Z")
        val end = Event(Event.EventType.ANDROID, "123", "session_end", "2020-11-03T14:03:05.603Z")
        coEvery { mockRepo.search(scrolling = any(), block = any()) } returns mockk(relaxed = true) {
            every { mappedHits } returns flowOf(start, scroll, click, start, scroll, click, end)
        }

        val uut = Sessions(mockRepo)
        val (sessions) = with(uut) { extract(userId) }

        assertEquals(2, sessions.size)
        repeat(2) {
            assertEquals(start, sessions[it].sessionStartEvent)
            if (it == 0)
                assertEquals(click, sessions[it].sessionEndEvent)
            else
                assertEquals(end, sessions[it].sessionEndEvent)
            assertEquals(2, sessions[it].sessionEventsInChronologicalOrder.size)
            assertEquals(scroll, sessions[it].sessionEventsInChronologicalOrder[0])
            assertEquals(click, sessions[it].sessionEventsInChronologicalOrder[1])
        }
    }

    @Test
    fun `extraction with previous session and without start but with end works`() = withAnalyse {

        val userId = UserId("123")
        val start = Event(Event.EventType.ANDROID, "123", "session_start", "2020-11-03T14:00:05.603Z")
        val scroll = Event(Event.EventType.ANDROID, "123", "scroll", "2020-11-03T14:01:05.603Z")
        val click = Event(Event.EventType.ANDROID, "123", "click", "2020-11-03T14:02:05.603Z")
        val end = Event(Event.EventType.ANDROID, "123", "session_end", "2020-11-03T14:03:05.603Z")
        coEvery { mockRepo.search(scrolling = any(), block = any()) } returns mockk(relaxed = true) {
            every { mappedHits } returns flowOf(start, scroll, click, end, scroll, click, end)
        }

        val uut = Sessions(mockRepo)
        val (sessions) = with(uut) { extract(userId) }

        assertEquals(2, sessions.size)
        repeat(2) {
            if (it == 0)
                assertEquals(start, sessions[it].sessionStartEvent)
            else
                assertEquals(scroll, sessions[it].sessionStartEvent)
            assertEquals(end, sessions[it].sessionEndEvent)
            assertEquals(2, sessions[it].sessionEventsInChronologicalOrder.size)
            assertEquals(scroll, sessions[it].sessionEventsInChronologicalOrder[0])
            assertEquals(click, sessions[it].sessionEventsInChronologicalOrder[1])
        }
    }

    @Test
    fun `extraction with two session and dangling start and end works`() = withAnalyse {

        val userId = UserId("123")
        val start = Event(Event.EventType.ANDROID, "123", "session_start", "2020-11-03T14:00:05.603Z")
        val scroll = Event(Event.EventType.ANDROID, "123", "scroll", "2020-11-03T14:01:05.603Z")
        val click = Event(Event.EventType.ANDROID, "123", "click", "2020-11-03T14:02:05.603Z")
        val end = Event(Event.EventType.ANDROID, "123", "session_end", "2020-11-03T14:03:05.603Z")
        coEvery { mockRepo.search(scrolling = any(), block = any()) } returns mockk(relaxed = true) {
            every { mappedHits } returns flowOf(
                start,
                start,
                scroll,
                click,
                end,
                end,
                start,
                start,
                scroll,
                click,
                click,
                end,
                end
            )
        }

        val uut = Sessions(mockRepo)
        val (sessions) = with(uut) { extract(userId) }

        assertEquals(2, sessions.size)
        repeat(2) {
            assertEquals(start, sessions[it].sessionStartEvent)
            assertEquals(end, sessions[it].sessionEndEvent)
            assertEquals(it + 2, sessions[it].sessionEventsInChronologicalOrder.size)
            assertEquals(scroll, sessions[it].sessionEventsInChronologicalOrder[0])
            assertEquals(click, sessions[it].sessionEventsInChronologicalOrder[1])
            if (it == 1) {
                assertEquals(click, sessions[it].sessionEventsInChronologicalOrder[2])
            }
        }
    }
}