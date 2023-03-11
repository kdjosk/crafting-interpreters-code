package jlox;

import java.util.List;
import static jlox.TokenType.*;

class Parser {
  private static class ParseError extends RuntimeException {}
  
  private final List<Token> tokens;
  private int current = 0;
  
  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }
  
  Expr parse() {
    try {
      return expression();
    } catch (ParseError error) {
      return null;
    }
  }
  
  // expression -> errExpressionNoLeftOperand | ternary ( "," ternary )* ;
  private Expr expression() {
    if (check(COMMA)) {
      error(peek(), "Expected expression before ','");
      errExpressionNoLeftOperand();
    }
    
    Expr expr = ternary();
    
    while(match(COMMA)) {
      Token operator = previous();
      Expr right = ternary();
      expr = new Expr.Binary(expr, operator, right);
    }
    
    return expr;
  }
  
  //errExpressionNoLeftOperand -> ( "," ternary )* ;
  private void errExpressionNoLeftOperand() {
    while (match(COMMA)) {
      ternary();
    }
  }
  
  //  ternary -> equality ( "?" equality ":" ternary )? ;
  private Expr ternary() {
    Expr expr = equality();
    if (match(QUESTION_MARK)) {
      Expr ifTrue = equality();
      consume(COLON, "Expected ':' for ternary operator");
      Expr ifFalse = ternary();
      
      return new Expr.Ternary(expr, ifTrue, ifFalse);
    }
    
    return expr;
    
  }
  
  //  equality -> errEqualityNoLeftOperand | comparison ( ( "!=" | "==" ) comparison )*;
  private Expr equality() {
    if (check(BANG_EQUAL, EQUAL_EQUAL)) {
      error(peek(), "Expected expression before '" + peek().lexeme +"'");
      errEqualityNoLeftOperand();
    }
    
    Expr expr = comparison();
    
    while(match(BANG_EQUAL, EQUAL_EQUAL)) {
      Token operator = previous();
      Expr right = comparison();
      expr = new Expr.Binary(expr, operator, right);
    }
    
    return expr;
  }
  
  // errEqualityNoLeftOperand -> ( ( "!=" | "==" ) comparison )* ;
  private void errEqualityNoLeftOperand() {
    while(match(BANG_EQUAL, EQUAL_EQUAL)) {
      comparison();
    }
  }
  
  // comparison -> term ( ( ">" | ">=" | "<" | "<=") term )* ;
  private Expr comparison() {
    Expr expr = term();
    
    while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      Token operator = previous();
      Expr right = term();
      expr = new Expr.Binary(expr, operator, right);
    }
    
    return expr;
  }
  
  // term -> factor ( ( "-" | "+" ) factor )* ;
  private Expr term() {
    Expr expr = factor();
    
    while (match(PLUS, MINUS)) {
      Token operator = previous();
      Expr right = factor();
      expr = new Expr.Binary(expr, operator, right);
    }
    
    return expr;
  }
  
  // factor -> unary ( ( "/" | "*" ) unary )* ;
  private Expr factor() {
    Expr expr = unary();
    
    while (match(SLASH, STAR)) {
      Token operator = previous();
      Expr right = unary();
      expr = new Expr.Binary(expr, operator, right);
    }
    
    return expr;
  }
  
  // unary -> ( "!" | "-" ) unary | primary ;
  private Expr unary() {
    if (match(BANG, MINUS)) {
      Token operator = previous();
      Expr right = unary();
      return new Expr.Unary(operator, right);
    }
    
    return primary();
  }
  
  // primary -> NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" ;
  private Expr primary() {
    if (match(FALSE)) return new Expr.Literal(false);
    if (match(TRUE)) return new Expr.Literal(true);
    if (match(NIL)) return new Expr.Literal(null);
    
    if (match(NUMBER, STRING)) {
      return new Expr.Literal(previous().literal);
    }
    
    if (match(LEFT_PAREN)) {
      Expr expr = expression();
      consume(RIGHT_PAREN, "Expect ')' after expression.");
      return new Expr.Grouping(expr);
    }
    
    throw error(peek(), "Expect expression.");
  }
  
  private boolean match(TokenType... types) {
    for (TokenType type : types) {
      if (check(type)) {
        advance();
        return true;
      }
    }
    
    return false;
  }
  
  private Token consume(TokenType type, String message) {
    if (check(type)) return advance();
    
    throw error(peek(), message);
  }
  
  private boolean check(TokenType... types) {
    if (isAtEnd()) return false;
    for (TokenType type : types) {
      if (peek().type == type) {
        return true;
      }
    }
    return false;
  }
  
  private Token advance() {
    if (!isAtEnd()) current++;
    return previous();
  }
  
  private boolean isAtEnd() {
    return peek().type == EOF;
  }
  
  private Token peek() {
    return tokens.get(current);
  }
  
  private Token previous() {
    return tokens.get(current - 1);
  }
  
  private ParseError error(Token token, String message) {
    Lox.error(token, message);
    return new ParseError();
  }
  
  private void synchronize() {
    advance();
    
    while (!isAtEnd()) {
      if (previous().type == SEMICOLON) return;
      
      switch (peek().type) {
        case CLASS:
        case FUN:
        case VAR:
        case FOR:
        case IF:
        case WHILE:
        case PRINT:
        case RETURN:
          return;
      }
      
      advance();
    }
  }
}
