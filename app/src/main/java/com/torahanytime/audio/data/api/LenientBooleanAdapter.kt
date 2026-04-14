package com.torahanytime.audio.data.api

import com.squareup.moshi.*

/**
 * Moshi adapter that handles booleans that may come as true/false, 0/1, or null from the API.
 */
class LenientBooleanAdapter {
    @FromJson
    fun fromJson(reader: JsonReader): Boolean? {
        return when (reader.peek()) {
            JsonReader.Token.BOOLEAN -> reader.nextBoolean()
            JsonReader.Token.NUMBER -> reader.nextInt() != 0
            JsonReader.Token.NULL -> {
                reader.nextNull<Any>()
                null
            }
            JsonReader.Token.STRING -> {
                val s = reader.nextString()
                s == "true" || s == "1"
            }
            else -> {
                reader.skipValue()
                null
            }
        }
    }

    @ToJson
    fun toJson(value: Boolean?): Boolean? = value
}
