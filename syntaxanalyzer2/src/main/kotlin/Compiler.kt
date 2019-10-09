
import java.io.File

fun main() {
    val basePath = "/Users/air/CodeSpace/learn/nand2tetris/projects/11/"
    val entrance = File("${basePath}ComplexArrays")
//    val entrance = File("${basePath}Average")
    if (entrance.isDirectory) {
        entrance.listFiles { _, name ->
            name.endsWith(".jack")
        }?.forEach {
            println(it)
            val analyzer = Compiler(it)
            analyzer.start()
        }
    } else {
        val analyzer = Compiler(entrance)
//    val analyzer = Analyzer(File("${basePath}Square/Square.jack"))
//    val analyzer = Analyzer(File("${basePath}Square/SquareGame.jack"))
//    val analyzer = Compiler(File("${basePath}Square/Main.jack"))
        analyzer.start()
    }

}

class Compiler(jackFile: File) {
    private val outputFile: File = File(jackFile.parent + "/" + jackFile.nameWithoutExtension + ".vm")
    private val writer = VMWriter(outputFile)
    private val engine = CompilationEngine(jackFile, writer)
    fun log() {
//        println(tokenizer.commands())
    }

    fun start() {
        /* tokenizer.writeStart()
         while (tokenizer.hasMoreTokens()) {
             tokenizer.advance()
             tokenizer.writeType()
         }
         tokenizer.writeEnd()*/
//        tokenizer.writeXml()
        outputFile.delete()
        engine.start()
    }
}