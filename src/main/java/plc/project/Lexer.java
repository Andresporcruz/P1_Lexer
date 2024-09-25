//Andres Portillo
//Final  Modification 9-13-2024

package plc.project;

import java.util.ArrayList;
import java.util.List;

public final class Lexer {

    private final String input; // The input string to lex
    private int index = 0; // Current position in the input
    private int startPosition; // Start position of the current token

    public Lexer(String input) {
        this.input = input;
    }

    /**
     * Main method to lex the input string into a list of tokens.
     * It skips whitespace and lexes each token until the end of the input.
     */
    public List<Token> lex() throws ParseException {
        List<Token> tokens = new ArrayList<>();
        while (hasNext()) {
            skipWhitespace(); // Skip any whitespace characters
            if (!hasNext()) {
                break; // Break if we've reached the end after skipping whitespace
            }
            startPosition = index; // Set the start position before lexing the next token
            Token token = lexToken(); // Lex the next token
            tokens.add(token); // Add the token to the list
        }
        return tokens; // Return the list of tokens
    }

    /**
     * Skips over any whitespace characters in the input.
     */
    private void skipWhitespace() {
        while (hasNext() && Character.isWhitespace(peek())) {
            consume(); // Consume the whitespace character
        }
    }

    /**
     * Determines which token to lex based on the current character.
     * It delegates to the appropriate lexing method.
     */
    public Token lexToken() throws ParseException {
        char current = peek();

        if (Character.isLetter(current) || current == '_') {
            // If the current character is a letter or underscore, lex an identifier
            return lexIdentifier();
        } else if (current == '+' || current == '-' || Character.isDigit(current)) {
            // If it's a sign or digit, lex a number (integer or decimal)
            return lexNumber();
        } else if (current == '\'') {
            // If it's a single quote, lex a character literal
            return lexCharacter();
        } else if (current == '"') {
            // If it's a double quote, lex a string literal
            return lexString();
        } else {
            // Otherwise, lex an operator
            return lexOperator();
        }
    }

    /**
     * Lexes an IDENTIFIER token.
     * Identifiers start with a letter or underscore, followed by letters, digits, underscores, or hyphens.
     */
    public Token lexIdentifier() throws ParseException {
        StringBuilder identifier = new StringBuilder();
        char firstChar = consume(); // Consume the first character
        if (Character.isDigit(firstChar) || firstChar == '-') {
            // Identifiers cannot start with a digit or hyphen
            throw new ParseException("Invalid Identifier start", index - 1);
        }
        identifier.append(firstChar);
        // Consume the rest of the identifier characters
        while (hasNext() && (Character.isLetterOrDigit(peek()) || peek() == '_' || peek() == '-')) {
            identifier.append(consume());
        }
        String literal = identifier.toString();
        // Create and return the IDENTIFIER token
        return new Token(Token.Type.IDENTIFIER, literal, startPosition);
    }

    /**
     * Lexes an INTEGER or DECIMAL token.
     * Numbers may start with an optional sign and must have at least one digit.
     * Decimals include a decimal point followed by one or more digits.
     */
    public Token lexNumber() throws ParseException {
        StringBuilder number = new StringBuilder();
        if (peek() == '+' || peek() == '-') {
            // Consume the optional sign
            number.append(consume());
        }
        if (!hasNext() || !Character.isDigit(peek())) {
            // There must be at least one digit
            throw new ParseException("Expected digit after sign", index);
        }
        // Consume the integer part
        while (hasNext() && Character.isDigit(peek())) {
            number.append(consume());
        }
        // Check for decimal point
        if (hasNext() && peek() == '.') {
            number.append(consume()); // Consume the decimal point
            if (!hasNext() || !Character.isDigit(peek())) {
                // There must be at least one digit after the decimal point
                throw new ParseException("Invalid decimal format", index);
            }
            // Consume the fractional part
            while (hasNext() && Character.isDigit(peek())) {
                number.append(consume());
            }
        }
        String literal = number.toString();
        // Determine the token type (INTEGER or DECIMAL) based on the presence of a decimal point
        Token.Type type = literal.contains(".") ? Token.Type.DECIMAL : Token.Type.INTEGER;
        // Create and return the INTEGER or DECIMAL token
        return new Token(type, literal, startPosition);
    }

    /**
     * Lexes a CHARACTER token.
     * Character literals are enclosed in single quotes and contain exactly one character or a valid escape sequence.
     */
    public Token lexCharacter() throws ParseException {
        consume(); // Consume the opening single quote
        if (!hasNext()) {
            // Unterminated character literal
            throw new ParseException("Unterminated character literal", index);
        }
        char character = consume();
        if (character == '\\') {
            // Handle escape sequences
            if (!hasNext()) {
                throw new ParseException("Invalid escape sequence", index);
            }
            char escapeChar = consume();
            // Validate the escape character
            if ("bnrt'\"\\".indexOf(escapeChar) == -1) {
                throw new ParseException("Invalid escape sequence", index - 1);
            }
        } else if (character == '\'' || character == '\n' || character == '\r') {
            // Invalid character in character literal
            throw new ParseException("Invalid character literal", index - 1);
        }
        if (!hasNext() || consume() != '\'') {
            // Ensure the character literal is properly closed
            throw new ParseException("Unterminated character literal", index);
        }
        // Extract the token literal from the input
        String literal = input.substring(startPosition, index);
        // Create and return the CHARACTER token
        return new Token(Token.Type.CHARACTER, literal, startPosition);
    }

    /**
     * Lexes a STRING token.
     * String literals are enclosed in double quotes and may contain valid escape sequences.
     */
    public Token lexString() throws ParseException {
        consume(); // Consume the opening double quote
        while (hasNext() && peek() != '"') {
            if (peek() == '\\') {
                consume(); // Consume the backslash
                if (!hasNext()) {
                    throw new ParseException("Invalid escape sequence", index);
                }
                char escapeChar = consume();
                // Validate the escape character
                if ("bnrt'\"\\".indexOf(escapeChar) == -1) {
                    throw new ParseException("Invalid escape sequence", index - 1);
                }
            } else if (peek() == '\n' || peek() == '\r') {
                // Strings cannot span multiple lines
                throw new ParseException("Unterminated string literal", index);
            } else {
                consume(); // Consume the character
            }
        }
        if (!hasNext()) {
            // Unterminated string literal
            throw new ParseException("Unterminated string literal", index);
        }
        consume(); // Consume the closing double quote
        // Extract the token literal from the input
        String literal = input.substring(startPosition, index);
        // Create and return the STRING token
        return new Token(Token.Type.STRING, literal, startPosition);
    }

    /**
     * Lexes an OPERATOR token.
     * Operators include any non-whitespace character not part of the other token types.
     * Special handling for multi-character operators (<=, >=, !=, ==).
     */
    public Token lexOperator() throws ParseException {
        char current = consume();
        StringBuilder operator = new StringBuilder();
        operator.append(current);
        // Check for multi-character operators
        if (hasNext() && (current == '=' || current == '!' || current == '<' || current == '>') && peek() == '=') {
            operator.append(consume()); // Consume the next character
        }
        String literal = operator.toString();
        // Create and return the OPERATOR token
        return new Token(Token.Type.OPERATOR, literal, startPosition);
    }

    /**
     * Checks if there are more characters to process.
     */
    private boolean hasNext() {
        return index < input.length();
    }

    /**
     * Peeks at the current character without consuming it.
     */
    private char peek() {
        return input.charAt(index);
    }

    /**
     * Consumes the current character and advances the index.
     */
    private char consume() {
        return input.charAt(index++);
    }
}
