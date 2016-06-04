package com.lykke.matching.engine.logging

import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.log4j.Logger
import java.util.concurrent.BlockingQueue

class HttpLogger: Thread {
    companion object {
        val LOGGER = Logger.getLogger(HttpLogger::class.java.name)
    }

    val path: String
    val queue: BlockingQueue<LoggableObject>

    constructor(path: String, queue: BlockingQueue<LoggableObject>) : super() {
        this.path = path
        this.queue = queue
    }

    override fun run() {
        while (true) {
            val obj = queue.take()
            try {
                val httpClient = HttpClientBuilder.create().build()
                val request = HttpPost(path)
                val params = StringEntity(obj.getJson())
                request.addHeader("content-type", "application/json")
                request.setEntity(params)
                httpClient.execute(request)
            } catch (e : Exception) {
                LOGGER.error("Unable to write log to http: ${e.message}", e)
            }
        }
    }
}