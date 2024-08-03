package lang

import error as err
import kotlin.collections.mutableListOf
import lang.Utils.ifTrue
import lang.model.*
import lang.model.TokenType.*

class Parser(
    private val tokens: List<Token>,
) {

  private var currentInd: Int = 0

  fun parse() = statements()

  private fun statements(): List<Stmt> {
    val statements = mutableListOf<Stmt?>()
    while (!isAtEnd()) {
      skipNewLines()
      if (isAtEnd()) break
      statements += declaration()
    }
    return statements.filterNotNull()
  }

  private fun declaration(): Stmt? {
    try {
      return when {
        match(LET) -> varDeclaration()
        match(FN) -> function()
        match(CLASS) -> classDeclaration()
        else -> statement()
      }
    } catch (_: ParseException) {
      synchronize()
      return null
    }
  }

  private fun classDeclaration(): Stmt {
    val className = consume("expect class name", IDENTIFIER)
    consume("expect '{' after class name", LEFT_BRACE)
    val methods = mutableListOf<Fn>()
    while(!check(RIGHT_BRACE) && !isAtEnd()) {
      methods += function() as Fn
    }
    consume("expect '}' after class name", RIGHT_BRACE)
    return ClassStmt(className, methods)
  }

  private fun ifStmt(): Stmt {
    val condition = expression()
    var elseBranch: List<Stmt>? = null
    consume("expect '{' after condition", LEFT_BRACE)
    val thenBranch = block()
    if (match(ELSE)) {
      consume("expect '{' after condition", LEFT_BRACE)
      elseBranch = block()
    }
    return If(condition, thenBranch, elseBranch)
  }

  private fun function(): Stmt {
    val name = consume("expect function name", IDENTIFIER)
    expect("expect '(' after function name", LEFT_PAREN)
    val params = mutableListOf<Token>()
    if (!check(RIGHT_PAREN)) {
      do {
        params += consume("expect param", IDENTIFIER)
      } while (match(COMMA))
    }
    expect("expect ')' after params", RIGHT_PAREN)
    expect("expect '{'", LEFT_BRACE)

    val funBody = block()
    return Fn(name, params, funBody)
  }

  private fun varDeclaration(): Stmt {
    val name = advance()
    var expr: Expr? = null
    if (match(EQ)) {
      expr = expression()
    }
    expect("Expect new line", LINE, EOF)
    return Variable(name, expr)
  }

  private fun statement(): Stmt =
      when {
        match(IF) -> ifStmt()
        match(FOR) -> forStmt()
        match(PRINT) -> {
          val expression = expression()
          val printStmt = Print(expression)
          expect("Expect new line", LINE, EOF)
          printStmt
        }
        match(LEFT_BRACE) -> Block(block())
        match(RETURN) -> returnStmt()
        else -> Expression(expression())
      }

  private fun returnStmt(): Stmt {
    val keyword = previous()
    var value: Expr? = null
    if (!check(LINE)) {
      value = expression()
    }
    consume("Expect LF after return", LINE)
    return ReturnStmt(keyword, value)
  }

  private fun forStmt(): Stmt {
    // initializer -> ; -> conditions -> ; -> increment -> {
    var initializer: Expr? = null

    if (!check(SEMICOLON)) {
      initializer = expression()
    }
    expect("expect ';' after for initializer", SEMICOLON)

    val condition: Expr = if (!check(SEMICOLON)) {
      expression()
    } else {
      Literal(true)
    }

    expect("expect ';' after for condition", SEMICOLON)

    var increment: Expr? = null
    if (!check(LEFT_BRACE)) {
      increment = expression()
    }

    expect("expect '{' after for", LEFT_BRACE)
    val body = block()

    return For(initializer, condition, increment, body)
  }

  private fun block(): List<Stmt> {
    val statements = mutableListOf<Stmt?>()
    while (!check(RIGHT_BRACE) && !isAtEnd()) {
      skipNewLines()
      if (check(RIGHT_BRACE)) break
      statements += declaration()
    }
    expect("Expect '}'", RIGHT_BRACE)
    return statements.filterNotNull()
  }

  private fun expression() = assignment()

  private fun assignment(): Expr {
    val expr = or()
    if (match(EQ)) {
      val eq = previous()
      val right = assignment()
      if (expr is Var) {
        return Assign(expr.token.text, right)
      } else {
        error("invalid assignment type", eq)
      }
    }
    return expr
  }

  private fun or(): Expr {
    val expr = and()
    return if (match(OR)) {
      val operator = previous()
      val right = and()
      Logical(expr, operator, right)
    } else expr
  }

  private fun and(): Expr {
    val expr = equality()
    return if (match(AND)) {
      val operator = previous()
      val right = equality()
      Logical(expr, operator, right)
    } else expr
  }

  private fun equality(): Expr {
    var expr = comparison()
    while (match(EQ_EQ, NOT_EQ)) {
      val operator = previous().type
      val right = comparison()
      expr = Binary(expr, operator, right)
    }
    return expr
  }

  private fun comparison(): Expr {
    var expr = term()
    while (match(LT, LTE, GT, GTE)) {
      val operator = previous().type
      val right = term()
      expr = Binary(expr, operator, right)
    }
    return expr
  }

  private fun term(): Expr {
    var expr = factor()
    while (match(PLUS, MINUS)) {
      val operator = previous().type
      val right = factor()
      expr = Binary(expr, operator, right)
    }
    return expr
  }

  private fun factor(): Expr {
    var expr = unary()
    while (match(ASTERISK, SLASH)) {
      val operator = previous().type
      val right = unary()
      expr = Binary(expr, operator, right)
    }
    return expr
  }

  private fun unary(): Expr =
      if (match(MINUS, BANG)) {
        val operator = previous()
        val right = unary()
        Unary(operator, right)
      } else call()

  private fun call(): Expr {
    var expr = primary()
    if (match(LEFT_PAREN)) {
      val arguments = mutableListOf<Expr>()
      do {
        if (!check(RIGHT_PAREN)) {
          arguments += expression()
        }
      } while (match(COMMA))
      expect("Expect ')' after arguments", RIGHT_PAREN)
      expr = Call(expr, arguments)
    }
    return expr
  }

  private fun primary(): Expr =
      when {
        match(STRING) -> Literal(previous().value)
        match(NUMBER) -> Literal(previous().text.toDouble())
        match(NIL) -> Literal(null)
        match(LEFT_PAREN) -> {
          val expr = expression()
          expect("Expect right paren", RIGHT_PAREN)
          Grouping(expr)
        }
        match(FALSE) -> Literal(false)
        match(TRUE) -> Literal(true)
        match(IDENTIFIER) -> Var(previous())
        else -> throw error("Compile error! ", tokens[currentInd])
      }

  private fun expect(message: String, vararg types: TokenType) {
    when (match(*types)) {
      false -> error(message, tokens[currentInd])
      else -> {}
    }
  }

  private fun error(message: String, token: Token): ParseException {
    err(token, message)
    return ParseException()
  }

  private fun consume(message: String, type: TokenType): Token {
    if (check(type)) return advance()
    throw error(message, peek())
  }

  private fun peek() = tokens[currentInd]

  private fun match(vararg types: TokenType): Boolean = types.any { check(it) }.ifTrue { advance() }

  private fun check(type: TokenType) = peek().type == type

  private fun previous() = tokens[currentInd - 1]

  private fun advance() = tokens[currentInd++]

  private fun isAtEnd() = currentInd >= tokens.size || tokens[currentInd].type == EOF

  private fun skipNewLines() {
    while (check(LINE)) advance()
  }

  private fun synchronize() {
    advance()
    while (!isAtEnd()) {
      if (previous().type == LINE) return
      when(peek().type) {
        FN, LET, IF, FOR, PRINT, RETURN -> return
        else -> {}
      }
      advance()
    }
  }
}

class ParseException : RuntimeException()
