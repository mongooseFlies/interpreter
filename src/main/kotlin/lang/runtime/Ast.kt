package lang.runtime

import kotlin.text.buildString
import lang.model.*

class Ast : Expr.Visitor, Stmt.Visitor {
  override fun visitBinaryExpr(binary: Binary): String {
    val left = binary.left.visit(this)
    val right = binary.right.visit(this)
    return buildString { append("(${binary.operator} $left $right)") }
  }

  override fun visitUnaryExpr(unary: Unary): String {
    val right = unary.right.visit(this)
    return buildString { append("(${unary.operator.type} $right)") }
  }

  override fun visitGroupingExpr(grouping: Grouping): String {
    val expr = grouping.expr.visit(this)
    return buildString { append("(GROUP $expr)") }
  }

  override fun visitLiteralExpr(literal: Literal): String = buildString { append(literal.value) }

  override fun visitVarExpr(expr: Var) = buildString { append(expr.token.text) }

  override fun visitCallExpr(expr: Call) = buildString {
    append("(CALL ${expr.callee.visit(this@Ast)} ")
    if (expr.arguments.isNotEmpty()) {
      append("(PARAMS (")
      val argumentsLen = expr.arguments.size
      for (index in expr.arguments.indices) {
        append("${expr.arguments[index]}")
        if (index < argumentsLen - 1)
          append(", ")
        else
          append(")")
      }
      append(")")
    }
    append(")")
  }

  override fun visitAssignExpr(expr: Assign) = buildString {
    append("(ASSIGN (${expr.name} -> ${expr.value.visit(this@Ast)} )")
  }

  override fun visitLogicalExpr(expr: Logical) = buildString {
    append("(${expr.operator.text} -> ")
    append("(${expr.left.visit(this@Ast)} ")
    append("(${expr.right.visit(this@Ast)}) ")
  }


  override fun visitExpressionStmt(stmt: Expression) {
    stmt.expr.visit(this)
  }

  override fun visitPrintStmt(stmt: Print) = buildString {
    append("(PRINT ${stmt.expr.visit(this@Ast)})")
  }

  override fun visitVarStmt(stmt: Variable) = buildString {
    append("(VAR ${stmt.name.text} ${stmt.initializer})})")
  }

  override fun visitBlockStmt(block: Block) = buildString {
    append(("(BLOCK "))
    for (stmt in block.statements) append(stmt.visit(this@Ast))
    append(")")
  }

  override fun visitFnStmt(fn: Fn) = buildString {
    append("(FN -> ${fn.name.text}(PARAMS ")
    for (index in fn.params.indices) {
      append(fn.params[index].text)
      if (index < fn.params.size - 1)
        append(", ")
      else
        append(")")
    }
    append(")")
    for (stmt in fn.body) stmt.visit(this@Ast)
    append(")")
  }

  override fun visitIfStmt(ifStmt: If) = buildString {
    append("(IF -> ${ifStmt.condition.visit(this@Ast)} -> (")
    for (stmt in ifStmt.then)
      stmt.visit(this@Ast)
    append(") ")
    if (ifStmt.elseBranch != null) {
      append("ELSE (")
      for (stmt in ifStmt.elseBranch)
        stmt.visit(this@Ast)
      append(")")
    }
  }

  override fun visitForStmt(forStmt: For) = buildString {
    append("(FOR -> ${forStmt.condition.visit(this@Ast)})")
  }

  override fun visitReturnStmt(returnStmt: ReturnStmt) = buildString {
    append("(RETURN ")
    returnStmt.value?.let { append(it.visit(this@Ast)) }
    append(")")
  }

  override fun visitClassStmt(classStmt: ClassStmt): Any? = buildString {
    append("(Class -> ${classStmt.name}")
    append("(METHODS ==> (")
    for (method in classStmt.methods)
      append("${method.name} ")
    append("))")
  }
}
