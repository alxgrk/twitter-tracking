package de.alxgrk.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.inbot.eskotlinwrapper.AsyncIndexRepository
import kotlinx.coroutines.flow.first
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.BucketOrder
import org.elasticsearch.search.aggregations.bucket.terms.IncludeExclude
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms
import org.elasticsearch.search.builder.SearchSourceBuilder

class UserIds(private val repo: AsyncIndexRepository<Event>) {

    suspend fun Analyse.get() = repo
        .search {
            source(
                SearchSourceBuilder.searchSource()
                    .aggregation(
                        AggregationBuilders.terms("users")
                            .field("user_id")
                            .order(BucketOrder.count(false))
                            .includeExclude(IncludeExclude(null, "unknown_user"))
                    )
                    .size(0)
            )
        }
        .rawResponses().first().aggregations
        .first { it.name == "users" }
        .let { it as ParsedStringTerms }
        .buckets
        .map { UserId(it.keyAsString) }
}

class UserId @JsonCreator constructor(@JsonProperty("id") val id: String)