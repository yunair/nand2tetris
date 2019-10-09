enum class SymbolKind(val value: String) {
    STATIC("static"), FIELD("field"), VAR("local"), ARG("argument"), NONE("none");

    companion object{
        fun get(value: String): SymbolKind {
            return values().find { it.value == value } ?: NONE
        }
    }
}

class SymbolTable {
    private val table = mutableMapOf<SymbolKind, List<Symbol>>()
    fun get(kind: SymbolKind): MutableList<Symbol> {
        return table.getOrDefault(kind, arrayListOf()).toMutableList()
    }

    fun put(kind: SymbolKind, types: MutableList<Symbol>) {
        table[kind] = types
    }

    fun define(name: String, type: String, kind: SymbolKind) {
        val types = get(kind)
        types.add(Symbol(name, type, kind))
        put(kind, types)
    }

    fun varCount(kind: SymbolKind): Int {
        return get(kind).size
    }

    fun kindOf(name: String): SymbolKind {
        for (entry in table.entries) {
            val find = entry.value.find {
                it.name == name
            }
            if (find != null) {
                return find.kind
            }
        }
        return SymbolKind.NONE
    }

    fun typeOf(name: String): String {
        for (entry in table.entries) {
            val find = entry.value.find {
                it.name == name
            }
            if (find != null) {
                return find.type
            }
        }
        return ""
    }

    fun indexOf(name: String): Int {
        for (entry in table.entries) {
            val find = entry.value.find {
                it.name == name
            }
            if (find != null) {
                return entry.value.indexOf(find)
            }
        }
        return -1
    }

    fun get(name: String): Symbol? {
        for (entry in table.entries) {
            val find = entry.value.find {
                it.name == name
            }
            if (find != null) {
                return find
            }
        }
        return null
    }

    fun clear() {
        table.clear()
    }

    override fun toString(): String {
        return table.toString()
    }
}