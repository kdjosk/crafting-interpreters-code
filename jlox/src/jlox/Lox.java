package jlox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Lox {
    private static final Interpreter interpreter = new Interpreter();
    static boolean hadError = false;
    static boolean hadRuntimeError = false;
    private static List<String> linesText = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: jlox [script]");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        String text = new String(bytes, Charset.defaultCharset());
        
        runFromFile(text);
        if (hadError) System.exit(65);
        if (hadRuntimeError) System.exit(70);
    }

    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for (;;) {
            System.out.print(">>> ");
            String line = reader.readLine();
            if (line == null)
                break;
            runFromInteractive(line);
            hadError = false;
        }
    }
    
    private static Parser makeParser(String source) {
      Scanner scanner = new Scanner(source);
      List<Token> tokens = scanner.scanTokens();
      linesText = scanner.getLines();
      return new Parser(tokens);
    }
    
    private static void runFromInteractive(String source) {
      Parser parser = makeParser(source);
    
      Object stmt_or_expr = parser.parseFromInteractive();
      
      // Stop if there was a syntax error.
      if (hadError) return;
      
      if (stmt_or_expr instanceof Stmt) {
        interpreter.interpret((Stmt)stmt_or_expr);
      } else if (stmt_or_expr instanceof Expr) {
        Object value = interpreter.interpret((Expr)stmt_or_expr);
        System.out.println(interpreter.stringify(value));
      }
    }

    private static void runFromFile(String source) {
        Parser parser = makeParser(source);
        List<Stmt> statements = parser.parseFromFile();
        
        // Stop if there was a syntax error.
        if (hadError) return;
        
        Resolver resolver = new Resolver(interpreter);
        resolver.resolve(statements);
        
        // Stop if there was a resolution error.
        if (hadError) return;
        
        interpreter.interpret(statements);
    }
    
    static void error(int line, int column, String message) {
      report(line, column, message);
    }
    
    static void error(Token token, String message) {
      if (token.type == TokenType.EOF) {
        report(token.line, token.column, message);
      } else {
        report(token.line, token.column, message);
      }
    }
    
    static void runtimeError(RuntimeError error) {
      report(error.token.line, error.token.column, error.getMessage());
      hadRuntimeError = false;
    }

    private static void report(int line, int column, String message) {
      if (column < 0) column = 0;
      
      if(line >= 2) {
        System.err.println((line - 1) + " | " + linesText.get(line - 2));
      }
      System.err.println(line + " | " + linesText.get(line - 1));
      System.err.println(" ".repeat(column + 4) + "^");  
      System.err.println(
			"[line: " + line + "] Error: " + message);
      hadError = true;
    }

}
