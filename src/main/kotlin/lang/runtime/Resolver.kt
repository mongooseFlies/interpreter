package lang.runtime

import error
import lang.model.*
import lang.model.Grouping

typealias Scope = ArrayDeque<MutableMap<String, Boolean>>

class Resolver(
  private val interpreter: Interpreter,
  private var scopes: Scope = Scope(),
  private var currentFunction: FunctionType = FunctionType.NONE
) : Stmt.Visitor, Expr.Visitor {

  fun resolve(statements: List<Stmt>) = statements.forEach { resolve(it) }

  private fun resolve(stmt: Stmt) = stmt.visit(this)

  override fun visitExpressionStmt(stmt: Expression) = resolve(stmt.expr)

  private fun resolve(expr: Expr) = expr.visit(this)

  override fun visitPrintStmt(stmt: Print) {
    resolve(stmt.expr)
  }

  override fun visitVarStmt(stmt: Variable) {
    declare(stmt.name)
    stmt.initializer?.let { resolve(it) }
    define(stmt.name)
  }

  override fun visitBlockStmt(block: Block) {
    addScope()
    resolve(block.statements)
    removeScope()
  }

  override fun visitFnStmt(fn: Fn) {
    declare(fn.name)
    define(fn.name)
    resolveFn(fn, FunctionType.FUNCTION)
  }

  private fun resolveFn(fn: Fn, type: FunctionType) {
    val enclosingFun = currentFunction
    currentFunction = type
    addScope()
    for (param in fn.params) {
      declare(param)
      define(param)
    }
    resolve(fn.body)
    removeScope()
    currentFunction = enclosingFun
  }

  private fun removeScope() {
    scopes.removeLast()
  }

  private fun addScope() {
    scopes.addLast(mutableMapOf())
  }

  override fun visitIfStmt(ifStmt: If) {
    resolve(ifStmt.condition)
    resolve(ifStmt.then)
    ifStmt.elseBranch?.let { resolve(it) }
  }

  override fun visitForStmt(forStmt: For) {
    forStmt.initializer?.let { resolve(it) }
    resolve(forStmt.condition)
    forStmt.increment?.let { resolve(it) }
    resolve(forStmt.body)
  }

  override fun visitReturnStmt(returnStmt: ReturnStmt) {
    if (currentFunction == FunctionType.NONE) {
      error(returnStmt.keyword, "Can't return from top-level code")
    }
    returnStmt.value?.let {  resolve(it) }
  }

  override fun visitClassStmt(classStmt: ClassStmt) {
    define(classStmt.name)
    declare(classStmt.name)

    addScope()
    for (method in classStmt.methods) {
      resolveFn(method, FunctionType.METHOD)
    }
    removeScope()
  }

  override fun visitBinaryExpr(binary: Binary) {
    resolve(binary.left)
    resolve(binary.right)
  }

  override fun visitUnaryExpr(unary: Unary) {
    resolve(unary.right)
  }

  override fun visitGroupingExpr(grouping: Grouping) {
    resolve(grouping.expr)
  }

  override fun visitLiteralExpr(literal: Literal) {}

  override fun visitVarExpr(expr: Var) {
    if (scopes.isNotEmpty() && scopes.lastOrNull()?.get(expr.token.text) == false)
        error(expr.token, "Can't read local variable in its own initializer")
    resolveLocal(expr, expr.token.text)
  }

  private fun resolveLocal(expr: Expr, name: String) {
    for (i in scopes.indices.reversed()) {
      if (scopes[i].containsKey(name)) {
        interpreter.resolve(expr, scopes.size - 1 - i)
        return
      }
    }
  }

  override fun visitCallExpr(expr: Call) {
    resolve(expr.callee)
    expr.arguments.forEach { resolve(it) }
  }

  override fun visitAssignExpr(expr: Assign) {
    resolve(expr.value)
    resolveLocal(expr, expr.name)
  }

  override fun visitLogicalExpr(expr: Logical) {
    resolve(expr.left)
    resolve(expr.right)
  }

  private fun declare(name: Token) {
    val scope = scopes.lastOrNull() ?: return
    if (scope.containsKey(name.text)) error(name, "Variable Already declared")
    scope[name.text] = false
  }

  private fun define(name: Token) {
    scopes.lastOrNull()?.put(name.text, true) ?: return
  }
}

enum class FunctionType {
  NONE,
  FUNCTION,
  METHOD
}
