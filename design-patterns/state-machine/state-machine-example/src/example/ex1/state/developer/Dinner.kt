package example.ex1.state.developer

import example.ex1.context.Context
import example.ex1.state.State
import kotlin.random.Random

/**
 * 밥 -> 퇴근 or 야근 (저녁을 먹었다면 야근 가능성 상당히 높음)
 */
class Dinner: State {

    companion object {
        private val current = DeveloperState.DINNER
    }

    override fun operate(context: Context) {
        println("CURRENT STATE: $current")
        println("저녁먹고 퇴근 아니면 야근!")

        context.state = when(Random.nextInt(5)) {
            0 -> OffWork()
            else -> NightShift()
        }
    }
}