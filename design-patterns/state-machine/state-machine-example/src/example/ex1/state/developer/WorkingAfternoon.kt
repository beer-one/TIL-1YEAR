package example.ex1.state.developer

import example.ex1.context.Context
import example.ex1.state.State
import kotlin.random.Random

/**
 * 반은 회사에서 저녁 (야근 가능성)
 * 반은 퇴근
 */
class WorkingAfternoon: State {

    companion object {
        private val current = DeveloperState.WORKING_AFTERNOON
    }

    override fun operate(context: Context) {
        println("CURRENT STATE: $current")
        println("설마 오늘 야근..?")

        context.state = when (Random.nextInt(2)) {
            0 -> OffWork()
            else -> Dinner()
        }
    }
}