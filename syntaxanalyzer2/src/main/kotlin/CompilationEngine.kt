import java.io.File

/**
 * 从tokenizer读取输入，输出写入VMWriter
 */
class CompilationEngine(inputFile: File, private val writer: VMWriter) {
    private val tokenizer = Tokenizer(inputFile)
    private val tokens = tokenizer.getTokens()
    private val classSymbolTable = SymbolTable()
    private val subSymbolTable = SymbolTable()
    private lateinit var className: String
    private var labelIndex = 0
    fun start() {
        compileClass()
        println()
        println(classSymbolTable)
    }

    private fun write(token: Token) {
        val type = token.type.type
        // outputFile.appendText("<$type> ${token.command} </$type>\n")
    }

    /**
     * class:  'class' className '{' classVarDec* subroutineDec* '}'
     * classVarDec: {'static' | 'field'} type varName(', varName)* ';'
     * type: 'int' | 'char' | 'boolean' | className
     * subroutineDec: {'constructor'| 'function' |  'method'}
     *               ('void' | type) subroutineName '(' parameterList ')'
     *                subroutineBody
     * parameterList ((type varName) (',' type varName)*)?
     * subroutineBody: '{' varDec* statements '}'
     * varDec: 'var' type varName (',' varName)* ';'
     * className: identifier
     * subroutineName: identifier
     * varName: identifier
     *
     * -----------------
     * statements: statement*
     * statement: letStatement|ifStatement|whileStatement|doStatement|returnStatement
     * letStatement: 'let' varName('[' expression ']')? '=' expression ';'
     * ifStatement: 'if' '(' expression ')' '{' statements '}' ('else' '{' statements '}')?
     * whileStatement: 'while' '(' expression ')' '{' statements '}'
     * doStatement: 'do' subroutineCall ';'
     * ReturnStatement: 'return' expression? ';'
     *
     * -------------------
     * expression: term (op term)*
     * term: integerConstant|stringConstant|keywordConstant|varName|varName'['expression']'|subroutineCall|'('expression')'|unaryOp term
     * subroutineCall: subroutineName'(' expressionList ')' | (className|varName)'.'subroutineName'('expressionList')'
     * expressionList: (expression(',' expression)*)?
     * op: '+' | '-' | '*' | '/' | '&' | '|' | '<' | '>' | '='
     * unaryOp: '-' | '~'
     * keywordConstant: 'true' | 'false' | 'null' | 'this'
     */

    private fun compileClass() {
        // outputFile.appendText("<class>\n")
        var token = tokens.removeAt(0)
        if (token.type == TokenTypeEnum.KEYWORD && token.command == KeyWordEnum.CLASS.keyWord) {
            write(token)
        } else {
            throw IllegalArgumentException("class token is not found")
        }
        token = tokens.removeAt(0)
        if (token.type == TokenTypeEnum.IDENTIFIER) {
            // 类名也是一种type
            types.add(token.command)
            write(token)
            className = token.command
        } else {
            throw IllegalArgumentException("class name error")
        }
        parseSymbolToken("{")
        while (tokens.isNotEmpty()) {
            val symbol = getSymbol("}")
            if (symbol != null) {
                write(symbol)
                break
            } else {
                token = tokens.removeAt(0)
                if (token.type == TokenTypeEnum.KEYWORD && (token.command == KeyWordEnum.STATIC.keyWord || token.command == KeyWordEnum.FIELD.keyWord)) {
                    tokens.add(0, token)
                    compileClassVarDec()
                }

                if (token.type == TokenTypeEnum.KEYWORD && (token.command == KeyWordEnum.CONSTRUCTOR.keyWord || token.command == KeyWordEnum.FUNCTION.keyWord || token.command == KeyWordEnum.METHOD.keyWord)) {
                    tokens.add(0, token)
                    compileSubroutine()
                }
            }

        }
        // outputFile.appendText("</class>")
    }

    private fun compileClassVarDec() {
        // outputFile.appendText("<classVarDec>\n")
        // {'static' | 'field'}
        parseVarDec("class var")
        // outputFile.appendText("</classVarDec>\n")
    }

