package lang.runtime

import error
import lang.model.*
import lang.model.Grouping
import lang.model.Set

typealias Scope = ArrayDeque<MutableMap<String, Boolean>>

enum class ClassType {
  NONE, CLASS, SUBCLASS
}

class Resolver(
  private val interpreter: Interpreter,
  private var scopes: Scope = Scope(),
  private var currentFunction: FunctionType = FunctionType.NONE,
  private var currentClass: ClassType = ClassType.NONE,
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
    returnStmt.value?.let {
      if (currentFunction == FunctionType.INITIALIZER) {
        error(returnStmt.keyword, "Can't return from initializer")
      }
      resolve(it)
    }
  }

  override fun visitClassStmt(classStmt: ClassStmt) {
    val enclosingClass = currentClass
    currentClass = ClassType.CLASS

    define(classStmt.name)
    declare(classStmt.name)

    classStmt.superClass?.let {
      if (classStmt.name.text == it.token.text)
        error(classStmt.name, "Class can't inherit itself")
      addScope()
      scopes.lastOrNull()?.put("super", true)
      currentClass = ClassType.SUBCLASS
      resolve(it)
    }

    addScope()
    scopes.lastOrNull()?.put("self", true)
    for (method in classStmt.methods) {
      val declaration = if (method.name.text == "init") FunctionType.INITIALIZER else FunctionType.METHOD
      resolveFn(method, declaration)
    }
    removeScope()

    classStmt.superClass?.let { removeScope() }

    currentClass = enclosingClass
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

  override fun visitGetExpr(expr: Get) {
    resolve(expr.obj)
  }

  override fun visitSetExpr(expr: Set) {
    resolve(expr.obj)
    resolve(expr.value)
  }

  override fun visitSelfExpr(expr: Self) {
    if (currentClass == ClassType.NONE) error(expr.name, "can't use 'self' outside of a class")
    resolveLocal(expr, expr.name.text)
  }

  override fun visitSuperExpr(expr: Super) {
    if (currentClass == ClassType.NONE) error(expr.name, "Can't use super keyword outside class")
    else if (currentClass != ClassType.SUBCLASS) error(expr.name, "Can't user super keyword in a class with no superclass")
    resolveLocal(expr, expr.name.text)
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
  METHOD,
  INITIALIZER
}
