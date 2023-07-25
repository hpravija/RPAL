import java.util.Stack;

// For evaluating conditionals
public class Beta extends ASTNode {
  private Stack<ASTNode> thenPart;
  private Stack<ASTNode> elsePart;

  public Beta() {
    setType(ASTNodeType.BETA);
    thenPart = new Stack<ASTNode>();
    elsePart = new Stack<ASTNode>();
  }

  public Stack<ASTNode> getThenPart() {
    return thenPart;
  }

  public Stack<ASTNode> getElsePart() {
    return elsePart;
  }

  public void setThenPart(Stack<ASTNode> thenPart) {
    this.thenPart = thenPart;
  }

  public void setElsePart(Stack<ASTNode> elsePart) {
    this.elsePart = elsePart;
  }

  public Beta accept(NodeCopier nodeCopier) {
    return nodeCopier.copy(this);
  }

}
