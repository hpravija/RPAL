public class ASTNode {
  private ASTNodeType type;
  private String value;
  private ASTNode child;
  private ASTNode sibling;
  private int sourceLineNumber;

  public String getName() {
    return type.name();
  }

  public ASTNodeType getType() {
    return type;
  }

  public void setType(ASTNodeType type) {
    this.type = type;
  }

  public ASTNode getChild() {
    return child;
  }

  public void setChild(ASTNode child) {
    this.child = child;
  }

  public ASTNode getSibling() {
    return sibling;
  }

  public void setSibling(ASTNode sibling) {
    this.sibling = sibling;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public ASTNode accept(NodeCopier nodeCopier) {
    return nodeCopier.copy(this);
  }

  public int getSourceLineNumber() {
    return sourceLineNumber;
  }

  public void setSourceLineNumber(int sourceLineNumber) {
    this.sourceLineNumber = sourceLineNumber;
  }
}
