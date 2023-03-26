package jlox;

import java.util.List;

public class LoxFunction implements LoxCallable {
  private final String name;
  private final List<Token> params;
  private final List<Stmt> body;
  private final Environment closure;
  private final boolean isInitializer;
  
  LoxFunction(
      String name, List<Token> params,
      List<Stmt> body, Environment closure,
      boolean isInitializer) {
    this.isInitializer = isInitializer;
    this.name = name;
    this.params = params;
    this.body = body;
    this.closure = closure;
  }
  
  LoxFunction bind(LoxInstance instance) {
    Environment environment = new Environment(closure);
    environment.define(instance);
    return new LoxFunction(name, params, body, environment, isInitializer);
  }
  
  @Override
  public int arity() {
    return params.size();
  }

  @Override
  public Object call(Interpreter interpreter, List<Object> arguments) {
    Environment environment = new Environment(closure);
    
    for (int i = 0; i < params.size(); i++) {
      environment.define(arguments.get(i));
    }
    
    try {
      interpreter.executeBlock(body, environment);
    } catch (Return returnValue) {
      return returnValue.value;
    }
    
    if (isInitializer) return closure.getAt(0, 0); // "this"
    return null;
  }
  
  @Override
  public String toString() {
    return "<fn " + name + ">";
  }
}


