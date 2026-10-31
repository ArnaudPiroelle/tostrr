package app.tostrr.data.testing

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import java.io.IOException

/**
 * An [OkHttpClient] that never leaves the JVM: each request is answered from the responses set per URL path with
 * [on], 404 otherwise, and kept in [calls].
 */
class FakeHttp {
    data class Call(val request: Request, val body: String?)

    private class Answer(val code: Int, val body: String?, val failure: IOException?)

    private val answers = mutableMapOf<String, Answer>()
    val calls = mutableListOf<Call>()

    /** Answers requests to [path] (`/api/1/users/orders`) with [code] and [body]. */
    fun on(path: String, body: String, code: Int = 200) {
        answers[path] = Answer(code, body, null)
    }

    /** Requests to [path] fail as if the network were down. */
    fun failOn(path: String) {
        answers[path] = Answer(0, null, IOException("offline"))
    }

    fun callsTo(path: String) = calls.filter { it.request.url.encodedPath == path }

    val client: OkHttpClient = OkHttpClient.Builder().addInterceptor { chain ->
        val request = chain.request()
        calls += Call(request, request.body?.let { body -> Buffer().also(body::writeTo).readUtf8() })
        val answer = answers[request.url.encodedPath] ?: Answer(404, "", null)
        answer.failure?.let { throw it }
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(answer.code)
            .message("")
            .body(answer.body.orEmpty().toResponseBody("application/json".toMediaType()))
            .build()
    }.build()
}
