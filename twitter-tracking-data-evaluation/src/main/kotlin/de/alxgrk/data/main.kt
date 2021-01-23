package de.alxgrk.data

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.path
import de.alxgrk.data.pca.PCA
import de.alxgrk.data.plots.findChartNames
import de.alxgrk.data.plots.findCharts
import io.inbot.eskotlinwrapper.AsyncIndexRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.client.asyncIndexRepository
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.signer.Aws4Signer
import software.amazon.awssdk.regions.Region
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    try {
        Analyse().main(args)
        exitProcess(0)
    } catch (e: Exception) {
        exitProcess(1)
    }
}

class Analyse : CliktCommand() {

    private val host by option(help = "elasticsearch host (don't prepend http:// or similar!)").required()

    internal val debug by option(help = "enable debug log output").flag()

    internal val trace by option(help = "enable trace log output").flag()

    internal val refresh by option(help = "clear cache and get events from elasticsearch").flag()

    internal val export by option(help = "export plot as svg using Orca (requires Docker on your machine)").flag()

    private val task by argument(help = "all, manual, pca, ${findChartNames().joinToString()}")
        .choice("all", "manual", "pca", *findChartNames().toTypedArray())

    override fun run() = runBlocking {
        try {
            val repo = initialize(host)

            val sessionsPerUserId = (
                if (refresh || !Cache.exists())
                    coroutineScope {

                        val userIds = with(UserIds(repo)) { get() }
                        debug("UserIds: ${userIds.joinToString()}")

                        userIds
                            .map { userId ->
                                async(Dispatchers.IO) {
                                    val (sessions) = with(Sessions(repo)) { extract(userId) }
                                    debug("finished session extraction for user $userId")
                                    userId to sessions
                                }
                            }
                            .awaitAll()
                            .also { Cache.store(it) }
                    }
                else
                    Cache.retrieve()
                )
                .toMap()

            if (debug) {
                sessionsPerUserId.forEach { (userId, sessions) ->
                    debug("User: $userId")
                    sessions.forEachIndexed { idx, it ->
                        debug("$idx. Session:\t${it.sessionStartEvent.timestamp} - ${it.sessionEndEvent.timestamp}: ${it.sessionEventsInChronologicalOrder.size} events")
                    }
                }
            }

            if (task == "all") {
                coroutineScope {
                    findChartNames()
                        .map { async { selectChart(it, sessionsPerUserId) } }
                        .awaitAll()
                }
            } else if (task == "manual") {
                sessionsPerUserId.forEach { (u, s) ->
                    println("First session of user ${u.id} was on ${s.first().sessionStartEvent.zonedTimestamp()}")
                }
            } else if (task == "pca") {
                with(PCA(sessionsPerUserId)) { execute() }
            } else {
                selectChart(task, sessionsPerUserId)
            }
        } catch (e: Exception) {
            echo("Quitting with exception: $e", err = true)
            debug(e.stackTraceToString())
        }
        Unit
    }

    private fun selectChart(task: String, sessionsPerUserId: Map<UserId, List<Session>>) {
        val chart = findCharts()
            .getOrElse(task) { throw IllegalArgumentException("Unknown task '$task'...") }
        with(chart) { create(sessionsPerUserId) }
    }

    operator fun Boolean.invoke(logMessage: String) {
        if (this)
            echo(logMessage)
    }

    operator fun Boolean.invoke(logMessage: () -> String) {
        if (this)
            echo(logMessage())
    }

    fun info(message: String) {
        echo(message)
    }
}

fun initialize(host: String): AsyncIndexRepository<Event> {
    println("Trying to connect to Elasticsearch at $host")

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
        restClientBuilder
            .setRequestConfigCallback {
                it.setSocketTimeout(60000)
            }
            .setHttpClientConfigCallback {
                val awsRequestSigningInterceptor = AwsRequestSigningInterceptor(
                    Region.EU_CENTRAL_1,
                    "es",
                    Aws4Signer.create(),
                    DefaultCredentialsProvider.create()
                )
                it.addInterceptorLast(awsRequestSigningInterceptor)
            }
    }

    val esClient = RestHighLevelClient(restClientBuilder)
    return esClient.asyncIndexRepository(
        index = "events"
    )
}
