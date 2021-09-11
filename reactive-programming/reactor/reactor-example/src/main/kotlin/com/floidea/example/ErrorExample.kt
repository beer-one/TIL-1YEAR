package com.floidea.example

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.util.Loggers
import kotlin.random.Random

fun main() {
    val logger = LoggerFactory.getLogger("Logger")

    val fallback = Mono.fromCallable {
        Random.nextInt(-1, -10)
    }

    val flux = Flux.fromIterable(1..10)
        .handle<Int> { num, sink ->
            if (num == 4) sink.error(NumberIs4Exception())
            else sink.next(num)
        }.onErrorResume { fallback }

    flux.subscribe { logger.info(it.toString()) }
}

class NumberIs4Exception(): RuntimeException()