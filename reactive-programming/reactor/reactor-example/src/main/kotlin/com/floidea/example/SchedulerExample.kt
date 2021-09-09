package com.floidea.example

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.Duration

fun main() {
val logger = LoggerFactory.getLogger("Logger")
logger.info("Start")
//
//val flux = Flux.fromIterable(1..5)
//    .handle<Int> { num, sink ->
//        runBlocking { delay(100L) }
//        sink.next(num)
//    }
//
//
//flux.subscribeOn(Schedulers.parallel())
//    .subscribe { logger.info("1: $it") }
//flux.subscribeOn(Schedulers.parallel())
//    .subscribe { logger.info("2: $it") }
//
//runBlocking { delay(10000) }
//
val intervalFlux = Flux.interval(Duration.ofMillis(300), Schedulers.newSingle("Single"))

intervalFlux.subscribe { logger.info("Subscribed, $it") }
}