    private fun compileSubroutine() {
        // outputFile.appendText("<subroutineDec>\n")
        // {'constructor'| 'function' |  'method'}
        var token = tokens.removeAt(0)
        val isConstructor = token.command == KeyWordEnum.CONSTRUCTOR.keyWord
        // 'constructor'| 'function' 不用加this这个参数, 所以不需要额外的1
        val extraParamCount =
            if (isConstructor || token.command == KeyWordEnum.FUNCTION.keyWord) 0 else 1
        write(token)
        // ('void' | type)
        token = tokens.removeAt(0)
        val returnVoid = token.type == TokenTypeEnum.KEYWORD && token.command == "void"
        if (returnVoid || types.contains(token.command)) {
            write(token)
        } else {
            throw IllegalArgumentException("subroutine type ${token.command} is illegal")
        }
        subSymbolTable.clear()
        // subroutineName
        val name = parseIdentifier("subroutine").command
        // '('
        parseSymbolToken("(")
        if (extraParamCount != 0) {
            subSymbolTable.define("this", className, SymbolKind.ARG)
        }
        // parameterList
        compileParameterList()
        // ')'
        parseSymbolToken(")")

        // subroutineBody
        compileSubroutineBody(name, isConstructor)
        if (returnVoid) {
            writer.writePush(Segment.CONST, 0)
        }
        // 函数结束一定要有return
        writer.writeReturn()
        // '}'
        // outputFile.appendText("</subroutineDec>\n")
        println("subSymbolTable")
        println(subSymbolTable)
    }

    private fun compileSubroutineBody(name: String, isConstructor: Boolean) {
        // outputFile.appendText("<subroutineBody>\n")
        // '{' varDec* statements '}'
        parseSymbolToken("{")
        while (tokens.isNotEmpty()) {
            val token = tokens.removeAt(0)
            if (token.command != "var") {
                tokens.add(0, token)
                break
            } else {
                tokens.add(0, token)
                compileVarDec()
            }
        }
        val varCount = subSymbolTable.varCount(SymbolKind.VAR)
        // varCount 代表的是local字段的数量
        writer.writeFunction("${className}.$name", varCount)
        if (isConstructor) {
            // class-level symbol table field count
            writer.writePush(Segment.CONST, classSymbolTable.varCount(SymbolKind.FIELD))
            writer.writeCall("Memory.alloc", 1)
            // anchors `this` at the base address
            writer.writePop(Segment.POINTER, 0)
        } else if (subSymbolTable.get("this") != null) {
            writer.writePush(Segment.ARG, 0)
            writer.writePop(Segment.POINTER, 0) // THIS = argument 0
        }
        compileStatements()
        parseSymbolToken("}")
        // outputFile.appendText("</subroutineBody>\n")
    }

    private fun parseIdentifier(typeName: String): Token {
        val token = tokens.removeAt(0)
        if (token.type == TokenTypeEnum.IDENTIFIER) {
            return token
        } else {
            throw IllegalArgumentException("$typeName name ${token.command} is illegal")
        }
    }

    private fun parseSymbolToken(aimSymbol: String): Token {
        require(tokens.isNotEmpty()) { "can't find '$aimSymbol'" }
        val token = tokens.removeAt(0)
        if (token.type == TokenTypeEnum.SYMBOL && token.command == aimSymbol) {
            write(token)
        } else {
            throw IllegalArgumentException("can't find '$aimSymbol'")
        }
        return token
    }

    private fun compileParameterList() {
        // (type varName) (',' type varName)*
        // outputFile.appendText("<parameterList>\n")
//        var count = 0
        parseTypeVarName()
        while (tokens.isNotEmpty()) {
            val symbol = getSymbol(",")
            if (symbol != null) {
//                count++
                write(symbol)
                parseTypeVarName()
            } else {
                break
            }
        }
//        return count
        // outputFile.appendText("</parameterList>\n")
    }

    private fun parseTypeVarName() {
        val token = tokens.removeAt(0)
        if (types.contains(token.command)) {
            write(token)
            val identifier = parseIdentifier("var")
            subSymbolTable.define(identifier.command, token.command, SymbolKind.ARG)
        } else {
            tokens.add(0, token)
        }
    }

    private fun compileVarDec() {
        // outputFile.appendText("<varDec>\n")
        // 'var'
        parseVarDec("subroutine var")
        // outputFile.appendText("</varDec>\n")
    }

    private fun parseVarDec(name: String) {
        // 'var|static|field' type varName (',' varName)* ';'
        val token = tokens.removeAt(0)
        write(token)
        // type
        val type = parseType(name)
        // varName
        val varName = parseIdentifier(name)
        if (name.contains("class")) {
            classSymbolTable.define(varName.command, type.command, SymbolKind.get(token.command))
//            println(classSymbolTable)
        } else {
            subSymbolTable.define(varName.command, type.command, SymbolKind.VAR)
        }
        while (tokens.isNotEmpty()) {
            val symbol = getSymbol(",")
            if (symbol != null) {
                write(symbol)
                val identify = parseIdentifier(name)
                if (name.contains("class")) {
                    classSymbolTable.define(identify.command, type.command, SymbolKind.get(token.command))
                } else {
                    subSymbolTable.define(identify.command, type.command, SymbolKind.VAR)
                }
            } else {
                parseSymbolToken(";")
                return
            }
        }
    }

