package jlox;

import jlox.Expr.Binary;
import jlox.Expr.Erroneous;
import jlox.Expr.Grouping;
import jlox.Expr.Literal;
import jlox.Expr.Ternary;
import jlox.Expr.Unary;
import static jlox.TokenType.*;

class Interpreter implements Expr.Visitor<Object> {
  
  void interpret(Expr expression) {
    try {
      Object value = evaluate(expression);
      System.out.println(stringify(value));
    } catch (RuntimeError error) {
      Lox.runtimeError(error);
    }
  }
  
  private String stringify(Object object) {
    if (object == null) return "nil";
    
    if (object instanceof Double) {
      String text = object.toString();
      if (text.endsWith(".0")) {
        text = text.substring(0, text.length() - 2);
      }
      return text;
    }
    if (object instanceof String) {
      return "\"" + (String)object + "\"";
    }
    
    return object.toString();
  }

  @Override
  public Object visitBinaryExpr(Binary expr) {
    Object left = evaluate(expr.left);
    Object right = evaluate(expr.right);
    
    switch (expr.operator.type) {
      case COMMA:
        evaluate(expr.left);
        return evaluate(expr.right);
      case BANG_EQUAL: return !isEqual(left, right);
      case EQUAL_EQUAL: return isEqual(left, right);
      case GREATER:
        checkNumberOperands(expr.operator, left, right);
        return (double)left > (double)right;
      case GREATER_EQUAL:
        checkNumberOperands(expr.operator, left, right);
        return (double)left >= (double)right;
      case LESS:
        checkNumberOperands(expr.operator, left, right);
        return (double)left < (double)right;
      case LESS_EQUAL:
        checkNumberOperands(expr.operator, left, right);
        return (double)left >= (double)right;
      case MINUS:
        checkNumberOperands(expr.operator, left, right);
        return (double)left - (double)right;
      case SLASH:
        checkNumberOperands(expr.operator, left, right);
        if ((double)right == 0.0) {
          throw new RuntimeError(expr.operator, "Division by zero.");
        }
        return (double)left / (double)right;
      case STAR:
        checkNumberOperands(expr.operator, left, right);
        return (double)left * (double)right;
      case PLUS:
        if (left instanceof Double && right instanceof Double) {
          return (double)left + (double)right;
        }
        if (left instanceof String && right instanceof String) {
          return (String)left + (String)right;
        }
        if (left instanceof String) {
          return (String)left + stringify(right);
        }
        if (right instanceof String) {
          return stringify(left) + (String)right;
        }
        
        throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
      default:
        return null;
    }
  }

  @Override
  public Object visitTernaryExpr(Ternary expr) {
    if (isTruthy(evaluate(expr.cond))) {
      return evaluate(expr.ifTrue);
    } else {
      return evaluate(expr.ifFalse);
    }
  }

  @Override
  public Object visitGroupingExpr(Grouping expr) {
    return evaluate(expr.expression);
  }

  @Override
  public Object visitLiteralExpr(Literal expr) {
    return expr.value;
  }

  @Override
  public Object visitUnaryExpr(Unary expr) {
    Object right = evaluate(expr.right);
    
    switch (expr.operator.type) {
      case MINUS:
        checkNumberOperand(expr.operator, right);
        return -(double)right;
      case BANG:
        return !isTruthy(right);
      default:
        return null;
    }
  }
  
  private void checkNumberOperand(Token operator, Object operand) {
    if (operand instanceof Double) return;
    throw new RuntimeError(operator, "Operand must be a number.");
  }
  
  private void checkNumberOperands(Token operator, Object left, Object right) {
    if (left instanceof Double && right instanceof Double) return;
    throw new RuntimeError(operator, "Operands must be a number.");
  }
  
  
  private boolean isTruthy(Object object) {
    if (object == null) return false;
    if (object instanceof Boolean) return (boolean)object;
    return true;
  }
  
  private boolean isEqual(Object a, Object b) {
    if (a == null && b == null) return true;
    if (a == null) return false;
    
    return a.equals(b);
  }

  @Override
  public Object visitErroneousExpr(Erroneous expr) {
    // TODO Auto-generated method stub
    return null;
  }
  
  private Object evaluate(Expr expr) {
    return expr.accept(this);
  }
  
  
}
