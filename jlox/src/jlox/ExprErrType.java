package jlox;

enum ExprErrType {
  NO_LEFT_OPERAND_FOR_BINARY_OPERATOR("No left operand provided for binary expression");

  public final String message;
  
  private ExprErrType(String message) {
    this.message = message;
  }
}
