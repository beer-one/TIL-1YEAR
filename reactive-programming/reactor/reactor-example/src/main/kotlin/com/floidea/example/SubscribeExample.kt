package com.floidea.example

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.lang.RuntimeException

fun main() {
    val intFlux = Flux.range(10, 50).log()
        .handle<Int> { num, sink ->
            if (num == 12) sink.error(NumberException())
            else sink.next(num)
        }

    intFlux.subscribe(
        { num ->
            println("Item: $num")
        },
        { error ->
            println("Error message: ${error.message}")
        },
        {
            println("subscription is completed")
        },
        { sub ->
            sub.request(5)
        }
    )

//    intFlux.subscribe { num ->
//        println("item: $num")
//    }

//    intFlux.subscribe()
}

class NumberException: RuntimeException("숫자가 왜 12지?")