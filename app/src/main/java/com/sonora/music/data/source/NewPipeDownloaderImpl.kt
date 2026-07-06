package com.sonora.music.data.source

import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response

/**
 * OkHttp-backed [Downloader] required by NewPipeExtractor. NewPipe uses this for every InnerTube
 * call, including the client-spoof requests that fetch YouTube stream URLs.
 */
class NewPipeDownloaderImpl(private val client: OkHttpClient) : Downloader() {

    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val builder = okhttp3.Request.Builder()
            .method(httpMethod, dataToSend?.toRequestBody())
            .url(url)
            .addHeader("User-Agent", USER_AGENT)

        headers.forEach { (name, values) ->
            builder.removeHeader(name)
            values.forEach { builder.addHeader(name, it) }
        }

        client.newCall(builder.build()).execute().use { resp ->
            val body = resp.body?.string()
            return Response(
                resp.code,
                resp.message,
                resp.headers.toMultimap(),
                body,
                resp.request.url.toString(),
            )
        }
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0 Safari/537.36"
    }
}
