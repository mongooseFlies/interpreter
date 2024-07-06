package lang.runtime

import lang.model.Fn

class Function(
    private val declaration: Fn,
    private val closure: Environment
) : Callable {
    override fun call(
        interpreter: Interpreter,
        arguments: List<Any?>,
    ) :Any? {
        val environment = Environment(enclosing = closure)
        for (i in arguments.indices) {
            environment.define(declaration.params[i].text, arguments[i])
        }
        try {
            interpreter.executeBlock(declaration.body, environment)
        } catch (returnValue: Return) {
            return returnValue.value
        }
        return null
    }
}
