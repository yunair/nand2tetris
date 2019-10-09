package com.air.assembler

object SymbolTable {
    private val map = hashMapOf<String, Int>()

    init {
        map["SP"] = 0
        map["LCL"] = 1
        map["ARG"] = 2
        map["THIS"] = 3
        map["THAT"] = 4
        map["R0"] = 0
        map["R1"] = 1
        map["R2"] = 2
        map["R3"] = 3
        map["R4"] = 4
        map["R5"] = 5
        map["R6"] = 6
        map["R7"] = 7
        map["R8"] = 8
        map["R9"] = 9
        map["R10"] = 10
        map["R11"] = 11
        map["R12"] = 12
        map["R13"] = 13
        map["R14"] = 14
        map["R15"] = 15
        map["SCREEN"] = 16384
        map["KBD"] = 24576
    }

    fun addEntry(symbol: String, address: Int) {
        map[symbol] = address
    }

    fun contains(symbol: String): Boolean {
        return map.containsKey(symbol)
    }

    fun getAddress(symbol: String): Int {
        return map[symbol] ?: 0
    }

    override fun toString(): String {
        return map.toString()
    }
}

/*
fun main(args: Array<String>) {
    println(SymbolTable.contains("R0"))
}*/
