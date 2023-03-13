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
        
        
        run(text);
        if (hadError) System.exit(65);
        if (hadRuntimeError) System.exit(70);
    }

    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for (;;) {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null)
                break;
            run(line);
            hadError = false;
        }
    }

    private static void run(String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        linesText = scanner.getLines();
        Parser parser = new Parser(tokens);
        Expr expression = parser.parse();
        
        // Stop if there was a syntax error.
        if (hadError) return;
        
        for (Token t : tokens) {
          System.out.println(t.toString());
        }
        System.out.println(new AstPrinter().print(expression));
        interpreter.interpret(expression);
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
        if (column >= 0) {
          System.err.println(linesText.get(line - 1));
          System.err.println(" ".repeat(column) + "^");  
        }
		System.err.println(
			"[line: " + line + "] Error: " + message);
		hadError = true;
	}

}
