package app.pago.okhttp_applicationstream

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class ApplicationStreamService(private val baseUrl: String) {
    private val client = OkHttpClient()

    // Parse the data in the buffer and return the list of extracted objects and what is left in the buffer
    // if there was an incomplete object in there
    private fun <T> parseDataInBuffer(
        stringBuffer: String,
        dataType: Class<T>
    ): Pair<String, List<T>> {
        val objects: MutableList<T> = mutableListOf()

        try {
            // A chunk might have multiple JSON objects in it,
            // if they are small enough and the server responds fast enough
            // Or the buffer can hold multiple chunks
            stringBuffer.lines().forEach() { line ->
                if (line.isNotBlank()) {
                    objects.add(Gson().fromJson(line, dataType))
                }
            }

            // We managed to parse all objects from this buffer
            // We return the objects and an empty remaining buffer
            return "" to objects
        } catch (e: JsonSyntaxException) {
            println("Chunk incomplete")

            // If the string buffer held multiple objects, only the last one can be incomplete
            // So in order to not emit the same objects twice, we'll remove the complete objects from the string buffer
            val remainingBuffer = stringBuffer.lines().last()
            return remainingBuffer to objects
        }
    }

    fun <T> get(path: String, responseType: Class<T>): Flow<T> {
        val request = Request.Builder()
            .url("$baseUrl/$path")
            .build()

        return channelFlow {
            withContext(Dispatchers.IO) {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    println("Unexpected code $response for $baseUrl/$path")
                }

                val source = response.body?.source()

                // We'll read the data from the buffer in chunks
                // If a JSON object is larger than a single chunk we'll keep appending chunks until we can parse a valid JSON object
                var stringBuffer = ""

                try {
                    while (!source!!.exhausted()) {
                        val chunk = source.readUtf8()
                        stringBuffer += chunk

                        val (remainingBuffer, objects) = parseDataInBuffer(
                            stringBuffer,
                            responseType
                        )
                        stringBuffer = remainingBuffer
                        objects.forEach { send(it) }

                    }
                } catch (e: IOException) {
                   println("Stream ended or errored: ${e.message} for $baseUrl/$path")
                }
            }
        }
    }
}