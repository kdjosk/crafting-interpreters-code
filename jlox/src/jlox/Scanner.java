package jlox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static jlox.TokenType.*;

public class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private List<String> linesText = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;
    private int lineStart = 0;
    private int column = -1;
    
    
    private static final Map<String, TokenType> keywords;
    
    static {
        keywords = new HashMap<>();
        keywords.put("and", AND);
        keywords.put("class", CLASS);
        keywords.put("else", ELSE);
        keywords.put("false", FALSE);
        keywords.put("for", FOR);
        keywords.put("fun", FUN);
        keywords.put("if", IF);
        keywords.put("nil", NIL);
        keywords.put("or", OR);
        keywords.put("print", PRINT);
        keywords.put("return", RETURN);
        keywords.put("super", SUPER);
        keywords.put("this", THIS);
        keywords.put("true", TRUE);
        keywords.put("var", VAR);
        keywords.put("while", WHILE);
        keywords.put("break", BREAK);
        keywords.put("continue", CONTINUE);
    }
    
    Scanner(String source) {
      this.source = source;
    }
    
    List<String> getLines() {
      return linesText;
    }
    
    List<Token> scanTokens() {
      while (!isAtEnd()) {
        start = current;
        scanToken();
      }
      linesText.add(source.substring(lineStart, current));
      tokens.add(new Token(EOF, "", null, line, column));
      return tokens;
    }
    
    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case '-': addToken(MINUS); break;
            case '+': addToken(PLUS); break;
            case ';': addToken(SEMICOLON); break;
            case '*': addToken(STAR); break;
            case '?': addToken(QUESTION_MARK); break;
            case ':': addToken(COLON); break;
            case '!':
                addToken(match('=') ? BANG_EQUAL : BANG);
                break;
            case '=':
                addToken(match('=') ? EQUAL_EQUAL : EQUAL);
                break;
            case '<':
                addToken(match('=') ? LESS_EQUAL : LESS);
                break;
            case '>':
                addToken(match('=') ? GREATER_EQUAL : GREATER);
                break;
            case '/':
                comment();
                break;
            case ' ':
            case '\r':
            case '\t':
                // Ignore whitespace.
                break;
            case '\n':
                line++;
                column = -1;
                linesText.add(source.substring(lineStart, current - 1));
                lineStart = current;
                break;
                
            case '"': string(); break;
            
            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    Lox.error(line, column, "Unexpected character: " + c);
                }
                break;
        }
    }
    
    private void identifier() {
        while (isAlphaNumeric(peek())) advance();
        
        String text = source.substring(start, current);
        TokenType type = keywords.get(text);
        if (type == null) type = IDENTIFIER;
        addToken(type);
    }
    
    private void number() {
        while (isDigit(peek())) advance();
        
        if (peek() == '.' && isDigit(peekNext())) {
            advance();
            while (isDigit(peek())) advance();
        }
        
        addToken(NUMBER,
                Double.parseDouble(source.substring(start, current)));
    }
    
    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++;
            advance();
        }
        
        if (isAtEnd()) {
            Lox.error(line, column, "Unterminated string.");
            return;
        }
        
        // The closing ".
        advance();
        
        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }
    
    private void comment() {
        if (match('/')) {
            // A comment goes until the end of the line.
            while (peek() != '\n' && !isAtEnd()) advance();
        } else if (match('*')) {
            // Block comment 
            int nest_lvl = 1;
            while (!isAtEnd()) {
                if (peek() == '\n') { 
                    line++;
                    column = -1;
                    linesText.add(source.substring(lineStart, current - 1));
                    lineStart = current;
                } else if (peek() == '/' && peekNext() == '*') {
                    nest_lvl++;
                    advance(); advance();
                } else if (peek() == '*' && peekNext() == '/') {
                    nest_lvl--;
                    advance(); advance();
                }
                
                if (nest_lvl == 0) {
                    return;
                }
                
                if (!isAtEnd()) {
                    advance();
                }
            }
            
            Lox.error(line, column, "Unterminated block comment");
        } else {
            addToken(SLASH);
        }
    }
    
    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;
        
        current++;
        column++;
        return true;
    }
    
    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }
    
    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }
    
    private boolean isAlpha(char c) {
        return c >= 'a' && c <= 'z' ||
               c >= 'A' && c <= 'Z' ||
               c == '_';
    }
    
    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }
    
    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }
    
    private boolean isAtEnd() {
        return current >= source.length();
    }
    
    private char advance() {
        column++;
        return source.charAt(current++);
    }
    
    private void addToken(TokenType type) {
        addToken(type, null);
    }
    
    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line, column - text.length() + 1));
    }
}
