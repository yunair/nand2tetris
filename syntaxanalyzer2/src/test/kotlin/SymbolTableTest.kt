import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class SymbolTableTest {
    @Test
    fun shouldGetList() {
        val symbolTable = SymbolTable()

        putSymbol(symbolTable, SymbolKind.FIELD, Symbol("id", "int", SymbolKind.FIELD))
        putSymbol(symbolTable, SymbolKind.FIELD, Symbol("name", "string", SymbolKind.FIELD))
        val fieldTypes = symbolTable.get(SymbolKind.FIELD)
        assertThat(fieldTypes[0].name, `is`("id"))
        assertThat(fieldTypes[1].name, `is`("name"))
    }

    private fun putSymbol(symbolTable: SymbolTable, kind: SymbolKind, symbol: Symbol) {
        val types = symbolTable.get(kind)
        types.add(symbol)
        symbolTable.put(kind, types)
    }
}