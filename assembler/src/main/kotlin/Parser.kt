package com.air.assembler

import java.io.File

enum class CommandType {
    A_COMMAND, C_COMMAND, L_COMMAND
}

private const val UNKNOWN_SYMBOL_START = 16
fun main() {
    val basePath = "/Users/air/CodeSpace/learn/nand2tetris/projects/06"
    Parser(File("$basePath/pong/Pong.asm")).print()
}

class Parser(private val file: File) {
    private val destMap = mapOf(
        Pair("", "000"),
        Pair("M", "001"),
        Pair("D", "010"),
        Pair("MD", "011"),
        Pair("A", "100"),
        Pair("AM", "101"),
        Pair("AD", "110"),
        Pair("AMD", "111")
    )

    private val jumpMap = mapOf(
        Pair("", "000"),
        Pair("JGT", "001"),
        Pair("JEQ", "010"),
        Pair("JGE", "011"),
        Pair("JLT", "100"),
        Pair("JNE", "101"),
        Pair("JLE", "110"),
        Pair("JMP", "111")
    )

    private val compMap = mapOf(
        Pair("0", "0101010"),
        Pair("1", "0111111"),
        Pair("-1", "0111010"),
        Pair("D", "0001100"),
        Pair("A", "0110000"),
        Pair("M", "1110000"),
        Pair("!D", "0001101"),
        Pair("!A", "0110001"),
        Pair("!M", "1110001"),
        Pair("-D", "0001111"),
        Pair("-A", "0110011"),
        Pair("-M", "1110011"),
        Pair("D+1", "0011111"),
        Pair("A+1", "0110111"),
        Pair("M+1", "1110111"),
        Pair("D-1", "0001110"),
        Pair("A-1", "0110010"),
        Pair("M-1", "1110010"),
        Pair("D+A", "0000010"),
        Pair("D+M", "1000010"),
        Pair("D-A", "0010011"),
        Pair("D-M", "1010011"),
        Pair("A-D", "0000111"),
        Pair("M-D", "1000111"),
        Pair("D&A", "0000000"),
        Pair("D&M", "1000000"),
        Pair("D|A", "0010101"),
        Pair("D|M", "1010101")
    )


    private var currentCommand: String = ""
    private var unknownSymbolAddress = UNKNOWN_SYMBOL_START
    private val commands: MutableList<String> by lazy {
        val lines = file.readLines().filter {
            val line = it.trim()
            line != "" && !line.startsWith("// ")
        }.map {
            it.trim()
        }
        lines.toMutableList()
    }

    private val binaryCodes = arrayListOf<String>()

    fun print() {
        parseSymbol()

        while (hasMoreCommand()) {
            advance()
        }
        val outputFile = File(file.parentFile.absolutePath + "/" + file.nameWithoutExtension + ".hack")
        if (!outputFile.exists()) {
            outputFile.createNewFile()
        }
        outputFile.writeText(binaryCodes.joinToString("\n"))
    }

    private fun parseSymbol() {
        // parse L command
        var index = 0
        val iterator = commands.iterator()
        while (iterator.hasNext()) {
            currentCommand = iterator.next()
            if (commandType() == CommandType.L_COMMAND) {
                iterator.remove()
                if (!SymbolTable.contains(symbol())) {
//                    println(index)
                    SymbolTable.addEntry(symbol(), index)
                }
            } else {
                index++
            }

        }

        /*for ((index, command) in commands.withIndex()) {
            if (commandType() == CommandType.L_COMMAND) {
                if (!SymbolTable.contains(symbol())) {
                    SymbolTable.addEntry(symbol(), index)
                }
            }
        }*/


        // parse A command with L symbol
        for (command in commands) {
            currentCommand = command
            if (commandType() == CommandType.A_COMMAND) {
                if (!SymbolTable.contains(symbol())) {
                    try {
                        Integer.parseInt(symbol())
                    } catch (e: NumberFormatException) {
                        SymbolTable.addEntry(symbol(), unknownSymbolAddress)
                        unknownSymbolAddress++
                    }
                }
            }
        }
    }

    // 还有更多命令吗
    fun hasMoreCommand(): Boolean {
        return commands.isNotEmpty()
    }

    fun advance() {
        currentCommand = commands.removeAt(0)
        // 命令后面有注释，去除
        if (currentCommand.contains("//")) {
            currentCommand = currentCommand.substring(0, currentCommand.indexOf("//")).trim()
        }
        val code = when {
            commandType() == CommandType.C_COMMAND -> "111" + comp() + dest() + jump()
            commandType() == CommandType.A_COMMAND -> {
                val address = try {
                    Integer.parseInt(symbol())
                } catch (e: NumberFormatException) {
                    SymbolTable.getAddress(symbol())
                }
                intToBinary(address)
            }
            commandType() == CommandType.L_COMMAND -> {
                val address = SymbolTable.getAddress(symbol())
                intToBinary(address)
            }
            else -> ""

        }
        binaryCodes.add(code)

    }

    private fun intToBinary(value: Int): String {
        val values = Integer.toBinaryString(value)
        var zeros = ""
        for (i in 0 until (16 - values.length)) {
            zeros += "0"
        }
        return zeros + values
    }

    fun commandType(): CommandType {
        return when {
            currentCommand.startsWith("@") -> CommandType.A_COMMAND
            currentCommand.startsWith("(") -> CommandType.L_COMMAND
            else -> CommandType.C_COMMAND
        }
    }

    fun symbol(): String {
        if (commandType() == CommandType.A_COMMAND) {
            return currentCommand.substring(1)
        } else if (commandType() == CommandType.L_COMMAND) {
            return currentCommand.substring(1, currentCommand.length - 1)
        }
        return ""
    }

    private fun parseCommand(): Code {
        // dest=comp;jump
        //  If dest is empty, the ‘‘=’’ is omitted;
        // If jump is empty, the ‘‘;’’ is omitted
        val destSplits = currentCommand.split("=")
        val comp: String
        val jump: String
        val dest = if (destSplits.size == 1) {
            val compSplits = destSplits[0].split(";")
            comp = compSplits[0]
            jump = if (compSplits.size == 1) {
                ""
            } else {
                compSplits[1]
            }
            ""
        } else {
            val compSplits = destSplits[1].split(";")
            comp = compSplits[0]
            jump = if (compSplits.size == 1) {
                ""
            } else {
                compSplits[1]
            }
            destSplits[0]
        }

        return Code(dest, comp, jump)
    }

    fun dest(): String {
        if (commandType() == CommandType.C_COMMAND) {
            val code = parseCommand()
            return destMap[code.dest] ?: destMap.getValue("")
        }
        return ""
    }

    fun comp(): String {
        if (commandType() == CommandType.C_COMMAND) {
            val code = parseCommand()
            return compMap[code.comp] ?: ""
        }
        return ""
    }

    fun jump(): String {
        if (commandType() == CommandType.C_COMMAND) {
            val code = parseCommand()
            return jumpMap[code.jump] ?: jumpMap.getValue("")
        }
        return ""
    }
}