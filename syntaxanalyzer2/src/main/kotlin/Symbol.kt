data class Symbol(
    val name: String,
    /*类型:int|string|boolean*/ val type: String,
    /*种类: static|field|var*/ val kind: SymbolKind
) {
    fun wrapper(index: Int): SymbolWrapper {
        return SymbolWrapper(this.name, this.type, this.kind, index)
    }
}

data class SymbolWrapper(
    val name: String,
    /*类型:int|string|boolean*/ val type: String,
    /*种类: static|field|var*/ val kind: SymbolKind,
    val index: Int
)

val types = mutableListOf("int", "char", "boolean", "Array", "String", "PongGame", "Bat")

