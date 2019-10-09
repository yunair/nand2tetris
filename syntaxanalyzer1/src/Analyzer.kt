import java.io.File

fun main() {
    val basePath = "/Users/air/CodeSpace/learn/nand2tetris/projects/10/"
//    val analyzer = Analyzer(File("${basePath}ArrayTest/Main.jack"))
//    val analyzer = Analyzer(File("${basePath}Square/Square.jack"))
//    val analyzer = Analyzer(File("${basePath}Square/SquareGame.jack"))
    val analyzer = Analyzer(File("${basePath}Square/Main.jack"))
    analyzer.start()
}

class Analyzer(private val jackFile: File) {
    private val outputFile: File = File(jackFile.parent + "/" + jackFile.nameWithoutExtension + "1.xml")
    private val tokenizer = Tokenizer(jackFile)
    private val engine = CompilationEngine(jackFile, outputFile)

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