    private fun parseType(name: String): Token {
        val token = tokens.removeAt(0)
        // type
        if (types.contains(token.command)) {
            return token
        } else {
            throw IllegalArgumentException("${token.command} $name type is not found")
        }
    }

    private val statementsKey = arrayListOf("let", "if", "while", "do", "return")
    private val statementsHandler = mapOf(
        Pair(KeyWordEnum.LET.keyWord, { compileLet() }),
        Pair(KeyWordEnum.IF.keyWord, { compileIf() }),
        Pair(KeyWordEnum.WHILE.keyWord, { compileWhile() }),
        Pair(KeyWordEnum.DO.keyWord, { compileDo() }),
        Pair(KeyWordEnum.RETURN.keyWord, { compileReturn() })
    )

    private fun compileStatements() {
        // outputFile.appendText("<statements>\n")
        while (tokens.isNotEmpty()) {
            val token = tokens.removeAt(0)
            if (statementsKey.contains(token.command)) {
                tokens.add(0, token)
                statementsHandler[token.command]?.invoke()
            } else {
                tokens.add(0, token)
                break
            }
        }
        // outputFile.appendText("</statements>\n")
    }

    // return void need push constant 0 as dummy value
    // the caller of a void method must dump the returned value
    private fun compileDo() {
        // outputFile.appendText("<doStatement>\n")
        // 'do'
        val token = tokens.removeAt(0)
        write(token)
        // subroutineCall
        compileSubroutineCall()
        // ';'
        writer.writePop(Segment.TEMP, 0)
        parseSymbolToken(";")
        // outputFile.appendText("</doStatement>\n")
    }

    private fun compileLet() {
        // outputFile.appendText("<letStatement>\n")
        // 'let'
        val token = tokens.removeAt(0)
        write(token)
        // varName
        val nameToken = parseIdentifier("let var")
        // find in subSymbolTable
        val wrapper = getSymbolWrapper(nameToken)
        // ('[' expression ']')?
        val symbol = getSymbol("[")
        if (symbol != null) {
            writer.writePush(Segment.get(wrapper.kind.value), wrapper.index)
            write(symbol)
            compileExpression()
            parseSymbolToken("]")
            writer.writeArithmetic(Command.ADD)
        }
        // '='
        parseSymbolToken("=")
        // expression
        compileExpression()
        // ';'
        parseSymbolToken(";")
        if (symbol != null) {
            writer.writePop(Segment.TEMP, 0)
            writer.writePop(Segment.POINTER, 1)
            writer.writePush(Segment.TEMP, 0)
            writer.writePop(Segment.THAT, 0)
        } else {
            writer.writePop(Segment.get(wrapper.kind.value), wrapper.index)
        }
        // outputFile.appendText("</letStatement>\n")
    }

    private fun compileWhile() {
        // outputFile.appendText("<whileStatement>\n")
        //'while'
        val labelStart = "${className}.W${labelIndex}"
        labelIndex += 1
        val labelEnd = "${className}.W${labelIndex}"
        labelIndex += 1
        writer.writeLabel(labelStart)
        val token = tokens.removeAt(0)
        write(token)
        // '('
        parseSymbolToken("(")
        // expression
        compileExpression()
        writer.writeArithmetic(Command.NOT)
        writer.writeIf(labelEnd)
        // ')'
        parseSymbolToken(")")
        // '{'
        parseSymbolToken("{")
        // statements
        compileStatements()
        // '}'
        writer.writeGoto(labelStart)
        parseSymbolToken("}")
        writer.writeLabel(labelEnd)
        // outputFile.appendText("</whileStatement>\n")
    }

    private fun compileReturn() {
        // outputFile.appendText("<returnStatement>\n")
        // 'return'
        val token = tokens.removeAt(0)
        write(token)
        // expression?
        // ';'
        val symbol = getSymbol(";")
        if (symbol == null) {
            compileExpression()
            parseSymbolToken(";")
        } else {
            write(symbol)
        }
        // outputFile.appendText("</returnStatement>\n")
    }

