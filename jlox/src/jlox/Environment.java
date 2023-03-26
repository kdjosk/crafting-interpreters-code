package jlox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Environment {
  final Environment enclosing;
  private final List<Object> values = new ArrayList<>();
  
  Environment() {
    enclosing = null;
  }
  
  Environment(Environment enclosing) {
    this.enclosing = enclosing;
  }
  
  void define(Object value) {
    values.add(value);
  }
  
  Object getAt(int distance, int index) {
    return ancestor(distance).values.get(index);
  }
  
  void assignAt(int distance, int index, Object value) {
    ancestor(distance).values.set(index, value);
  }
  
  void updateLatestDefine(Object value) {
    // Useful in case of two stage class declaration
    values.set(values.size() - 1, value);
  }
  
  Environment ancestor(int distance) {
    Environment environment = this;
    for (int i = 0; i < distance; i++) {
      environment = environment.enclosing;
    }
    
    return environment;
  }
  
  
}
