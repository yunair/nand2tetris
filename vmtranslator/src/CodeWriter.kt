import java.io.File

private const val LOCAL = "LCL"
private const val ARG = "ARG"
private const val SP = "SP"
private const val THIS = "THIS"
private const val THAT = "THAT"
private const val TEMP = "5"
private const val POINTER = "3"

class CodeWriter(private val outputFile: File) {
    private var content: String = ""

    fun addComment(command: String) {
        content += "// $command\n"
    }

    private fun addCommand(command: String) {
        content += "$command\n"
    }

    var fileName = ""
        set(value) {
            functionName = ""
            field = value
        }

    private var temp = 1

    fun writeArithmetic(command: String) {
        when (command) {
            "add" -> {
                // 两者相加
                operateTwoStackValue("+")
            }
            "sub" -> {
                // 两者相减
                operateTwoStackValue("-")
            }
            "neg" -> {
                getValueFromStack()
                writeValueToStack("-D")
            }
            "eq" -> {
                judgeCommand("JEQ")
            }
            "gt" -> {
                judgeCommand("JGT")
            }
            "lt" -> {
                judgeCommand("JLT")
            }
            "and" -> {
                operateTwoStackValue("&")
            }
            "or" -> {
                // 两者or
                operateTwoStackValue("|")
            }
            "not" -> {
                getValueFromStack()
                writeValueToStack("!D")
            }
        }
    }

    private fun operateTwoStackValue(operate: String) {
        getValueFromStack()
        // 将SP第一次pop的值给i
        addCommand("@R13")
        addCommand("M=D")

        getValueFromStack()
        // 两者operate
        addCommand("@R13")
        addCommand("D=D${operate}M")
        writeValueToStack()
    }

    private fun judgeCommand(judge: String) {
        getValueFromStack()
        // 将SP第一次pop的值给i
        addCommand("@R13")
        addCommand("M=D")

        getValueFromStack()
        // 两者相减
        addCommand("@R13")
        addCommand("D=D-M")
        val tag = "Judge$temp"
        val endTag = "E$tag"
        temp++
        // 判断成功，则将-1写到栈上
        // 判断失败，则将0写到栈上

        addCommand("@$tag")
        addCommand("D;$judge")
        addCommand("D=-1")
        addCommand("@$endTag")
        addCommand("0;JMP")
        addCommand("($tag)")
        addCommand("D=0")
        addCommand("($endTag)")
        writeValueToStack()
    }

    private fun getValueFromStack() {
        addCommand("@$SP")
        addCommand("AM=M-1")
        addCommand("D=M")
    }

    private fun writeValueToStack(value: String = "D") {
        addCommand("@$SP")
        addCommand("A=M")
        addCommand("M=$value")
        addCommand("@$SP")
        addCommand("M=M+1")
    }


    private fun writeValueToDirectSegment(segment: String, index: Int) {
        if (segment !in arrayOf(TEMP, POINTER)) {
            throw IllegalArgumentException("this $segment is not supported")
        }
        getValueFromStack()
        // 把value存到对应Index处
        addCommand("@$segment")
//        addCommand("D=A")
//        addCommand("@$index")
//        addCommand("A=A+D")
        for (i in 0 until index) {
            addCommand("A=A+1")
        }
        addCommand("M=D")
    }

    private fun writeDirectSegmentValueToStack(segment: String, index: Int) {
        if (segment !in arrayOf(TEMP, POINTER)) {
            throw IllegalArgumentException("this $segment is not supported")
        }
        addCommand("@$segment")
        for (i in 0 until index) {
            addCommand("A=A+1")
        }
        /* addCommand("D=A")
         addCommand("@$index")
         addCommand("A=A+D")*/
        addCommand("D=M")
        writeValueToStack()
    }

    private fun writeValueToTemp(index: Int) {
        writeValueToDirectSegment(TEMP, index)
    }

    private fun writeTempValueToStack(index: Int) {
        writeDirectSegmentValueToStack(TEMP, index)
    }

    private fun writeValueToPointer(index: Int) {
        writeValueToDirectSegment(POINTER, index)
    }

    private fun writePointerValueToStack(index: Int) {
        writeDirectSegmentValueToStack(POINTER, index)
    }

    private fun writeValueToPointSegment(segment: String, index: Int) {
        if (segment !in arrayOf(LOCAL, ARG, THIS, THAT)) {
            throw IllegalArgumentException("this $segment is not supported")
        }
        getValueFromStack()
        // 把value存到对应Index处
        addCommand("@$segment")
        addCommand("A=M")
        for (i in 0 until index) {
            addCommand("A=A+1")
        }
        addCommand("M=D")
    }

    private fun writePointSegmentValueToStack(segment: String, index: Int) {
        if (segment !in arrayOf(LOCAL, ARG, THIS, THAT)) {
            throw IllegalArgumentException("this $segment is not supported")
        }
        addCommand("@$segment")
        addCommand("A=M")
        for (i in 0 until index) {
            addCommand("A=A+1")
        }
//        addCommand("@$index")
//        addCommand("A=A+D")
        addCommand("D=M")
        writeValueToStack()
    }

    private fun writeValueToLocal(index: Int) {
        writeValueToPointSegment(LOCAL, index)
    }

    private fun writeLocalValueToStack(index: Int) {
        writePointSegmentValueToStack(LOCAL, index)
    }

    private fun writeValueToArg(index: Int) {
        writeValueToPointSegment(ARG, index)
    }

    private fun writeArgValueToStack(index: Int) {
        writePointSegmentValueToStack(ARG, index)
    }

