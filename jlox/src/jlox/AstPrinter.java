package jlox;

import jlox.Expr.Binary;
import jlox.Expr.Ternary;
import jlox.Expr.Grouping;
import jlox.Expr.Literal;
import jlox.Expr.Unary;
import jlox.Expr.Erroneous;

public class AstPrinter implements Expr.Visitor<String> {
  String print(Expr expr) {
    return expr.accept(this);
  }

  @Override
  public String visitBinaryExpr(Binary expr) {
    return parenthesize(expr.operator.lexeme, expr.left, expr.right);
  }
  
  @Override
  public String visitErroneousExpr(Erroneous expr) {
    return parenthesize(expr.error_type.message);
  }
  
  @Override
  public String visitTernaryExpr(Ternary expr) {
    return parenthesize("?:", expr.cond, expr.ifTrue, expr.ifFalse);
  }
  
  @Override
  public String visitGroupingExpr(Grouping expr) {
    return parenthesize("", expr.expression);
  }

  @Override
  public String visitLiteralExpr(Literal expr) {
    if (expr.value == null) return "nil";
    return expr.value.toString();
  }

  @Override
  public String visitUnaryExpr(Unary expr) {
    return parenthesize(expr.operator.lexeme, expr.right);
  }
  
  private String parenthesize(String name, Expr...exprs) {
    StringBuilder builder = new StringBuilder();
    
    builder.append("(").append(name);
    for (Expr expr : exprs) {
      builder.append(" ");
      builder.append(expr.accept(this));
    }
    builder.append(")");
    
    return builder.toString();
  }
  
  private String rpn(String name, Expr...exprs) {
    StringBuilder builder = new StringBuilder();
    
    for (Expr expr : exprs) {
      builder.append(expr.accept(this));
      builder.append(" ");
    }
    
    builder.append(name);
    
    return builder.toString();
  }
    
}
