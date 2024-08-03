package lang.runtime

data class Class(
    val name: String,
    val methods: MutableMap<String, Function>
) : Callable {

    override fun call(interpreter: Interpreter, arguments: List<Any?>) =
        Instance(this)

    fun getMethod(name: String) = methods[name]

    override fun toString() = "<class $name>"
}