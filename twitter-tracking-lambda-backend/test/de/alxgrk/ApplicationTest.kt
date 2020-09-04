package de.alxgrk

import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplicationTest {
    @Test
    fun testRoot() {
        withTestApplication({ module(testing = true) }) {
            handleRequest(HttpMethod.Post, "/events") {
                setBody("""
                    {
                        "action": "click",
                        "eventType": "BROWSER",
                        "userId": "123",
                        "action": "scroll",
                        "timestamp": "2020-08-27T17:00:00Z",
                        "target": "visitAuthorsProfile",
                        "selector": "div > div > div",
                        "scrollPosition": 1234,
                        "estimatedTweetsScrolled": 13
                    }
                """.replace("\\s".toRegex(), ""))
                addHeader("Content-Type", "application/json")
            }
                .apply {
                    assertEquals(HttpStatusCode.Created, response.status())
                    assertTrue(response.content!!.isBlank())
                }
        }
    }
}
