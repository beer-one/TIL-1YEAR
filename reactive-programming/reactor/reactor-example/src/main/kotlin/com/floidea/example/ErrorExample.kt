package com.floidea.example

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.util.Loggers
import reactor.util.retry.Retry
import java.time.Duration
import kotlin.random.Random

fun main() {
    val logger = LoggerFactory.getLogger("Logger")

    val flux = Flux.interval(Duration.ofMillis(100L))
        .handle<Long> { num, sink ->
            if(Random.nextInt(10) == 0) sink.error(TemporaryException())
            else sink.next(num)
        }.doOnError { logger.error("Temporary error occurred!!") }
        .retryWhen(Retry.from { it.take(3) })
        .doFinally { logger.info("Finished") }

    flux.subscribe { logger.info(it.toString()) }

    runBlocking { delay(10000L) }
}

class NumberIs4Exception(): RuntimeException()
class NumberIs5Exception(): RuntimeException()

class WrongNumberException: RuntimeException()

class TemporaryException: RuntimeException()