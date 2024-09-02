package lang.runtime

data class Class(
    val name: String,
    val methods: MutableMap<String, Function>,
    val superClass: Class? = null
) : Callable {

    override fun call(interpreter: Interpreter, arguments: List<Any?>) : Instance {
        val instance = Instance(this)
        getMethod("init")?.bind(instance)?.call(interpreter, arguments)
        return instance
    }

    fun getMethod(name: String): Function? = methods[name] ?: superClass?.getMethod(name)

    override fun toString() = "<class $name>"
}