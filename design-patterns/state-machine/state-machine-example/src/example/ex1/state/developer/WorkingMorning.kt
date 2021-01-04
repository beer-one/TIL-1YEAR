package example.ex1.state.developer

import example.ex1.context.Context
import example.ex1.state.State
import kotlin.random.Random

/**
 * 일 -> 점심
 */
class WorkingMorning: State {

    companion object {
        private val current = DeveloperState.WORKING_MORNING
    }

    override fun operate(context: Context) {
        println("CURRENT STATE: $current")
        println("하 피곤해 점심 뭐지,,?")

        context.state = Lunch()
    }
}