package de.alxgrk.data

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import de.undercouch.bson4jackson.BsonFactory
import java.io.File

object Cache {

    val mapper = ObjectMapper(BsonFactory()).registerModule(KotlinModule())!!
    val cacheFile = File(".sessions.bson")

    fun exists() = cacheFile.exists()

    fun <T : Any> store(any: T) {
        cacheFile.writeBytes(mapper.writeValueAsBytes(any))
    }

    inline fun <reified T : Any> retrieve(): T = mapper.readValue(cacheFile.readBytes())
}