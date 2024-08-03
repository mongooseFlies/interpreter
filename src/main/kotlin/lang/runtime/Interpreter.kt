package lang.runtime

import lang.model.*
import lang.model.TokenType.*
import runtimeErr

class Interpreter(
    private val globals: Environment = Environment(),
    private var environment: Environment = globals,
    private val locals: MutableMap<Expr, Int> = mutableMapOf()
) : Expr.Visitor, Stmt.Visitor {

  fun interpret(statements: List<Stmt>) = try {
    statements.forEach { it.visit(this) }
  } catch (err: RuntimeError) {
    runtimeErr(err)
  }

  private fun eval(expr: Expr) = expr.visit(this)

  override fun visitBinaryExpr(binary: Binary): Any {
    val left = eval(binary.left)
    val right = eval(binary.right)

    val operator = binary.operator
    return when (operator) {
      ASTERISK -> {
        assertNumbers(left, right)
        (left as Double) * (right as Double)
      }
      SLASH -> {
        assertNumbers(left, right)
        (left as Double) / (right as Double)
      }
      MINUS -> {
        assertNumbers(left, right)
        (left as Double) - (right as Double)
      }
      PLUS ->
          when {
            left is Double && right is Double -> left + right
            left is String && right is String -> "$left$right"
            left is String -> "$left$right"
            else -> RuntimeError("can add only strings or numbers")
          }
      GTE -> {
        assertNumbers(left, right)
        (left as Double) >= (right as Double)
      }
      GT -> {
        assertNumbers(left, right)
        (left as Double) > (right as Double)
      }
      LT -> {
        assertNumbers(left, right)
        (left as Double) < (right as Double)
      }
      LTE -> {
        assertNumbers(left, right)
        (left as Double) <= (right as Double)
      }
      EQ_EQ -> isEquals(left, right)
      NOT_EQ -> !isEquals(left, right)
      else -> {}
    }
  }

  private fun isEquals(left: Any?, right: Any?): Boolean {
    if (left == null && right == null) return true
    return left?.equals(right) ?: false
  }

  override fun visitUnaryExpr(unary: Unary): Any {
    val expression = eval(unary.right)
    return when (unary.operator.type) {
      BANG -> isTruthy(expression)
      MINUS -> {
        if (expression is Double) return -1 * expression
        else throw RuntimeError("Expect a number after ${unary.operator.type}")
      }
      else -> {}
    }
  }

  override fun visitGroupingExpr(grouping: Grouping): Any? = eval(grouping.expr)

  override fun visitLiteralExpr(literal: Literal): Any? = literal.value

  override fun visitVarExpr(expr: Var) = lookUpVar(expr.token, expr)

  private fun lookUpVar(token: Token, expr: Expr): Any? {
    val depth = locals[expr]
    return depth?.let {
      environment.getAt(depth, token.text)
    } ?: globals.get(token.text)
  }

  override fun visitCallExpr(expr: Call): Any? {
    val callee = eval(expr.callee)
    if (callee !is Callable) {
      throw RuntimeError("expr is not callable")
    }
    val arguments = mutableListOf<Any?>()
    for (argument in expr.arguments) arguments += eval(argument)
    return callee.call(this, arguments)
  }

  override fun visitAssignExpr(expr: Assign): Any? {
    val value = eval(expr.value)
    val depth = locals[expr]
    depth?.let {
      environment.assignAt(depth, expr.name, value)
    } ?: globals.assign(expr.name, value)
    return value
  }

  override fun visitLogicalExpr(expr: Logical): Any? {
    val left = eval(expr.left)

    if (expr.operator.type == OR) {
      if (isTruthy(left)) return left
    } else {
      if (!isTruthy(left)) return left
    }
    return eval(expr.right)
  }

  private fun assertNumbers(left: Any?, right: Any?) {
    if (left !is Double || right !is Double) throw RuntimeError("Expect number")
  }

  private fun isTruthy(obj: Any?) =
      when (obj) {
        is Boolean -> obj
        null -> false
        else -> true
      }

  override fun visitExpressionStmt(stmt: Expression) = eval(stmt.expr)

  override fun visitPrintStmt(stmt: Print) {
    when(val expr = eval(stmt.expr)) {
      null -> println("nil")
      else -> println(expr)
    }
  }

  override fun visitVarStmt(stmt: Variable) {
    var initializer: Any? = null
    if (stmt.initializer != null) {
      initializer = eval(stmt.initializer)
    }
    environment.define(stmt.name.text, initializer)
  }

  override fun visitBlockStmt(block: Block) {
    executeBlock(block.statements, Environment(enclosing = environment))
  }

  fun executeBlock(body: List<Stmt>, environment: Environment) {
    val previous = this.environment
    this.environment = environment
    try {
      for (stmt in body) stmt.visit(this)
    } finally {
      this.environment = previous
    }
  }

  override fun visitFnStmt(fn: Fn) {
    val function = Function(fn, environment)
    environment.define(fn.name.text, function)
  }

  override fun visitIfStmt(ifStmt: If) {
    val condition = eval(ifStmt.condition)
    if (isTruthy(condition)) {
      executeBlock(ifStmt.then, environment)
    } else if (ifStmt.elseBranch != null) executeBlock(ifStmt.elseBranch, environment)
  }

  override fun visitForStmt(forStmt: For) {
    if (forStmt.initializer != null) {
      if (forStmt.initializer !is Assign) {
        throw RuntimeError("invalid assign stmt in for-loop")
      }
      val initializer = forStmt.initializer
      environment.define(initializer.name, eval(initializer.value))
    }

    while (isTruthy(eval(forStmt.condition))) {
      executeBlock(forStmt.body, environment)
      forStmt.increment?.let { eval(it) }
    }
  }

  override fun visitReturnStmt(returnStmt: ReturnStmt): Any? {
    val value = returnStmt.value?.let {
      eval(it)
    }
    throw Return(value)
  }

  override fun visitClassStmt(classStmt: ClassStmt): Any {
    environment.define(classStmt.name.text, null)
    val methods = mutableMapOf<String, Function>()
    for (method in classStmt.methods) {
      val func = Function(method, environment)
      methods[method.name.text] = func
    }
    return Class(classStmt.name.text, methods).also {
      environment.assign(classStmt.name.text, it)
    }
  }

  fun resolve(expr: Expr, depth: Int) {
    locals[expr] = depth
  }
}

data class RuntimeError(override val message: String, val token: Token? = null) : RuntimeException(message)
data class Return(val value: Any?): RuntimeException(null, null, false, false)