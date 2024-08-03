package lang

import error
import lang.model.Token
import lang.model.TokenType
import lang.model.TokenType.*

data class Lexer(private val source: String) {
  private val tokens = mutableListOf<Token>()
  private var startInd = 0
  private var currentInd = 0
  private var line = 0
  private val keywords =
      mapOf(
          "if" to IF,
          "else" to ELSE,
          "for" to FOR,
          "true" to TRUE,
          "false" to FALSE,
          "let" to LET,
          "nil" to NIL,
          "print" to PRINT,
          "fn" to FN,
          "return" to RETURN,
          "and" to AND,
          "or" to OR,
          "class" to CLASS
      )

  fun tokens(): List<Token> {
    while (!isAtEnd()) {
      startInd = currentInd
      processToken()
    }
    tokens.add(Token(type = EOF, text = "", value = null, line))
    return tokens
  }

  private fun processToken() {
    when (advance()) {
      '(' -> addToken(LEFT_PAREN)
      ')' -> addToken(RIGHT_PAREN)
      '{' -> addToken(LEFT_BRACE)
      '}' -> addToken(RIGHT_BRACE)
      '+' -> addToken(PLUS)
      '-' -> addToken(MINUS)
      '/' -> if (match('/')) while (!isAtEnd() && peek() != '\n') advance() else addToken(SLASH)
      '*' -> addToken(ASTERISK)
      '"' -> string()
      '<' -> {
        val type = if (match('=')) LTE else LT
        addToken(type)
      }
      '>' -> {
        val type = if (match('=')) GTE else GT
        addToken(type)
      }
      '=' -> {
        val type = if (match('=')) EQ_EQ else EQ
        addToken(type)
      }
      '!' -> {
        val type = if (match('=')) NOT_EQ else BANG
        addToken(type)
      }
      ',' -> addToken(COMMA)
      ';' -> addToken(SEMICOLON)
      '\n', '\r' -> {
        //        if (tokens.isNotEmpty() && tokens[currentInd - 1].type != LINE)
        addToken(LINE)
        line++
      }
      '\t', ' ' -> {}
      in '0'..'9' -> number()
      in 'a'..'z', in 'A'..'Z' -> identifier()
    // Char.MIN_VALUE -> addToken(EOF)
    }
  }

  private fun identifier() {
    while (isAlphaNumeric(peek())) advance()
    val text = source.slice(startInd ..< currentInd)
    val keyword = keywords[text]
    val type = keyword ?: IDENTIFIER
    addToken(type)
  }

  private fun match(expected: Char): Boolean {
    if (isAtEnd()) return false
    val char = peek()
    return when (char) {
      expected -> {
        advance()
        true
      }
      else -> false
    }
  }

  private fun isAlphaNumeric(char: Char) = isAlpha(char) || isDigit(char)

  private fun isAlpha(char: Char) = char in 'a'..'z' || char in 'A'..'Z'

  private fun isDigit(char: Char) = char in '0'..'9'

  private fun string() {

    while (peek() != '"' && !isAtEnd()) {
      if (peek() == '\n') line++
      advance()
    }

    if (isAtEnd()) {
      error(line, "unterminated string")
      return
    }

    // consume "
    advance()

    val text = source.substring(startInd + 1, currentInd - 1)
    addToken(STRING, text)
  }

  private fun peek() =
      when {
        isAtEnd() -> Char.MIN_VALUE
        else -> source[currentInd]
      }

  private fun number() {
    while (isDigit(peek())) advance()
    // NOTE: handle double values
    if (match('.')) while (isDigit(peek())) advance()
    val value = source.slice(startInd ..< currentInd)
    addToken(NUMBER, value)
  }

  private fun addToken(type: TokenType) {
    val text = source.slice(startInd ..< currentInd)
    val token = Token(type, text, null, line)
    tokens += token
  }

  private fun addToken(
      type: TokenType,
      value: Any?,
  ) {
    val text = source.slice(startInd ..< currentInd)
    val token = Token(type, text, value, line)
    tokens += token
  }

  private fun advance(): Char = source[currentInd++]

  private fun isAtEnd() = currentInd >= source.length
}
