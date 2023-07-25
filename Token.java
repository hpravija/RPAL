// Token passed from the scanner to parser
public class Token {
  private TokenType type;
  private String value;
  private int sourceLineNumber;

  public TokenType getType() {
    return type;
  }

  public String getValue() {
    return value;
  }

  public int getSourceLineNumber() {
    return sourceLineNumber;
  }

  public void setType(TokenType type) {
    this.type = type;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public void setSourceLineNumber(int sourceLineNumber) {
    this.sourceLineNumber = sourceLineNumber;
  }
}
