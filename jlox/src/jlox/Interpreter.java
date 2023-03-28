package jlox;


import static jlox.TokenType.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java.util.HashMap;

class Interpreter implements Expr.Visitor<Object>,
                             Stmt.Visitor<Void> {
  
  private Map<String, Object> globals = new HashMap<>();
  private Environment environment;
  private static boolean brakeSet = false;
  private static boolean continueSet = false;
  private final Map<Expr, Integer> locals = new HashMap<>();
  private final Map<Expr, Integer> environmentIndexes = new HashMap<>();
  
  Interpreter() {
    globals.put("clock", new LoxCallable() {

      @Override
      public int arity() { return 0; }

      @Override
      public Object call(Interpreter interpreter, 
                         List<Object> arguments) {
        return (double)System.currentTimeMillis() / 1000.0;
      }
      
      @Override
      public String toString() { return "<native fn>"; }
      
    });
  }
  
  void interpret(List<Stmt> statements) {
    try {
      for (Stmt statement : statements) {
        execute(statement);
      }
    } catch (RuntimeError error) {
      Lox.runtimeError(error);
    }
  }
  
  void interpret(Stmt statement) {
    try {
      execute(statement);
    } catch (RuntimeError error) {
      Lox.runtimeError(error);
    }
  }
  
  Object interpret(Expr expression) {
    try {
      return evaluate(expression);
    } catch (RuntimeError error) {
      Lox.runtimeError(error);
      return null;
    }
  }
  
  public String stringify(Object object) {
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
  public Void visitVarStmt(Stmt.Var stmt) {
    Object value = null;
    if (stmt.initializer != null) {
      value = evaluate(stmt.initializer);
    }
    
    if (environment != null) { 
      environment.define(value);
    } else {
      globals.put(stmt.name.lexeme, value);
    }
    return null;
  }
  
  @Override
  public Object visitAssignExpr(Expr.Assign expr) {
    Object value = evaluate(expr.value);
    
    Integer distance = locals.get(expr);
    Integer index = environmentIndexes.get(expr);
    if (distance != null) {
      environment.assignAt(distance, index, value);
    } else {
      globals.put(expr.name.lexeme, value);
    }
    
    return value;
  }

  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    evaluate(stmt.expression);
    return null;
  }
  
  @Override
  public Void visitClassStmt(Stmt.Class stmt) {
    Object superclass = null;
    if (stmt.superclass != null) {
      superclass = evaluate(stmt.superclass);
      if (!(superclass instanceof LoxClass)) {
        throw new RuntimeError(stmt.superclass.name,
            "Superclass must be a class");
      }
    }
    
    if (environment != null) {
      environment.define(null);
      LoxClass klass = createClass(stmt, (LoxClass)superclass);
      environment.updateLatestDefine(klass);
    } else {
      globals.put(stmt.name.lexeme, null);
      LoxClass klass = createClass(stmt, (LoxClass)superclass);
      globals.put(klass.name, klass);
    }
    
    return null;
  }
  
  @Override
  public Object visitSuperExpr(Expr.Super expr) {
    int distance = locals.get(expr);
    int index = environmentIndexes.get(expr);
    LoxClass superclass = (LoxClass) environment.getAt(
        distance, index);
  
    // "this" is always one environment closer, at index 0
    LoxInstance object = (LoxInstance)environment.getAt(
        distance - 1, 0);
    
    LoxFunction method = superclass.findMethod(expr.method.lexeme);
    
    if (method == null) {
      throw new RuntimeError(expr.method,
          "Undefined method '" + expr.method.lexeme + "'.");
    }
    
    return method.bind(object);
        
  }
  
  private LoxClass createClass(Stmt.Class stmt, LoxClass superclass) {
    if (stmt.superclass != null) {
      environment = new Environment(environment);
      environment.define(superclass);
    }
    
    Map<String, LoxFunction> methods = createLoxFunctions(stmt.methods);
    Map<String, LoxFunction> staticMethods = createLoxFunctions(stmt.staticMethods);
    
    LoxClass metaclass = new LoxClass(
        stmt.name.lexeme, superclass, staticMethods, null);
    
    LoxClass klass = new LoxClass(
        stmt.name.lexeme, superclass, methods, metaclass);
    
    if (stmt.superclass != null) {
      environment = environment.enclosing;
    }
    
    return klass;
  }
  
  private Map<String, LoxFunction> createLoxFunctions(List<Stmt.Function> functions) {
    Map<String, LoxFunction> loxFunctions = new HashMap<>();
    for (Stmt.Function function : functions) {
      LoxFunction loxFunction = new LoxFunction(
          function.name.lexeme, function.params, function.body, environment,
          function.name.lexeme.equals("init"));
      loxFunctions.put(function.name.lexeme, loxFunction);
    }
    return loxFunctions;
  }
  
  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    LoxFunction function = new LoxFunction(stmt.name.lexeme, stmt.params, stmt.body, environment, false);
    if (environment != null) {
      environment.define(function);
    } else {
      globals.put(stmt.name.lexeme, function);
    }
    
    return null;
  }
  
  @Override
  public Object visitLambdaExpr(Expr.Lambda expr) {
    return new LoxFunction("lambda", expr.params, expr.body, environment, false);
  }
  
  
  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    if (isTruthy(evaluate(stmt.condition))) {
      execute(stmt.thenBranch);
    } else if (stmt.elseBranch != null) {
      execute(stmt.elseBranch);
    }
    return null;
  }

  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    Object value = evaluate(stmt.expression);
    System.out.println(stringify(value));
    return null;
  }
  
  @Override
  public Void visitJumpStmt(Stmt.Jump stmt) {
    if (stmt.keyword.type == BREAK) {
      throw new Break();
    } else if (stmt.keyword.type == CONTINUE) {
      throw new Continue();
    } else if (stmt.keyword.type == RETURN) {
      Object value = null;
      if (stmt.value != null) value = evaluate(stmt.value);
      throw new Return(value);
    }
    return null;
  }
  
  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    while (isTruthy(evaluate(stmt.condition))) {
      try {
        execute(stmt.body);
      } catch(Break ex) {
        return null;
      } catch(Continue ex) {}
    }
    return null;
  }
  
  @Override
  public Void visitForStmt(Stmt.For stmt) {
    while (isTruthy(evaluate(stmt.condition))) {
      try {
        execute(stmt.body);
      } catch(Break ex) {
        return null;
      } catch(Continue ex) {}
      
      evaluate(stmt.increment);
    }
    return null;
  }
  
  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    executeBlock(stmt.statements, new Environment(environment));
    return null;
  }
  
  void executeBlock(List<Stmt> statements, Environment environment) {
    Environment previous = this.environment;
    try {
      this.environment = environment;
      
      for (Stmt statement : statements) {
        execute(statement);
      }
    } finally {
      this.environment = previous;
    }
  }
  
  @Override
  public Object visitLogicalExpr(Expr.Logical expr) {
    Object left = evaluate(expr.left);
    
    if (expr.operator.type == TokenType.OR) {
      if (isTruthy(left)) return left;
    } else {
      if (!isTruthy(left)) return left;
    }
    
    return evaluate(expr.right);
  }
  
  @Override
  public Object visitBinaryExpr(Expr.Binary expr) {
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
        return (double)left <= (double)right;
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
  public Object visitTernaryExpr(Expr.Ternary expr) {
    if (isTruthy(evaluate(expr.cond))) {
      return evaluate(expr.ifTrue);
    } else {
      return evaluate(expr.ifFalse);
    }
  }

  @Override
  public Object visitGroupingExpr(Expr.Grouping expr) {
    return evaluate(expr.expression);
  }

  @Override
  public Object visitLiteralExpr(Expr.Literal expr) {
    return expr.value;
  }
  
  @Override
  public Object visitGetExpr(Expr.Get expr) {
    Object object = evaluate(expr.object);
    if (object instanceof LoxInstance) {
      return ((LoxInstance) object).get(expr.name);
    }
    
    throw new RuntimeError(expr.name, "Can only access properties of instances.");
  }
  
  @Override
  public Object visitSetExpr(Expr.Set expr) {
    Object object = evaluate(expr.object);
    
    if (!(object instanceof LoxInstance)) {
      throw new RuntimeError(expr.name, "Can only access properties of instances.");
    }
    
    Object value = evaluate(expr.value);
    ((LoxInstance)object).set(expr.name, value);
    return value;
  }
  
  @Override
  public Object visitThisExpr(Expr.This expr) {
    return lookUpVariable(expr.keyword, expr);
  }
  
  @Override
  public Object visitCallExpr(Expr.Call expr) {
    Object callee = evaluate(expr.callee);
    
    List<Object> arguments = new ArrayList<>();
    for (Expr argument : expr.arguments) {
      arguments.add(evaluate(argument));
    }
    
    if (!(callee instanceof LoxCallable)) {
      throw new RuntimeError(expr.paren, "Can only call functions and classes.");
    }
    
    LoxCallable function = (LoxCallable)callee;
    
    if (arguments.size() != function.arity()) {
      throw new RuntimeError(expr.paren, 
          "Expected " + function.arity() 
          + " arguments but got "
          + arguments.size() + ".");
    }
    
    return function.call(this, arguments);
  }
  
  
  @Override
  public Object visitUnaryExpr(Expr.Unary expr) {
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
  
  @Override
  public Object visitVariableExpr(Expr.Variable expr) {
    return lookUpVariable(expr.name, expr);
  }
  
  private Object lookUpVariable(Token name, Expr expr) {
    Integer distance = locals.get(expr);
    Integer index = environmentIndexes.get(expr);
    if (distance != null) {
      return environment.getAt(distance, index);
    } else {
      return globals.get(name.lexeme);
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
  public Object visitErroneousExpr(Expr.Erroneous expr) {
    // TODO Auto-generated method stub
    return null;
  }
  
  private Object evaluate(Expr expr) {
    return expr.accept(this);
  }
  
  private void execute(Stmt stmt) {
    stmt.accept(this);
  }
  
  void resolve(Expr expr, int depth, int environmentIndex) {
    locals.put(expr, depth);
    environmentIndexes.put(expr, environmentIndex);
  }



}
