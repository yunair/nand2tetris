import java.io.File

enum class CommandType(val value: String) {
    C_ARITHMETIC(""), C_PUSH("push"), C_POP("pop"),
    C_LABEL("label"), C_GOTO("goto"), C_IF("if-goto"),
    C_FUNCTION("function"), C_RETURN("return"), C_CALL("call");

    companion object {
        fun fromValue(value: String): CommandType? = values().find { it.value == value }
    }


}

class Parser(private val file: File) {
    private val commands: MutableList<String> by lazy {
        val lines = file.readLines().filter {
            val line = it.trim()
            line != "" && !line.startsWith("// ")
        }.map {
            it.trim()
        }
        lines.toMutableList()
    }

    private var currentCommand: String = ""

    fun command(): String {
        return currentCommand
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
    }

    fun commandType(): CommandType {
        val items = currentCommand.split(" ")
        return CommandType.fromValue(items[0]) ?: CommandType.C_ARITHMETIC
    }

    fun arg1(): String {
        if (commandType() == CommandType.C_RETURN) {
            throw IllegalArgumentException("C_RETURN should not call this")
        }

        val items = currentCommand.split(" ")
        if (commandType() == CommandType.C_ARITHMETIC) {
            // C_ARITHMETIC 返回command自己
            return items[0]
        }
        return items[1]
    }

    fun arg2(): Int {
        if (commandType() in arrayOf(
                CommandType.C_PUSH,
                CommandType.C_POP,
                CommandType.C_FUNCTION,
                CommandType.C_CALL
            )
        ) {
            val items = currentCommand.split(" ")
            return items[2].toInt()
        }

        throw IllegalArgumentException("do not have arg2")
    }

}