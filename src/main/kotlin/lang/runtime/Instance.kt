package lang.runtime

import lang.model.Token

data class Instance(
    val klass: Class,
    val fields: MutableMap<String, Any?> = mutableMapOf()
) {

    fun get(name: Token): Any? {
        if (fields.containsKey(name.text)) return fields[name.text]
        val method = klass.getMethod(name.text)
        method?.let {
            return method.bind(this)
        } ?: throw RuntimeError("Undefined property", name)
    }

    fun set(name: Token, value: Any?) {
        fields[name.text] = value
    }
}