package jlox;

class Continue extends RuntimeException {
  Continue() {
    super(null, null, false, false);
  }
}
