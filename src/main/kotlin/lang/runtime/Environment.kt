package lang.runtime

data class Environment(
    private val values: MutableMap<String, Any?> = mutableMapOf(),
    val enclosing: Environment? = null
) {
    fun define(
        name: String,
        value: Any?,
    ) {
        values[name] = value
    }

    fun get(name: String): Any? =
        if (!values.containsKey(name)) {
            enclosing?.get(name) ?: throw RuntimeError("undeclared variable $name")
        } else {
            values[name]
        }

    fun assign(name: String, value: Any?) {
        if (values.contains(name)) {
            values[name] = value
        } else {
            enclosing?.assign(name, value) ?: throw RuntimeError("undeclared variable $name")
        }
    }

    fun getAt(depth: Int, name: String) = ancestor(depth).values[name]

    private fun ancestor(depth: Int): Environment {
        var env = this
        for (i in 0..< depth) {
            env = env.enclosing!!
        }
        return env
    }

    fun assignAt(depth: Int, name: String, value: Any?) {
       ancestor(depth).values[name] = value
    }

}
