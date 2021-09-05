package com.floidea.example

import com.floidea.example.subscriber.TestSubscriber
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.reactivestreams.Subscription
import reactor.core.publisher.BaseSubscriber
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

fun main() {
    var i = 1
    val upstream = Flux.generate<Int> {
        runBlocking { delay(500L) }
        it.next(i++)
    }.log()

    upstream.limitRate(10)
        .subscribe(TestSubscriber())
}
