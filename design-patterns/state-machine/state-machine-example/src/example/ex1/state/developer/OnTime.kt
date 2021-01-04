package example.ex1.state.developer

import example.ex1.context.Context
import example.ex1.state.State
import kotlin.random.Random

/**
 * 왔으면 일해야지
 */
class OnTime: State {

    companion object {
        private val current = DeveloperState.ON_TIME
    }

    override fun operate(context: Context) {
        println("CURRENT STATE: $current")
        println("안녕하세욤~")

        context.state = WorkingMorning()
    }
}