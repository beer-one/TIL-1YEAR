package com.floidea.example

import kotlinx.coroutines.*
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.util.function.Tuples
import java.lang.RuntimeException
import java.util.function.LongConsumer
import kotlin.coroutines.CoroutineContext

fun main() {
    val intFlux: Flux<Int> = Flux.generate(
        {
            1
        },
        { num, sink ->
            sink.next(num+1)
            runBlocking {
                delay(1000L)
            }
            num+1
        }
    )


    intFlux.doOnError { println("Error") }
        .doOnCancel { println("Cancel") }
        .publishOn(Schedulers.boundedElastic())
        .subscribeOn(Schedulers.boundedElastic())


    var disposable: Disposable? = null

    GlobalScope.launch {
        launch {
            disposable = intFlux.subscribe {
                println("num: $it")
            }
        }

        delay(1000L)

        launch {
            intFlux.subscribe {
                println("num2: $it")
            }
        }
    }


    runBlocking {
        delay(5000L)

        disposable?.dispose()
        println(disposable?.isDisposed)
        delay(5000L)
    }
    
}
