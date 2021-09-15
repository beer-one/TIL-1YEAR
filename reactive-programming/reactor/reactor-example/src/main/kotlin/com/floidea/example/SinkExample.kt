package com.floidea.example

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import reactor.core.publisher.Sinks


fun main() {
    val multicastSink = Sinks.many().multicast().onBackpressureBuffer<Int>()

    object : Thread() {
        override fun run() {
            var num = 1
            while (true) {
                val x = multicastSink.tryEmitNext(num)
                println(x)
                sleep(1000L)
            }
        }
    }.run()



    val flux = multicastSink.asFlux()

    flux.subscribe { println("FIRST: $it") }
    flux.subscribe { println("SECOND: $it") }

    runBlocking { delay(10000L) }
}