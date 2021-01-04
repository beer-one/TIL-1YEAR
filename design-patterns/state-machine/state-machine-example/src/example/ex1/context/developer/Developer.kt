package example.ex1.context.developer

import example.ex1.context.Context
import example.ex1.state.State
import example.ex1.state.developer.InHome

class Developer: Context {

    override var state: State = InHome()
        set(value) {
            field = value
            this.operate()
        }

    override fun operate() {
        state.operate(this)
    }
}