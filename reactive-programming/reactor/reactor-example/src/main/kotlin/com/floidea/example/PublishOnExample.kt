package com.floidea.example

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.util.Loggers

fun main() {
    val logger = LoggerFactory.getLogger("Thread")
    val schedulers = Schedulers.newParallel("parallel", 4)

    val flux = Flux.range(1, 2)
        .subscribeOn(Schedulers.boundedElastic())
        .map {
            logger.info("${it + 10}")
            it + 10
        }
        .publishOn(schedulers)
        .map {
            logger.info("value $it")
            "value $it"
        }

    Thread {
        flux.subscribe { logger.info(it) }
    }.run()

    Thread {
        flux.subscribe { logger.info(it) }
    }.run()

    Thread {
        flux.subscribe { logger.info(it) }
    }.run()

    runBlocking { delay(10000L) }
}