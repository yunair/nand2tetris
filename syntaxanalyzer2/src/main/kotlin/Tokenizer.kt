import java.io.File

enum class TokenTypeEnum(val type: String) {
    KEYWORD("keyword"), SYMBOL("symbol"), IDENTIFIER("identifier"), INT_CONST("integerConstant"), STRING_CONST("stringConstant"), NULL(
        "null"
    )
}

val symbols =
    arrayOf("{", "}", "(", ")", "[", "]", ".", ",", ";", "+", "-", "*", "/", "&", "|", ">", "<", "=", "~")

val keyWords =
    arrayOf(
        "class", "method", "function", "constructor", "int", "boolean", "char", "void", "var", "static",
        "field", "let", "do", "if", "else", "while", "return", "true", "false", "null", "this"
    )

enum class KeyWordEnum(val keyWord: String) {
    CLASS("class"),
    METHOD("method"),
    FUNCTION("function"),
    CONSTRUCTOR("constructor"),
    INT("int"),
    BOOLEAN("boolean"),
    CHAR("char"),
    VOID("void"),
    VAR("var"),
    STATIC("static"),
    FIELD("field"),
    LET("let"),
    DO("do"),
    IF("if"),
    ELSE("else"),
    WHILE("while"),
    RETURN("return"),
    TRUE("true"),
    FALSE("false"),
    NULL("null"),
    THIS("this");

    companion object {
        fun fromValue(value: String): KeyWordEnum = values().find { it.keyWord == value }!!
    }
}

data class Token(val type: TokenTypeEnum, val command: String)

class Tokenizer(private val file: File) {
    private val commands: MutableList<String> by lazy {
        var linesComment = false
        val lines = file.readLines().filter {
            val line = it.trim()
            // 去掉多行注释
            if (line.startsWith("/*")) {
                linesComment = true
            }
            // 当前行也要忽视
            if (line.contains("*/")) {
                linesComment = false
                return@filter false
            }
            line != "" && !line.startsWith("//") && !linesComment
        }.map {
            val index = it.indexOf("//")
            if (index > 0) {
                it.removeRange(index, it.length).trim()
            } else {
                it.trim()
            }
        }.filter { it != "" }
        lines.joinToString(" ").toCharArray().toMutableList().map {
            it.toString()
        }.toMutableList()
    }

    fun hasMoreTokens(): Boolean {
        return commands.isNotEmpty()
    }

    private var currentCommand: String = ""
    private var currentTokenType = TokenTypeEnum.NULL
    private var output = File(file.parent + "/" + file.nameWithoutExtension + "T1.xml")
    private val tokens = mutableListOf<Token>()
    fun getTokens(): MutableList<Token> {
        if (tokens.isEmpty()) {
            parseTokens()
        }
        return tokens
    }

    fun advance() {
        val sb = StringBuilder()
        currentTokenType = TokenTypeEnum.NULL
        var v: String
        while (commands.removeAt(0).apply { v = this }.isEmpty());
        if (v == " ") {
            while (commands.removeAt(0).apply { v = this } == " ");
        }
        if (v == "\"") {
            // read next
            var end: String
            while (commands.removeAt(0).apply { end = this } != "\"") {
                sb.append(end)
            }
            currentTokenType = TokenTypeEnum.STRING_CONST
        } else if (symbols.contains(v)) {
            currentTokenType = TokenTypeEnum.SYMBOL
            /*when (v) {
                ">" -> v = "&gt;"
                "<" -> v = "&lt;"
                "\"" -> v = "&quot;"
                    "&" -> v = "&amp;"
            }*/
            sb.append(v)
        } else {
            sb.append(v)
            var end: String
            while (commands.removeAt(0).apply { end = this } != " ") {
                if (symbols.contains(end)) {
                    commands.add(0, end)
                    break
                }
                sb.append(end)
            }
//            println(sb.toString())
            currentTokenType = if (keyWords.contains(sb.toString())) {
                TokenTypeEnum.KEYWORD
            } else {
                try {
                    Integer.parseInt(sb.toString())
                    TokenTypeEnum.INT_CONST
                } catch (e: Exception) {
                    TokenTypeEnum.IDENTIFIER
                }
            }

        }
        currentCommand = sb.toString()
        tokens.add(Token(currentTokenType, currentCommand))
    }

    fun keyWord(): KeyWordEnum {
        return KeyWordEnum.fromValue(currentCommand)
    }

    fun tokenType(): TokenTypeEnum {
        return currentTokenType
    }

    fun symbol(): Char {
        return currentCommand[0]
    }

    fun identifier(): String {
        return currentCommand
    }

    fun intVal(): Int {
        return Integer.parseInt(currentCommand)
    }

    fun stringVal(): String {
        return currentCommand
    }

    private fun write(tokenType: TokenTypeEnum, content: String) {
        val type = tokenType.type
        output.appendText("<$type> $content </$type>\n")
    }

    fun writeStart() {
        output.writeText("<tokens>\n")
    }

    fun writeEnd() {
        output.appendText("</tokens>")
    }

    fun writeXml() {
        writeStart()
        getTokens().forEach {
            write(it.type, it.command)
        }
        writeEnd()
    }


    fun writeType() {
        when (tokenType()) {
            TokenTypeEnum.INT_CONST -> {
                write(tokenType(), intVal().toString())
            }
            TokenTypeEnum.KEYWORD -> {
                write(tokenType(), currentCommand)
            }
            TokenTypeEnum.SYMBOL -> {
                write(tokenType(), currentCommand)
            }
            TokenTypeEnum.STRING_CONST -> {
                write(tokenType(), stringVal())
            }
            TokenTypeEnum.IDENTIFIER -> {
                write(tokenType(), identifier())
            }
            else -> {
            }
        }
    }

    fun parseTokens() {
        // 处理所有token
        while (hasMoreTokens()) {
            advance()
        }
    }
}