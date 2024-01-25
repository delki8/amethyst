/**
 * Copyright (c) 2023 Vitor Pamplona
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
 * Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.vitorpamplona.amethyst.service

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class ResponseSizeFetcher {
    fun getResponseSize(url: String): Long? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(url)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connect()

            val contentLength = connection.getHeaderField("Content-Length")
            contentLength?.toLong() ?: downloadAndGetSize(url)
        } catch (e: IOException) {
            // Handle exceptions (e.g., MalformedURLException, IOException, etc.)
            e.printStackTrace()
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun downloadAndGetSize(url: URL): Long? {
        var connection: HttpURLConnection? = null
        return try {
            connection = url.openConnection() as HttpURLConnection
            connection.connect()

            // Download the resource (you can choose to read the content if needed)
            val contentLength = connection.contentLength.toLong()

            contentLength
        } catch (e: IOException) {
            // Handle exceptions during the download if needed
            e.printStackTrace()
            null
        } finally {
            connection?.disconnect()
        }
    }
}
