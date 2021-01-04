package example.ex1.state.developer

import example.ex1.context.Context
import example.ex1.state.State
import kotlin.random.Random

/**
 * 점심 -> 다시 일
 */
class Lunch: State {

    companion object {
        private val current = DeveloperState.LUNCH
    }

    override fun operate(context: Context) {
        println("CURRENT STATE: $current")
        println("점심 꿀맛!")

        context.state = WorkingAfternoon()
    }
}