    private fun writeValueToThis(index: Int) {
        writeValueToPointSegment(THIS, index)
    }

    private fun writeThisValueToStack(index: Int) {
        writePointSegmentValueToStack(THIS, index)
    }

    private fun writeValueToThat(index: Int) {
        writeValueToPointSegment(THAT, index)
    }

    private fun writeThatValueToStack(index: Int) {
        writePointSegmentValueToStack(THAT, index)
    }


    fun writePushPop(command: CommandType, segment: String, index: Int) {
        when (command) {
            CommandType.C_PUSH -> {
                when (segment) {
                    "constant" -> {
                        addCommand("@$index")
                        addCommand("D=A")
                        writeValueToStack()
                    }
                    "local" -> {
                        writeLocalValueToStack(index)
                    }
                    "argument" -> {
                        writeArgValueToStack(index)
                    }
                    "this" -> {
                        writeThisValueToStack(index)
                    }
                    "that" -> {
                        writeThatValueToStack(index)
                    }
                    "pointer" -> {
                        writePointerValueToStack(index)
                    }
                    "temp" -> {
                        writeTempValueToStack(index)
                    }
                    "static" -> {
                        addCommand("@$fileName.$index")
                        addCommand("D=M")
                        writeValueToStack()
                    }
                }
            }
            CommandType.C_POP -> {
                when (segment) {
                    "local" -> {
                        writeValueToLocal(index)
                    }
                    "argument" -> {
                        writeValueToArg(index)
                    }
                    "this" -> {
                        writeValueToThis(index)
                    }
                    "that" -> {
                        writeValueToThat(index)
                    }
                    "pointer" -> {
                        writeValueToPointer(index)
                    }
                    "temp" -> {
                        writeValueToTemp(index)
                    }
                    "static" -> {
                        getValueFromStack()
                        addCommand("@$fileName.$index")
                        addCommand("M=D")
                    }
                }
            }
            else -> throw IllegalArgumentException("$command command is illegal")
        }
    }

    fun writeInit() {
        // SP=256
        addCommand("@256")
        addCommand("D=A")
        addCommand("@$SP")
        addCommand("M=D")
        // call sys.init
        writeCall("Sys.init", 0)
    }

    fun writeLabel(label: String) {
        addCommand("(${label(label)})")
    }

    fun writeGoto(label: String) {
        addCommand("@${label(label)}")
        addCommand("0;JMP")
    }

    fun writeIf(label: String) {
        getValueFromStack()
        addCommand("@${label(label)}")
        addCommand("D;JLT")
    }

    private var tempFunCall = 0
    fun writeCall(functionName: String, numArgs: Int) {
        // push returnAddress
        addCommand("@$functionName\$ret.$tempFunCall")
        addCommand("D=A")
        writeValueToStack()
        // push LCL
        addCommand("@$LOCAL")
        addCommand("D=M")
        writeValueToStack()
        // push ARG
        addCommand("@$ARG")
        addCommand("D=M")
        writeValueToStack()
        // push THIS
        addCommand("@$THIS")
        addCommand("D=M")
        writeValueToStack()
        // push THAT
        addCommand("@$THAT")
        addCommand("D=M")
        writeValueToStack()
        // ARG = SP - 5 - nArgs
        addCommand("@$SP")
        addCommand("D=M")
        addCommand("@5")
        addCommand("D=D-A")
        addCommand("@$numArgs")
        addCommand("D=D-A")
        addCommand("@$ARG")
        addCommand("M=D")
        // LCL = SP
        addCommand("@$SP")
        addCommand("D=M")
        addCommand("@$LOCAL")
        addCommand("M=D")
        // goto functionName
        addCommand("@$functionName")
        addCommand("0;JMP")
        // (returnAddress)
        addCommand("($functionName\$ret.$tempFunCall)")
        tempFunCall++
    }

    var functionName = ""
    fun writeFunction(functionName: String, numArgs: Int) {
        this.functionName = functionName
        addCommand("($functionName)")
        for (i in 0 until numArgs) {
            writeValueToStack("0")
//            writePushPop(CommandType.C_POP, LOCAL, i)
        }
    }

    fun writeReturn() {
        // endFrame = LCL
        addCommand("@$LOCAL")
        addCommand("D=M")
        addCommand("@R13")
        addCommand("M=D")
        // retAddr = *(endFrame - 5)
        addCommand("@5")
        addCommand("A=D-A")
        addCommand("D=M")
        addCommand("@R14")
        addCommand("M=D")
        // *ARG = pop()
        getValueFromStack()
        addCommand("@$ARG")
        addCommand("A=M")
        addCommand("M=D")
        // SP = ARG + 1
        addCommand("@$ARG")
        addCommand("D=M+1")
        addCommand("@$SP")
        addCommand("M=D")
        // THAT = *(endFrame - 1)
        returnFrame(1, THAT)
        // THIS = *(endFrame - 2)
        returnFrame(2, THIS)
        // ARG = *(endFrame - 3)
        returnFrame(3, ARG)
        // LCL = *(endFrame - 4)
        returnFrame(4, LOCAL)
        // goto retAddr
        addCommand("@R14")
        addCommand("A=M")
        addCommand("0;JMP")
    }

    private fun returnFrame(minus: Int, segment: String) {
        addCommand("@R13")
        addCommand("D=M")
        addCommand("@$minus")
        addCommand("A=D-A")
        addCommand("D=M")
        addCommand("@$segment")
        addCommand("M=D")
    }

    private fun label(label: String): String {
        return if (functionName.isEmpty()) {
            label
        } else {
            "$functionName$$label"
        }
    }

    fun close() {
        outputFile.writeText(content)
    }
}