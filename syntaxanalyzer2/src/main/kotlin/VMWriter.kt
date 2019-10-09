import java.io.File

enum class Segment(val value: String) {
    CONST("constant"), ARG("argument"), LOCAL("local"), STATIC("static"),
    THIS("this"), THAT("that"), POINTER("pointer"), TEMP("temp");

    companion object {
        fun get(value: String): Segment {
            if (value == "field") {
                return THIS
            }
            return values().find {
                it.value == value
            }!!
        }
    }
}

enum class Command {
    ADD, SUB, NEG, EQ, GT, LT, AND, OR, NOT;

    fun value(): String {
        return this.name.toLowerCase()
    }
}


class VMWriter(private val outputFile: File) {
    init {
        outputFile.delete()
    }

    fun writePush(segment: Segment, index: Int) {
        write("push " + segment.value + " " + index)
    }

    fun writePop(segment: Segment, index: Int) {
        write("pop " + segment.value + " " + index)
    }

    fun writeArithmetic(command: Command) {
        write(command.value())
    }

    fun writeLabel(label: String) {
        write("label $label")
    }

    fun writeGoto(label: String) {
        write("goto $label")
    }

    fun writeIf(label: String) {
        write("if-goto $label")
    }

    fun writeCall(name: String, nArgs: Int) {
        write("call $name $nArgs")
    }

    fun writeFunction(name: String, nArgs: Int) {
        write("function $name $nArgs")
    }

    fun writeReturn() {
        write("return")
    }

    fun close() {

    }

    private fun write(text: String) {
        outputFile.appendText(text + "\n")
    }
}