    private fun compileIf() {
        // outputFile.appendText("<ifStatement>\n")
        // 'if'
        val labelStart = "${className}.L${labelIndex}"
        labelIndex += 1
        val labelEnd = "${className}.L${labelIndex}"
        labelIndex += 1
        var token = tokens.removeAt(0)
        write(token)
        // '('
        parseSymbolToken("(")
        // expression
        compileExpression()
        writer.writeArithmetic(Command.NOT)
        writer.writeIf(labelStart)
        // ')'
        parseSymbolToken(")")
        // '{'
        parseSymbolToken("{")
        // statements
        compileStatements()
        writer.writeGoto(labelEnd)
        //  '}'
        parseSymbolToken("}")
        // ('else' '{' statements '}')?
        token = tokens.removeAt(0)
        if (token.type == TokenTypeEnum.KEYWORD && token.command == "else") {
            write(token)
            // '{'
            parseSymbolToken("{")
            writer.writeLabel(labelStart)
            // statements
            compileStatements()
            //  '}'
            parseSymbolToken("}")
        } else {
            writer.writeLabel(labelStart)
            tokens.add(0, token)
        }
        writer.writeLabel(labelEnd)
        // outputFile.appendText("</ifStatement>\n")
    }

    private val ops = arrayListOf("+", "-", "*", "/", "&", "|", "<", ">", "=", "&lt;", "&gt;", "&amp;")
    private fun compileExpression() {
        // outputFile.appendText("<expression>\n")
        // term (op term)*
        compileTerm()
        while (tokens.isNotEmpty()) {
            val token = tokens.removeAt(0)
            if (token.type == TokenTypeEnum.SYMBOL && ops.contains(token.command)) {
//                println(token.command)
                write(token)
                compileTerm()
                when (token.command) {
                    "+" -> {
                        writer.writeArithmetic(Command.ADD)
                    }
                    "-" -> {
                        writer.writeArithmetic(Command.SUB)
                    }
                    "*" -> {
                        writer.writeCall("Math.multiply", 2)
                    }
                    "/" -> {
                        writer.writeCall("Math.divide", 2)
                    }
                    ">" -> {
                        writer.writeArithmetic(Command.GT)
                    }
                    "<" -> {
                        writer.writeArithmetic(Command.LT)
                    }
                    "&" -> {
                        writer.writeArithmetic(Command.AND)
                    }
                    "=" -> {
                        writer.writeArithmetic(Command.EQ)
                    }
                }
            } else {
                tokens.add(0, token)
                break
            }
        }

        // outputFile.appendText("</expression>\n")
    }

    private val unaryOps = arrayListOf("~", "-")
    private val keyWordConstants = arrayListOf("true", "false", "null", "this")
    private fun compileTerm() {
        // integerConstant|stringConstant|keywordConstant|varName|varName'['expression']'|subroutineCall|'('expression')'|unaryOp term
        // outputFile.appendText("<term>\n")
        var token = tokens.removeAt(0)
        if (token.type == TokenTypeEnum.INT_CONST) {
            // integerConstant
            write(token)
            writer.writePush(Segment.CONST, token.command.toInt())
        } else if (token.type == TokenTypeEnum.STRING_CONST) {
            // stringConstant
            write(token)
            writer.writePush(Segment.CONST, token.command.length)
            writer.writeCall("String.new", 1)
            for (ch in token.command) {
                writer.writePush(Segment.CONST, ch.toInt())
                writer.writeCall("String.appendChar", 2)
            }
        } else if (token.type == TokenTypeEnum.KEYWORD) {
            // keywordConstant
            if (keyWordConstants.contains(token.command)) {
                when {
                    token.command == "true" -> {
                        // true 被映射到常数 -1
                        writer.writePush(Segment.CONST, 1)
                        writer.writeArithmetic(Command.NEG)
                    }
                    token.command == "this" -> {
//                        println("this")
//                        println(token)
                        // 只可能出现在return 之后, 构造函数中
                        // return this
                        writer.writePush(Segment.POINTER, 0)
                    }
                    else -> // null 和 false被映射到常数 0
                        writer.writePush(Segment.CONST, 0)
                }
                write(token)
            }
        } else if (token.type == TokenTypeEnum.IDENTIFIER) {
            // varName|varName'['expression']'|subroutineCall
            val curIdentify = token
            token = tokens.removeAt(0)

            if (token.type == TokenTypeEnum.SYMBOL) {
                // varName'['expression']'|subroutineCall
                if (token.command == "." || token.command == "(") {
                    tokens.add(0, token)
                    tokens.add(0, curIdentify)
                    compileSubroutineCall()
                } else if (token.command == "[") {
                    val wrapper = getSymbolWrapper(curIdentify)
                    writer.writePush(Segment.get(wrapper.kind.value), wrapper.index)
                    write(curIdentify)
                    write(token)
                    compileExpression()
                    parseSymbolToken("]")
                    writer.writeArithmetic(Command.ADD)
                    writer.writePop(Segment.POINTER, 1)
                    writer.writePush(Segment.THAT, 0)
                } else {
                    // varName
                    tokens.add(0, token)
                    write(curIdentify)

                    val wrapper = getSymbolWrapper(curIdentify)
                    writer.writePush(Segment.get(wrapper.kind.value), wrapper.index)
                }
            } else {
                tokens.add(0, token)
                write(curIdentify)
            }
        } else if (unaryOps.contains(token.command)) {
            // unaryOp term
            write(token)
            compileTerm()
            if (token.command == "-") {
                writer.writeArithmetic(Command.NEG)
            } else {
                writer.writeArithmetic(Command.NOT)
            }
        } else if (token.type == TokenTypeEnum.SYMBOL && token.command == "(") {
            write(token)
            compileExpression()
            parseSymbolToken(")")
        }

        // outputFile.appendText("</term>\n")
    }

