package jlox;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import static jlox.TokenType.*;

class Parser {
  private static class ParseError extends RuntimeException {}
  
  private final List<Token> tokens;
  private int current = 0;
  
  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }
  
  // program -> (expression | declaration) EOF ;
  Object parseFromInteractive() {
    if (check(VAR, PRINT, LEFT_BRACE)) {
      Stmt stmt = declaration(false, false);
      if (!isAtEnd()) {
        error(peek(), "Single statement expected in interactive mode");
      }
      return stmt;
    }
    
    Expr expression = expression();
    
    if (match(SEMICOLON)) {
      Stmt stmt = new Stmt.Expression(expression);
      if (!isAtEnd()) {
        error(peek(), "Single statement expected in interactive mode");
      }
      return stmt;
    }
    
    if (!isAtEnd()) {
      error(peek(), "Single expression expected in interactive mode");
    }
     
    return expression;
  }
  
  // program -> declaration* EOF ;
  List<Stmt> parseFromFile() {
    List<Stmt> statements = new ArrayList<>();
    while (!isAtEnd()) {
      statements.add(declaration(false, false));
    }
    return statements;
  }
  
  // declaration -> varDecl | funDecl | statement ;
  // varDecl -> "var" IDENTIFIER ( "=" expression )? ";" ;
  private Stmt declaration(boolean insideLoop, boolean insideFun) {
    try {
      if (match(FUN)) return function("function");
      if (match(VAR)) return varDeclaration();
      return statement(insideLoop, insideFun);
    } catch (ParseError error) { 
      synchronize();
      return null;
    }
  }
  
  //  funDecl -> "fun" function ;
  //  function -> IDENTIFIER "(" parameters? ")" block ; 
  //  parameters -> IDENTIFIER ( "," IDENTIFIER )* ;
  private Stmt.Function function(String kind) {
    Token name = consume(IDENTIFIER, "Expected " + kind + " name.");
    consume(LEFT_PAREN, "Expected '(' after " + kind + " name.");
    List<Token> parameters = new ArrayList<>();
    if (!check(RIGHT_PAREN)) {
      do {
        if (parameters.size() >= 255) {
          error(peek(), "Can't have more than 255 parameters");
        }
        
        parameters.add(
            consume(IDENTIFIER, "Expect parameter name."));
      } while (match(COMMA));
    }
    
    consume(RIGHT_PAREN, "Expect ')' after parameters");
    
    consume(LEFT_BRACE, "Expected '{' before " + kind + " body.");
    List<Stmt> body = block(false, true);
    return new Stmt.Function(name,  parameters, body);
  }
  
  private Stmt varDeclaration() {
    Token name = consume(IDENTIFIER, "Expected variable name.");
    
    Expr initializer = null;
    if (match(EQUAL)) {
      initializer = expression();
    }
    
    consume(SEMICOLON, "Expected ';' after variable declaration.");
    return new Stmt.Var(name, initializer);
  }
  
  //  statement -> exprStmt | ifStmt | printStmt | whileStmt | jumpStmt | block ;
  //  exprStmt -> expression ";" ;
  //  ifStmt -> "if" "(" expression ")" statement ( "else" statement )? ;
  //  printStmt -> "print" expression ";" ;
  //  whileStmt -> "while" "(" expression ")" statement ;
  //  forStmt -> "for" "(" ( varDecl | exprStmt | ";") expression? ";" expression? ")" statement ;
  //  jumpStmt -> breakStmt | continueStmt ;
  //  breakStmt -> "break" ";" ;
  //  continueStmt -> "continue" ";" ;
  //  block -> "{" declaration* "}" ;
  private Stmt statement(boolean insideLoop, boolean insideFun) {
    if (match(IF)) return ifStatement(insideLoop, insideFun);
    if (match(PRINT)) return printStatement();
    if (match(WHILE)) return whileStatement(insideFun);
    if (match(FOR)) return forStatement(insideFun);
    if (match(LEFT_BRACE)) return new Stmt.Block(block(insideLoop, insideFun));
    
    if (match(CONTINUE, BREAK)) {
      if (insideLoop) return jumpStatement();
      throw error(previous(), "Jump statements are not allowed outside of loop body.");
    }
    
    if (match(RETURN)) {
      if (insideFun) return jumpStatement();
      throw error(previous(), "Return statements are not allowed outside of function body.");
    }
    
    return expressionStatement();
  }
  
  private Stmt printStatement() {
    Expr value = expression();
    consume(SEMICOLON, "Expected ';' after expression");
    return new Stmt.Print(value);
  }
  
  private Stmt expressionStatement() {
    Expr expr = expression();
    consume(SEMICOLON, "Expected ';' after expression");
    return new Stmt.Expression(expr);
  }
  
  private Stmt jumpStatement() {
    Token keyword = previous();
    Expr value = null;
    if (keyword.type == RETURN) {
      if (!check(SEMICOLON)) {
        value = expression();
      }
    }
    
    consume(SEMICOLON, "Expected ';' after " + keyword.lexeme + " statement");
    return new Stmt.Jump(keyword, value);
  }
  
  private Stmt forStatement(boolean insideFun) {
    consume(LEFT_PAREN, "Expected '(' after 'for'.");
    
    Stmt initializer;
    if (match(SEMICOLON)) {
      initializer = null;
    } else if (match(VAR)) {
      initializer = varDeclaration();
    } else {
      initializer = expressionStatement();
    }
    
    Expr condition = null;
    if (!check(SEMICOLON) ) {
      condition = expression();
    }
    consume(SEMICOLON, "Expected ';' after for-loop condition.");
    
    Expr increment = null;
    if (!check(RIGHT_PAREN)) {
      increment = expression();
    }
    consume(RIGHT_PAREN, "Expected ')' after increment expression");
    
    Stmt body = statement(true, insideFun);
    Stmt loop = new Stmt.For(condition, body, increment);
    
    if (initializer != null) {
      loop = new Stmt.Block(Arrays.asList(initializer, loop));
    }
    
    return loop;
    
  }
  
  private Stmt whileStatement(boolean insideFun) {
    consume(LEFT_PAREN, "Expected '(' after 'while'.");
    Expr condition = expression();
    consume(RIGHT_PAREN, "Expected ')' after condition.");
    Stmt body = statement(true, insideFun);
    
    return new Stmt.While(condition, body);
  }
  
  private Stmt ifStatement(boolean insideLoop, boolean insideFun) {
    consume(LEFT_PAREN, "Expected '(' after 'if'.");
    Expr condition = expression();
    consume(RIGHT_PAREN, "Expect ')' after if condition.");
    
    Stmt thenBranch = statement(insideLoop, insideFun);
    Stmt elseBranch = null;
    if (match(ELSE)) {
      elseBranch = statement(insideLoop, insideFun);
    }
    
    return new Stmt.If(condition, thenBranch, elseBranch);
  }
  
  private List<Stmt> block(boolean insideLoop, boolean insideFun) {
    List<Stmt> statements = new ArrayList<>();
    
    while (!check(RIGHT_BRACE) && !isAtEnd()) {
      statements.add(declaration(insideLoop, insideFun));
    }
    
    consume(RIGHT_BRACE, "Expected '}' after block.");
    return statements;
  }
  
  //  expression -> assignment ( "," assignment )* | errExprNoLeftOperand;
  //  errExprNoLeftOperand -> ( "," assignment )* ;
  private Expr expression() {
    if (check(COMMA)) {
      error(peek(), "Expected expression before ','");
      while (match(COMMA)) {
        assignment();
      }
      return new Expr.Erroneous(ExprErrType.NO_LEFT_OPERAND_FOR_BINARY_OPERATOR);
    }
    
    Expr expr = assignment();
    
    while(match(COMMA)) {
      Token operator = previous();
      Expr right = assignment();
      expr = new Expr.Binary(expr, operator, right);
    }
    
    return expr;
  }
  
  //  assignment -> IDENTIFIER "=" assignment | ternary | errAssignNoLeftOperand ;
  //  errAssignNoLeftOperand -> "=" assignment;
  private Expr assignment() {
    if (check(EQUAL)) {
      error(peek(), "Expected expression before '='");
      while (match(EQUAL)) {
        assignment();
      }
      return new Expr.Erroneous(ExprErrType.NO_LEFT_OPERAND_FOR_BINARY_OPERATOR);
    }
    
    Expr expr = ternary();
    
    if (match(EQUAL)) {
      Token equals = previous();
      Expr value = assignment();
      
      if (expr instanceof Expr.Variable) {
        Token name = ((Expr.Variable)expr).name;
        return new Expr.Assign(name, value);
      }
      
      error(equals, "Invalid assignment target.");
    }
    
    return expr;
  }
  
  //  ternary -> logic_or ( "?" logic_or ":" ternary )? ;
  private Expr ternary() {
    Expr expr = logic_or();
    if (match(QUESTION_MARK)) {
      Expr ifTrue = logic_or();
      consume(COLON, "Expected ':' for ternary operator");
      Expr ifFalse = ternary();
      
      return new Expr.Ternary(expr, ifTrue, ifFalse);
    }
    
    return expr;
    
  }
  
  //  logic_or -> logic_and ( "or" logic_and )* | errLogic_orNoLeftOperand ;
  //  errLogic_orNoLeftOperand -> ("or" logic_and)* ;
  private Expr logic_or() {
    if (check(OR)) {
      error(peek(), "Expected expression before '" + peek().lexeme +"'");
      while (match(OR)) {
        logic_and();
      }
      return new Expr.Erroneous(ExprErrType.NO_LEFT_OPERAND_FOR_BINARY_OPERATOR);
    }
    
    Expr expr = logic_and();
    
    while (match(OR)) {
      Token operator = previous();
      Expr right = logic_and();
      expr = new Expr.Logical(expr, operator, right);
    }
    
    return expr;
  }
  
  //  logic_and -> equality ( "and" equality )* | errLogic_andNoLetOperand;
  //  errLogic_andNoLeftOperand -> ( "and" equality )* ;
  private Expr logic_and() {
    if (check(AND)) {
      error(peek(), "Expected expression before '" + peek().lexeme + "'");
      while (match(AND)) {
        equality();
      }
      return new Expr.Erroneous(ExprErrType.NO_LEFT_OPERAND_FOR_BINARY_OPERATOR);
    }
    
    Expr expr = equality();
    
    while (match(AND)) {
      Token operator = previous();
      Expr right = equality();
      expr = new Expr.Logical(expr, operator, right);
    }
    
    return expr;
  }
  
  // equality -> errEqualityNoLeftOperand | comparison ( ( "!=" | "==" ) comparison )*;
  // errEqualityNoLeftOperand -> ( ( "!=" | "==" ) comparison )* ;
  private Expr equality() {
    if (check(BANG_EQUAL, EQUAL_EQUAL)) {
      error(peek(), "Expected expression before '" + peek().lexeme +"'");
      while(match(BANG_EQUAL, EQUAL_EQUAL)) {
        comparison();
      }
      return new Expr.Erroneous(ExprErrType.NO_LEFT_OPERAND_FOR_BINARY_OPERATOR);
    }
    
    Expr expr = comparison();
    
    while(match(BANG_EQUAL, EQUAL_EQUAL)) {
      Token operator = previous();
      Expr right = comparison();
      expr = new Expr.Binary(expr, operator, right);
    }
    
    return expr;
  }
  
  //  comparison -> errComparisonNoLeftOperand | term ( ( ">" | ">=" | "<" | "<=") term )* ; 
  //  errComparisonNoLeftOperand -> ( ( ">" | ">=" | "<" | "<=") term )* ; 
  private Expr comparison() {
    if (check(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      error(peek(), "Expected expression before '" + peek().lexeme + "'");
      while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
        term();
      }
      return new Expr.Erroneous(ExprErrType.NO_LEFT_OPERAND_FOR_BINARY_OPERATOR);
    }
    
    Expr expr = term();
    
    while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      Token operator = previous();
      Expr right = term();
      expr = new Expr.Binary(expr, operator, right);
    }
    
    return expr;
  }
  
  //  term -> errTermNoLeftOperand | factor ( ( "-" | "+" ) factor )*;
  //  errTermNoLeftOperand -> ( "+" factor )* ;
  private Expr term() {
    if (check(PLUS)) {
      error(peek(), "Expected expression before '" + peek().lexeme + "'");
      while (match(PLUS)) {
        term();
      }
      return new Expr.Erroneous(ExprErrType.NO_LEFT_OPERAND_FOR_BINARY_OPERATOR);
    }
    
    Expr expr = factor();
    
    while (match(PLUS, MINUS)) {
      Token operator = previous();
      Expr right = factor();
      expr = new Expr.Binary(expr, operator, right);
    }
    
    return expr;
  }
  
  //  factor -> errFactorNoLeftOperand | unary ( ( "/" | "*" ) unary )* ;
  //  errFactorNoLeftOperand -> ( ( "/" | "*" ) unary )* ;
  private Expr factor() {
    if (check(SLASH, STAR)) {
      error(peek(), "Expected expression before '" + peek().lexeme + "'");
      while (match(SLASH, STAR)) {
        unary();
      }
      return new Expr.Erroneous(ExprErrType.NO_LEFT_OPERAND_FOR_BINARY_OPERATOR);
    }
    
    Expr expr = unary();
    
    while (match(SLASH, STAR)) {
      Token operator = previous();
      Expr right = unary();
      expr = new Expr.Binary(expr, operator, right);
    }
    
    return expr;
  }
  
  // unary -> ( "!" | "-" ) unary | call ;
  private Expr unary() {
    if (match(BANG, MINUS)) {
      Token operator = previous();
      Expr right = unary();
      return new Expr.Unary(operator, right);
    }
    
    return call();
  }
  
  // call -> primary ( "(" arguments? ")" )* ;
  // arguments -> assignment ( "," assignment)* ;
  private Expr call() {
    Expr expr = primary();
    
    while (true) {
      if (match(LEFT_PAREN)) {
        expr = finishCall(expr);
      } else {
        break;
      }
    }
    
    return expr;
  }
  
  private Expr finishCall(Expr callee) {
    List<Expr> arguments = new ArrayList<>();
    if (!check(RIGHT_PAREN)) {
      do {
        if (arguments.size() >= 255) {
          error(peek(), "Can't have more than 255 arguments.");
        }
        arguments.add(assignment());
      } while (match(COMMA));
    }
    
    Token paren = consume(RIGHT_PAREN, "Expected ')' after arguments.");
    
    return new Expr.Call(callee, paren, arguments);
  }
  
  // primary -> NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" | IDENTIFIER;
  private Expr primary() {
    if (match(FALSE)) return new Expr.Literal(false);
    if (match(TRUE)) return new Expr.Literal(true);
    if (match(NIL)) return new Expr.Literal(null);
    
    if (match(NUMBER, STRING)) {
      return new Expr.Literal(previous().literal);
    }
    
    if (match(IDENTIFIER)) {
      return new Expr.Variable(previous());
    }
    
    if (match(LEFT_PAREN)) {
      Expr expr = expression();
      consume(RIGHT_PAREN, "Expect ')' after expression.");
      return new Expr.Grouping(expr);
    }
    
    throw error(peek(), "Expected expression.");
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
        case BREAK:
          return;
      }
      
      advance();
    }
  }
}
