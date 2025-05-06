package app.pago.okhttp_applicationstream

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import java.io.IOException
import java.util.concurrent.TimeUnit

data class ApplicationStreamServiceConfig(
    val baseUrl: String,
    val client: OkHttpClient,
    val readBufferSize: Int = DEFAULT_BUFFER_SIZE
){
    companion object{
        const val DEFAULT_BUFFER_SIZE = 1024
    }
}
class ApplicationStreamService(
    private val config: ApplicationStreamServiceConfig
) {
//    init {
//        val builder = OkHttpClient.Builder()
//        builder.readTimeout(10, TimeUnit.HOURS)
//        builder.writeTimeout(10, TimeUnit.HOURS)
//        builder.callTimeout(10, TimeUnit.HOURS)
//        builder.connectTimeout(10, TimeUnit.HOURS)
//
//        interceptors.forEach { interceptor ->
//            builder.addInterceptor(interceptor)
//        }
//
//        client = builder.build()
//    }

    // Parse the data in the buffer and return the list of extracted objects and what is left in the buffer
    // if there was an incomplete object in there
    private fun <T> parseDataInBuffer(
        stringBuffer: String,
        dataType: TypeToken<T>
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

    fun <T> get(path: String, responseType: TypeToken<T>): Flow<T> {
        val request = Request.Builder()
            .url("${config.baseUrl}/$path")
            .build()

        return performRequest(request, responseType = responseType)
    }

    fun <B : Any, R> post(path: String, body: B, responseType: TypeToken<R>): Flow<R> {
        val requestBody = Gson()
            .toJson(body)
            .toRequestBody(
                contentType = "application/json".toMediaType()
            )

        val request = Request.Builder()
            .url("${config.baseUrl}/$path")
            .method("POST", body = requestBody)
            .build()

        return performRequest(request, responseType = responseType)
    }

    private fun <T> performRequest(request: Request, responseType: TypeToken<T>): Flow<T> {
        return channelFlow {
            withContext(Dispatchers.IO) {
                val response = config.client.newCall(request).execute()
                if (!response.isSuccessful) {
                    println("Unexpected code $response for ${request.url}")
                }

                val source = response.body?.source()

                // We'll read the data from the buffer in chunks
                // If a JSON object is larger than a single chunk we'll keep appending chunks until we can parse a valid JSON object
                var stringBuffer = ""

                try {
                    while (!source!!.exhausted()) {
                        val sync = Buffer()
                        source.read(sync, config.readBufferSize.toLong())
                        stringBuffer += sync.readUtf8()

                        val (remainingBuffer, objects) = parseDataInBuffer(
                            stringBuffer,
                            responseType
                        )
                        stringBuffer = remainingBuffer
                        objects.forEach { send(it) }

                    }
                } catch (e: IOException) {
                    println("Stream ended or errored: ${e.message} for ${request.url}")
                }
            }
        }
    }
}