import java.io.File

class CompilationEngine(private val inputFile: File, private val outputFile: File) {
    private val tokenizer = Tokenizer(inputFile)
    private val tokens = tokenizer.getTokens()

    init {
    }

    fun start() {
        compileClass()
    }

    private fun write(token: Token) {
        val type = token.type.type
        outputFile.appendText("<$type> ${token.command} </$type>\n")
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

    private val types = mutableListOf("int", "char", "boolean", "Array", "String", "Square", "SquareGame")

    private fun compileClass() {
        outputFile.appendText("<class>\n")
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
        outputFile.appendText("</class>")
    }

    private fun compileClassVarDec() {
        outputFile.appendText("<classVarDec>\n")
        // {'static' | 'field'}
        parseVarDec("class var")
        outputFile.appendText("</classVarDec>\n")
    }

    private fun compileSubroutine() {
        outputFile.appendText("<subroutineDec>\n")
        // {'constructor'| 'function' |  'method'}
        var token = tokens.removeAt(0)
        write(token)
        // ('void' | type)
        token = tokens.removeAt(0)
        if ((token.type == TokenTypeEnum.KEYWORD && token.command == "void") || types.contains(token.command)) {
            write(token)
        } else {
            throw IllegalArgumentException("subroutine type ${token.command} is illegal")
        }

        // subroutineName
        parseIdentifier("subroutine")
        // '('
        parseSymbolToken("(")
        // parameterList
        compileParameterList()
        // ')'
        parseSymbolToken(")")
        // subroutineBody
        compileSubroutineBody()
        // '}'
        outputFile.appendText("</subroutineDec>\n")
    }

    private fun compileSubroutineBody() {
        outputFile.appendText("<subroutineBody>\n")
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
        compileStatements()
        parseSymbolToken("}")
        outputFile.appendText("</subroutineBody>\n")
    }

    private fun parseIdentifier(typeName: String) {
        val token = tokens.removeAt(0)
        if (token.type == TokenTypeEnum.IDENTIFIER) {
            write(token)
        } else {
            throw IllegalArgumentException("$typeName name ${token.command} is illegal")
        }
    }

    private fun parseSymbolToken(aimSymbol: String): Token {
        if (tokens.isEmpty()) {
            throw IllegalArgumentException("can't find '$aimSymbol'")
        }
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
        outputFile.appendText("<parameterList>\n")
        parseTypeVarName()
        while (tokens.isNotEmpty()) {
            val symbol = getSymbol(",")
            if (symbol != null) {
                write(symbol)
                parseTypeVarName()
            } else {
                break
            }
        }
        outputFile.appendText("</parameterList>\n")
    }

    private fun parseTypeVarName() {
        val token = tokens.removeAt(0)
        if (types.contains(token.command)) {
            write(token)
            parseIdentifier("var")
        } else {
            tokens.add(0, token)
        }
    }

    private fun compileVarDec() {
        outputFile.appendText("<varDec>\n")
        // 'var'
        parseVarDec("subroutine var")
        outputFile.appendText("</varDec>\n")
    }

    private fun parseVarDec(name: String) {
        // 'var' type varName (',' varName)* ';'
        val token = tokens.removeAt(0)
        write(token)
        // type
        parseType(name)
        // varName
        parseIdentifier(name)
        while (tokens.isNotEmpty()) {
            val symbol = getSymbol(",")
            if (symbol != null) {
                write(symbol)
                parseIdentifier(name)
            } else {
                parseSymbolToken(";")
                return
            }
        }
    }

    private fun parseType(name: String) {
        val token = tokens.removeAt(0)
        // type
        if (types.contains(token.command)) {
            write(token)
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
        outputFile.appendText("<statements>\n")
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
        outputFile.appendText("</statements>\n")
    }

    private fun compileDo() {
        outputFile.appendText("<doStatement>\n")
        // 'do'
        val token = tokens.removeAt(0)
        write(token)
        // subroutineCall
        compileSubroutineCall()
        // ';'
        parseSymbolToken(";")
        outputFile.appendText("</doStatement>\n")
    }

    private fun compileLet() {
        outputFile.appendText("<letStatement>\n")
        // 'let'
        val token = tokens.removeAt(0)
        write(token)
        // varName
        parseIdentifier("let var")
        // ('[' expression ']')?
        val symbol = getSymbol("[")
        if (symbol != null) {
            write(symbol)
            compileExpression()
            parseSymbolToken("]")
        }
        // '='
        parseSymbolToken("=")
        // expression
        compileExpression()
        // ';'
        parseSymbolToken(";")
        outputFile.appendText("</letStatement>\n")
    }

    private fun compileWhile() {
        outputFile.appendText("<whileStatement>\n")
        //'while'
        val token = tokens.removeAt(0)
        write(token)
        // '('
        parseSymbolToken("(")
        // expression
        compileExpression()
        // ')'
        parseSymbolToken(")")
        // '{'
        parseSymbolToken("{")
        // statements
        compileStatements()
        // '}'
        parseSymbolToken("}")
        outputFile.appendText("</whileStatement>\n")
    }

    private fun compileReturn() {
        outputFile.appendText("<returnStatement>\n")
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
        outputFile.appendText("</returnStatement>\n")
    }

    private fun compileIf() {
        outputFile.appendText("<ifStatement>\n")
        // 'if'
        var token = tokens.removeAt(0)
        write(token)
        // '('
        parseSymbolToken("(")
        // expression
        compileExpression()
        // ')'
        parseSymbolToken(")")
        // '{'
        parseSymbolToken("{")
        // statements
        compileStatements()
        //  '}'
        parseSymbolToken("}")
        // ('else' '{' statements '}')?
        token = tokens.removeAt(0)
        if (token.type == TokenTypeEnum.KEYWORD && token.command == "else") {
            write(token)
            // '{'
            parseSymbolToken("{")
            // statements
            compileStatements()
            //  '}'
            parseSymbolToken("}")
        } else {
            tokens.add(0, token)
        }
        outputFile.appendText("</ifStatement>\n")
    }

    private val ops = arrayListOf("+", "-", "*", "/", "&", "|", "<", ">", "=", "&lt;", "&gt;", "&amp;")
    private fun compileExpression() {
        outputFile.appendText("<expression>\n")
        // term (op term)*
        compileTerm()
        while (tokens.isNotEmpty()) {
            val token = tokens.removeAt(0)
            if (token.type == TokenTypeEnum.SYMBOL && ops.contains(token.command)) {
                write(token)
                compileTerm()
            } else {
                tokens.add(0, token)
                break
            }
        }

        outputFile.appendText("</expression>\n")
    }

    private val unaryOps = arrayListOf("~", "-")
    private val keyWordConstants = arrayListOf("true", "false", "null", "this")
    private fun compileTerm() {
        // integerConstant|stringConstant|keywordConstant|varName|varName'['expression']'|subroutineCall|'('expression')'|unaryOp term
        outputFile.appendText("<term>\n")
        var token = tokens.removeAt(0)
        if (token.type == TokenTypeEnum.INT_CONST) {
            // integerConstant
            write(token)
        } else if (token.type == TokenTypeEnum.STRING_CONST) {
            // stringConstant
            write(token)
        } else if (token.type == TokenTypeEnum.KEYWORD) {
            // keywordConstant
            if (keyWordConstants.contains(token.command)) {
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
                    write(curIdentify)
                    write(token)
                    compileExpression()
                    parseSymbolToken("]")
                } else {
                    tokens.add(0, token)
                    write(curIdentify)
                }
            } else {
                // varName
                tokens.add(0, token)
                write(curIdentify)
            }
        } else if (unaryOps.contains(token.command)) {
            // unaryOp term
            write(token)
            compileTerm()
        } else if (token.type == TokenTypeEnum.SYMBOL && token.command == "(") {
            write(token)
            compileExpression()
            parseSymbolToken(")")
        }

        outputFile.appendText("</term>\n")
    }

    private fun compileExpressionList() {
        // (expression(',' expression)*)?
        outputFile.appendText("<expressionList>\n")
        // '(' expressionList ')'
        // 检查得到的token是不是)
        val symbol = getSymbol(")")
        if (symbol != null) {
            tokens.add(0, symbol)
        } else {
            compileExpression()
            while (tokens.isNotEmpty()) {
                val comma = getSymbol(",")
                if (comma != null) {
                    write(comma)
                    compileExpression()
                } else {
                    break
                }
            }
        }
        outputFile.appendText("</expressionList>\n")
    }

    private fun compileSubroutineCall() {
        //subroutineCall: subroutineName'(' expressionList ')' | (className|varName)'.'subroutineName'('expressionList')'
        val token = tokens.removeAt(0)
        val symbol = getSymbol(".")
        if (symbol == null) {
            tokens.add(0, token)
            parseCurClassSubroutineCall()
        } else {
            write(token)
            write(symbol)
            parseCurClassSubroutineCall()
        }
    }

    private fun parseCurClassSubroutineCall() {
        val token = tokens.removeAt(0)
        if (token.type == TokenTypeEnum.IDENTIFIER) {
            write(token)
            parseSymbolToken("(")
            compileExpressionList()
            parseSymbolToken(")")
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
}