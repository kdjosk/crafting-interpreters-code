package jlox;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Stack;


class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
  private final Interpreter interpreter;
  private final Stack<Map<String, Variable>> scopes = new Stack<>();
  
  private FunctionType currentFunction = FunctionType.NONE;
  private ClassType currentClass = ClassType.NONE;
  private boolean insideLoop = false;
  
  Resolver(Interpreter interpreter) {
    this.interpreter = interpreter;
  }
  
  private class Variable {
    VariableState state;
    Token declaredName;
    // order of declaration in scopes is always the same as you can't 
    // conditionally declare a variable in the same scope, e.g.
    // if (a > 5) var b = 5;
    // is not allowed, therefore each variable can have a unique index
    // in it's respective environment
    int environmentIndex; 
    
    Variable(Token declaredName, VariableState state, int environmentIndex) {
      this.state = state;
      this.declaredName = declaredName;
      this.environmentIndex = environmentIndex;
    }
  }
 
  private enum VariableState {
    DECLARED,
    DEFINED,
    READ,
  }
  
  private enum FunctionType {
    NONE,
    FUNCTION,
    METHOD,
    INITIALIZER
  }
  
  private enum ClassType {
    NONE,
    CLASS,
    SUBCLASS
  }
  
  void resolve(List<Stmt> statements) {
    for (Stmt statement : statements) {
      resolve(statement);
    }
  }
  
  private void resolve(Stmt stmt) {
    stmt.accept(this);
  }
  
  private void resolve(Expr expr) {
    expr.accept(this);
  }
  
  private void beginScope() {
    scopes.push(new HashMap<String, Variable>());
  }
  
  private void endScope() {
    Map<String, Variable> scope = scopes.pop();
    for (Map.Entry<String, Variable> entry : scope.entrySet()) {
      if (entry.getValue().state != VariableState.READ) {
        Lox.error(entry.getValue().declaredName, "Local variable is never used");
      }
    }
  }
  
  private void declare(Token name) {
    if (scopes.isEmpty()) return;
    
    Map<String, Variable> scope = scopes.peek();
    if (scope.containsKey(name.lexeme)) {
      Lox.error(name, "Variable redeclaration is not allowed.");
    }
    scope.put(name.lexeme, new Variable(name, VariableState.DECLARED, scope.size()));
  }
 

  private void define(Token name) {
    if (scopes.isEmpty()) return;
    scopes.peek().get(name.lexeme).state = VariableState.DEFINED;
  }
  
  private void resolveLocal(Expr expr, Token name, boolean isRead) {
    for (int i = scopes.size() - 1; i >= 0; i--) {
      Map<String, Variable> scope = scopes.get(i);
      if (scope.containsKey(name.lexeme)) {
        interpreter.resolve(expr, scopes.size() - 1 - i, scope.get(name.lexeme).environmentIndex);
        
        if (isRead) {
          scopes.get(i).get(name.lexeme).state = VariableState.READ;
        }
        return;
      }
    }
  }
  
  private void resolveFunction(
      List<Token> params, List<Stmt> body, FunctionType type) {
    FunctionType enclosingFunction = currentFunction;
    currentFunction = type;
    
    beginScope();
    for (Token param : params) {
      declare(param);
      define(param);
    }
    resolve(body);
    endScope();
    
    currentFunction = enclosingFunction;
  }

  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    beginScope();
    resolve(stmt.statements);
    endScope();
    return null;
  }
  
  @Override
  public Void visitClassStmt(Stmt.Class stmt) {
    ClassType enclosingClass = currentClass;
    currentClass = ClassType.CLASS;
    
    declare(stmt.name);
    define(stmt.name);
    
    if (stmt.superclass != null &&
        stmt.name.lexeme.equals(stmt.superclass.name.lexeme)) {
      Lox.error(stmt.superclass.name, 
          "A class can't inherit from itself.");
    }
    
    if (stmt.superclass != null) {
      currentClass = ClassType.SUBCLASS;
      resolve(stmt.superclass);
      beginScope();
      scopes.peek().put("super", new Variable(stmt.name, VariableState.READ, 0));
    }
    
    beginScope();
    
    for (Stmt.Function method : stmt.staticMethods) {
      FunctionType type = FunctionType.METHOD;
      resolveFunction(method.params, method.body, type);
    }
    
    scopes.peek().put("this", new Variable(stmt.name, VariableState.READ, 0)); // So we don't get unused variable errors
    
    for (Stmt.Function method : stmt.methods) {
      FunctionType type = FunctionType.METHOD;
      
      if (method.name.lexeme.equals("init")) {
        type = FunctionType.INITIALIZER;
      }
      resolveFunction(method.params, method.body, type);
    }
    
    endScope();
    
    if (stmt.superclass != null) endScope();
    
    currentClass = enclosingClass;
    return null;
  }
  
  @Override
  public Void visitSuperExpr(Expr.Super expr) {
    if (currentClass == ClassType.NONE) {
      Lox.error(expr.keyword, 
          "Can't use 'super' outside of a class.");
    } else if (currentClass != ClassType.SUBCLASS) {
      Lox.error(expr.keyword, 
          "Can't use 'super' in a class with no superclass");
    }
    
    resolveLocal(expr, expr.keyword, true);
    return null;
  }

  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    resolve(stmt.expression);
    return null;
  }

  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    declare(stmt.name);
    define(stmt.name);
    
    resolveFunction(stmt.params, stmt.body, FunctionType.FUNCTION);
    return null;
  }

  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    resolve(stmt.condition);
    resolve(stmt.thenBranch);
    if (stmt.elseBranch != null) resolve(stmt.elseBranch);
    return null;
  }

  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    resolve(stmt.expression);
    return null;
  }

  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    declare(stmt.name);
    if (stmt.initializer != null) {
      resolve(stmt.initializer);
    }
    define(stmt.name);
    return null;
  }

  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    boolean insideEnclosingLoop = insideLoop;
    insideLoop = true;
    resolve(stmt.condition);
    resolve(stmt.body);
    return null;
    
  }

  @Override
  public Void visitForStmt(Stmt.For stmt) {
    boolean insideEnclosingLoop = insideLoop;
    resolve(stmt.condition);
    resolve(stmt.body);
    resolve(stmt.increment);
    insideLoop = insideEnclosingLoop;
    return null;
  }

  @Override
  public Void visitJumpStmt(Stmt.Jump stmt) {
    if (stmt.keyword.type == TokenType.RETURN) {
      if (currentFunction == FunctionType.NONE) {
        Lox.error(stmt.keyword, "Can't return from outside of a function body.");
      } else if (currentFunction == FunctionType.INITIALIZER
                 && stmt.value != null) {
        Lox.error(stmt.keyword, "Can't return a value from an initializer");
      }
    } else if (
        stmt.keyword.type == TokenType.CONTINUE || 
        stmt.keyword.type == TokenType.BREAK) {
       if (!insideLoop) {
         Lox.error(stmt.keyword,
             "Can't use " + stmt.keyword.lexeme 
             + " statement outside of loop body");
       }
    }
    
    if (stmt.value != null) {
      resolve(stmt.value);
    }
    return null;
  }

  @Override
  public Void visitAssignExpr(Expr.Assign expr) {
    resolve(expr.value);
    resolveLocal(expr, expr.name, false);
    return null;
  }

  @Override
  public Void visitBinaryExpr(Expr.Binary expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitTernaryExpr(Expr.Ternary expr) {
    resolve(expr.cond);
    resolve(expr.ifTrue);
    resolve(expr.ifFalse);
    return null;
  }

  @Override
  public Void visitGroupingExpr(Expr.Grouping expr) {
    resolve(expr.expression);
    return null;
  }

  @Override
  public Void visitLiteralExpr(Expr.Literal expr) {
    return null;
  }

  @Override
  public Void visitLogicalExpr(Expr.Logical expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitUnaryExpr(Expr.Unary expr) {
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitCallExpr(Expr.Call expr) {
    resolve(expr.callee);
    
    for (Expr argument : expr.arguments) {
      resolve(argument);
    }
    
    return null;
  }
  
  @Override
  public Void visitGetExpr(Expr.Get expr) {
    resolve(expr.object);
    return null;
  }
  
  @Override
  public Void visitSetExpr(Expr.Set expr) {
    resolve(expr.value);
    resolve(expr.object);
    return null;
  }
  
  @Override
  public Void visitThisExpr(Expr.This expr) {
    if (currentClass == ClassType.NONE) {
      Lox.error(expr.keyword,
          "Can't use 'this' outside of a class body");
      return null;
    }
    
    resolveLocal(expr, expr.keyword, true);
    return null;
  }
  
  @Override
  public Void visitErroneousExpr(Expr.Erroneous expr) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Void visitVariableExpr(Expr.Variable expr) {
    if (!scopes.isEmpty() && 
        scopes.peek().containsKey(expr.name.lexeme) &&
        scopes.peek().get(expr.name.lexeme).state == VariableState.DECLARED) {
      Lox.error(expr.name, "Can't read local variable in its own initializer");
    }
    
    resolveLocal(expr, expr.name, true);
    return null;
  }

  @Override
  public Void visitLambdaExpr(Expr.Lambda expr) {
    resolveFunction(expr.params, expr.body, FunctionType.FUNCTION);
    return null;
  }

}
