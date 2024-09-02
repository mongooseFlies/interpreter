package lang.runtime

import lang.model.Fn

class Function(
    private val declaration: Fn,
    private val closure: Environment,
    private val isInitializer: Boolean = false
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
            return if (isInitializer)
                     closure.getAt(0, "self")
                   else returnValue.value
        }
        return null
    }

    fun bind(instance: Instance) : Function {
        val env = Environment(enclosing = closure)
        env.define("self", instance)
        return Function(declaration, env, isInitializer)
    }

    override fun toString() = "<fn ${declaration.name.text}>"
}
