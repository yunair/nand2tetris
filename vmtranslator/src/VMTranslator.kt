import java.io.File

fun main() {
    val basePath = "/Users/air/CodeSpace/learn/nand2tetris/projects/08/FunctionCalls/"
//    val fileName = "SimpleFunction"
    val fileName = "StaticsTest"
    val dir = File("$basePath/$fileName")
    val writer = CodeWriter(File("$basePath/$fileName/$fileName.asm"))
    writer.writeInit()
    if (dir.isDirectory) {
//        println(Arrays.toString(dir.list()))
        dir.listFiles { _, name -> name.endsWith(".vm") }.forEach {
            writeCode(it, writer)
        }
    }
    writer.close()
}

private fun writeCode(file: File, writer: CodeWriter) {
    val parser = Parser(file)
    writer.fileName = file.nameWithoutExtension
    while (parser.hasMoreCommand()) {
        parser.advance()
        val commandType = parser.commandType()
        writer.addComment(parser.command())
        if (commandType == CommandType.C_ARITHMETIC) {
            writer.writeArithmetic(parser.arg1())
        } else if (commandType == CommandType.C_PUSH || commandType == CommandType.C_POP) {
            writer.writePushPop(commandType, parser.arg1(), parser.arg2())
        } else if (commandType == CommandType.C_IF) {
            writer.writeIf(parser.arg1())
        } else if (commandType == CommandType.C_GOTO) {
            writer.writeGoto(parser.arg1())
        } else if (commandType == CommandType.C_LABEL) {
            writer.writeLabel(parser.arg1())
        } else if (commandType == CommandType.C_FUNCTION) {
            writer.writeFunction(parser.arg1(), parser.arg2())
        } else if (commandType == CommandType.C_RETURN) {
            writer.writeReturn()
        } else if (commandType == CommandType.C_CALL) {
            writer.writeCall(parser.arg1(), parser.arg2())
        }
    }
}