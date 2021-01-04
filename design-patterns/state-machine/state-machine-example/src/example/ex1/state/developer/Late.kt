package example.ex1.state.developer

import example.ex1.context.Context
import example.ex1.state.State
import kotlin.random.Random

/**
 * 지각이면 혼나고 일해야지
 */
class Late: State {

    companion object {
        private val current = DeveloperState.LATE
    }

    override fun operate(context: Context) {
        println("CURRENT STATE: $current")
        println("죄송합니다,, (지각이다..)")
        println("일단 혼남..")

        context.state = WorkingMorning()
    }
}