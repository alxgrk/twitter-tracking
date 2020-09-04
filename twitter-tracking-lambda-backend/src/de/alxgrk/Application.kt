package de.alxgrk

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import io.inbot.eskotlinwrapper.IndexRepository
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.apache.http.HttpHost
import org.elasticsearch.client.*
import org.elasticsearch.client.indices.GetIndexRequest
import org.slf4j.event.Level
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.signer.Aws4Signer
import software.amazon.awssdk.regions.Region


fun main(args: Array<String>): Unit = io.ktor.server.cio.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    val eventRepo = if (!testing) initialize() else null

    install(Locations) {
    }

    install(AutoHeadResponse)

    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }

    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Post)
        header(HttpHeaders.ContentType)
        anyHost()
    }

    install(DefaultHeaders) {
        header("X-Engine", "Ktor")
    }

    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        }
    }

    routing {

        post("events") {
            val event = call.receive<Event>()
            log.info(event.toString())
            eventRepo?.index(event.userId + event.timestamp, event, create = false)
            call.respond(HttpStatusCode.Created, "")
        }
        options("events") {
            call.respond(HttpStatusCode.OK, "")
        }

    }
}

fun Application.initialize(): IndexRepository<Event> {
    val host = System.getenv("ELASTICSEARCH_URL") ?: "localhost"
    log.info("Trying to connect to Elasticsearch at $host")

    val prod = host != "localhost"
    val restClientBuilder =
        RestClient.builder(
            HttpHost(
                host,
                if (prod) 443 else 9200,
                if (prod) "https" else "http"
            )
        )

    if (prod) {
        restClientBuilder.setHttpClientConfigCallback {
            it.apply {
                val awsRequestSigningInterceptor = AwsRequestSigningInterceptor(
                    Region.EU_CENTRAL_1,
                    "es",
                    Aws4Signer.create(),
                    DefaultCredentialsProvider.create()
                )
                addInterceptorLast(awsRequestSigningInterceptor)
            }
        }
    }

    val esClient = RestHighLevelClient(restClientBuilder)
    val repo = esClient.indexRepository<Event>(
        index = "events"
    )
    if (!esClient.indices().exists(GetIndexRequest(repo.indexName), RequestOptions.DEFAULT))
        repo.createIndex {
            configure {
                settings {
                    replicas = 0
                    shards = 1
                }
                mappings {
                    keyword("event_type")
                    keyword("user_id")
                    keyword("action")
                    field("timestamp", "date")
                    keyword("target")
                    text("selector") {
                        fields {
                            keyword("original")
                        }
                    }
                    number<Int>("scroll_position")
                    number<Int>("estimated_tweets_scrolled")
                }
            }
        }
    return repo
}

@JsonInclude(Include.NON_NULL)
data class Event(
    val eventType: EventType,
    val userId: String,
    val action: String,
    val timestamp: String,
    val target: String?,
    val selector: String?,
    val scrollPosition: Int?,
    val estimatedTweetsScrolled: Int?
) {
    enum class EventType {
        BROWSER,
        ANDROID
    }
}

