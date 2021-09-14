package com.floidea.example

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import reactor.core.publisher.Sinks


fun main() {
    val multicastSink = Sinks.many().multicast().directBestEffort<Int>()

    (1..100).forEach { multicastSink.emitNext(it, Sinks.EmitFailureHandler.FAIL_FAST) }

    val flux = multicastSink.asFlux()

    flux.subscribe { println("FIRST: $it") }
    flux.subscribe { println("SECOND: $it") }

    runBlocking { delay(10000L) }
}