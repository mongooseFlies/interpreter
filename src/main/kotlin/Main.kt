import lang.Lexer
import lang.Parser
import lang.model.Token
import lang.model.TokenType
import lang.runtime.Ast
import lang.runtime.Interpreter
import lang.runtime.Resolver
import lang.runtime.RuntimeError
import java.io.File

var hadError = false
var hadRuntimeErr = false

fun main(args: Array<String>) {
    when (args.size) {
        0 -> repl()
        else -> source(args[0])
    }
}

private fun source(filename: String) {
    val file = File(filename)
    val source = file.readText()
    run(source)
}

fun repl() {
    val interpreter = Interpreter()
    val resolver = Resolver(interpreter)
    while (true) {
        print("-> ")
        val userStmt = readlnOrNull() ?: break
        runLine(userStmt, interpreter, resolver)
    }
}

private fun runLine(
    line: String,
    interpreter: Interpreter,
    resolver: Resolver,
) {
    val lexer = Lexer(line)
    val tokens = lexer.tokens()
    val parser = Parser(tokens)
    val statements = parser.parse()
    statements.forEach {
        it.visit(Ast())
    }
    resolver.resolve(statements)
    interpreter.interpret(statements)
}

fun run(source: String) {
    val lexer = Lexer(source)
    val tokens = lexer.tokens()

    val parser = Parser(tokens)
    val statements = parser.parse()

    if (!hadError) {
        // val ast = Ast()
        // statements.forEach { println(it.visit(ast)) }
        val interpreter = Interpreter()
        val resolver = Resolver(interpreter)
        resolver.resolve(statements)
        if (hadError) return
        interpreter.interpret(statements)
    }
}

fun error(
    line: Int,
    message: String,
) = report(line, "", message)

fun error(
    token: Token,
    message: String,
) {
    if (token.type === TokenType.EOF) {
        report(token.line, " at end", message)
    } else {
        report(token.line, " at ${token.text}", message)
    }
}

private fun report(
    line: Int,
    where: String,
    message: String,
) {
    System.err.println(
        "[line $line] Error$where: $message",
    )
    hadError = true
}

fun runtimeErr(error: RuntimeError) {
    println(error.message)
    error.token?.let { println("[line ${error.token.line}]") }
    hadRuntimeErr = true
}