    private fun compileExpressionList(): Int {
        // (expression(',' expression)*)?
        // outputFile.appendText("<expressionList>\n")
        // '(' expressionList ')'
        // 检查得到的token是不是)
        val symbol = getSymbol(")")
        if (symbol != null) {
            tokens.add(0, symbol)
            return 0
        } else {
            compileExpression()
            var count = 1
            while (tokens.isNotEmpty()) {
                val comma = getSymbol(",")
                if (comma != null) {
                    write(comma)
                    count++
                    compileExpression()
                } else {
                    break
                }
            }
            return count
        }
        // outputFile.appendText("</expressionList>\n")
    }

    // obj.foo(x1)
    // push obj address
    // push x1
    // call foo
    private fun compileSubroutineCall() {
        //subroutineCall: subroutineName'(' expressionList ')' | (className|varName)'.'subroutineName'('expressionList')'
        val token = tokens.removeAt(0)
        val symbol = getSymbol(".")
        if (symbol == null) {
            tokens.add(0, token)
            // 不加.调用，一定是当前方法
            writer.writePush(Segment.POINTER, 0)
            val subroutine = parseCurClassSubroutineCall()
            // 需要加this这个arg
            writer.writeCall("${className}.${subroutine.name}", subroutine.paramCount + 1)
        } else {
            write(token)
            write(symbol)
            val invoker: String
            val thisParamCount: Int
            if (subSymbolTable.kindOf(token.command) == SymbolKind.NONE && classSymbolTable.kindOf(token.command) == SymbolKind.NONE) {
                // className
                invoker = token.command
                thisParamCount = 0
            } else {
                // varName
                invoker = if (subSymbolTable.typeOf(token.command).isEmpty()) {
                    writer.writePush(Segment.THIS, classSymbolTable.indexOf(token.command))
                    classSymbolTable.typeOf(token.command)
                } else {
                    writer.writePush(Segment.LOCAL, subSymbolTable.indexOf(token.command))
                    subSymbolTable.typeOf(token.command)
                }
                thisParamCount = 1
            }
            val subroutine = parseCurClassSubroutineCall()
            writer.writeCall("${invoker}.${subroutine.name}", subroutine.paramCount + thisParamCount)

        }

    }

    private fun parseCurClassSubroutineCall(): Subroutine {
        val token = tokens.removeAt(0)
        if (token.type == TokenTypeEnum.IDENTIFIER) {
            val subroutineName = token.command
            write(token)
            parseSymbolToken("(")
            val paramCount = compileExpressionList()
            parseSymbolToken(")")
            return Subroutine(subroutineName, paramCount)
        } else {
            throw IllegalArgumentException(" subroutineName'(' expressionList ')' compile failed")
        }
    }

    private fun getSymbol(symbol: String): Token? {
        val token = tokens.removeAt(0)
        return if (token.type == TokenTypeEnum.SYMBOL && token.command == symbol) {
            token
        } else {
            tokens.add(0, token)
            null
        }
    }

    private fun getSymbolWrapper(token: Token): SymbolWrapper {
        val subroutineIndex = subSymbolTable.indexOf(token.command)
        val classIndex = classSymbolTable.indexOf(token.command)
        val varIndex = if (subroutineIndex == -1) classIndex else subroutineIndex
        val varSymbol = (if (subroutineIndex != -1) {
            subSymbolTable.get(token.command)
        } else {
            if (classIndex != -1) {
                classSymbolTable.get(token.command)
            } else {
                throw IllegalArgumentException("var ${token.command} is not define")
            }
        })
            ?: throw IllegalArgumentException("var ${token.command} is not define")
        return varSymbol.wrapper(varIndex)
    }
}