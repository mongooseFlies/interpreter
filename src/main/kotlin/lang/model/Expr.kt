package lang.model

interface Expr {
    interface Visitor {
        fun visitBinaryExpr(binary: Binary): Any?

        fun visitUnaryExpr(unary: Unary): Any?

        fun visitGroupingExpr(grouping: Grouping): Any?

        fun visitLiteralExpr(literal: Literal): Any?

        fun visitVarExpr(expr: Var): Any?

        fun visitCallExpr(expr: Call): Any?

        fun visitAssignExpr(expr: Assign): Any?

        fun visitLogicalExpr(expr: Logical): Any?
    }

    fun visit(visitor: Visitor): Any?
}

data class Binary(
    val left: Expr,
    val operator: TokenType,
    val right: Expr,
) : Expr {
    override fun visit(visitor: Expr.Visitor) = visitor.visitBinaryExpr(this)
}

data class Unary(
    val operator: Token,
    val right: Expr,
) : Expr {
    override fun visit(visitor: Expr.Visitor) = visitor.visitUnaryExpr(this)
}

data class Literal(
    val value: Any?,
) : Expr {
    override fun visit(visitor: Expr.Visitor) = visitor.visitLiteralExpr(this)
}

data class Grouping(
    val expr: Expr,
) : Expr {
    override fun visit(visitor: Expr.Visitor) = visitor.visitGroupingExpr(this)
}

data class Var(
    val token: Token,
) : Expr {
    override fun visit(visitor: Expr.Visitor) = visitor.visitVarExpr(this)
}

data class Assign(
    val name: String,
    val value: Expr
) : Expr {
    override fun visit(visitor: Expr.Visitor) =
        visitor.visitAssignExpr(this)

}

data class Call(
    val callee: Expr,
    val arguments: MutableList<Expr> = mutableListOf(),
) : Expr {
    override fun visit(visitor: Expr.Visitor) = visitor.visitCallExpr(this)
}

data class Logical(
    val left: Expr,
    val operator: Token,
    val right: Expr
) : Expr {
    override fun visit(visitor: Expr.Visitor) = visitor.visitLogicalExpr(this)
}