package parser;
import java.util.ArrayDeque;
import java.util.Stack;

import csem.Beta;
import csem.Delta;

// AST uses first child, next sibling representation

public class AST {
  private ASTNode root;
  private Delta currentDelta;
  private Delta rootDelta;
  private int deltaIndex;
  private boolean standardized;
  private ArrayDeque<PendingDelta> pendingDeltaQueue;

  public AST(ASTNode node) {
    this.root = node;
  }

  // Standardizing (bottom-up)

  public void standardize() {
    standardize(root);
    standardized = true;
  }

  private void standardize(ASTNode node) {
    // Standardizing child nodes
    if (node.getChild() != null) {
      ASTNode childNode = node.getChild();
      while (childNode != null) {
        standardize(childNode);
        childNode = childNode.getSibling();
      }
    }

    // Standardizing the current node
    switch (node.getType()) {
      // standardizing let
      case LET:
        ASTNode equalNode = node.getChild();
        if (equalNode.getType() != ASTNodeType.EQUAL)
          throw new StandardizeException("LET/WHERE: left child is not EQUAL");
        ASTNode e = equalNode.getChild().getSibling();
        equalNode.getChild().setSibling(equalNode.getSibling());
        equalNode.setSibling(e);
        equalNode.setType(ASTNodeType.LAMBDA);
        node.setType(ASTNodeType.GAMMA);
        break;

      // standardizing where
      case WHERE:
        equalNode = node.getChild().getSibling();
        node.getChild().setSibling(null);
        equalNode.setSibling(node.getChild());
        node.setChild(equalNode);
        node.setType(ASTNodeType.LET);
        standardize(node);
        break;

      // standardizing fcnform
      case FCNFORM:
        ASTNode childSibling = node.getChild().getSibling();
        node.getChild().setSibling(constructLambdaChain(childSibling));
        node.setType(ASTNodeType.EQUAL);
        break;

      // standardizing at
      case AT:
        ASTNode e1 = node.getChild();
        ASTNode n = e1.getSibling();
        ASTNode e2 = n.getSibling();
        ASTNode gammaNode = new ASTNode();
        gammaNode.setType(ASTNodeType.GAMMA);
        gammaNode.setChild(n);
        n.setSibling(e1);
        e1.setSibling(null);
        gammaNode.setSibling(e2);
        node.setChild(gammaNode);
        node.setType(ASTNodeType.GAMMA);
        break;

      // standardizing within
      case WITHIN:
        if (node.getChild().getType() != ASTNodeType.EQUAL
            || node.getChild().getSibling().getType() != ASTNodeType.EQUAL)
          throw new StandardizeException("WITHIN: one of the children is not EQUAL");
        ASTNode x1 = node.getChild().getChild();
        e1 = x1.getSibling();
        ASTNode x2 = node.getChild().getSibling().getChild();
        e2 = x2.getSibling();
        ASTNode lambdaNode = new ASTNode();
        lambdaNode.setType(ASTNodeType.LAMBDA);
        x1.setSibling(e2);
        lambdaNode.setChild(x1);
        lambdaNode.setSibling(e1);
        gammaNode = new ASTNode();
        gammaNode.setType(ASTNodeType.GAMMA);
        gammaNode.setChild(lambdaNode);
        x2.setSibling(gammaNode);
        node.setChild(x2);
        node.setType(ASTNodeType.EQUAL);
        break;

      // standardizing simultaneous definitions
      case SIMULTDEF:
        ASTNode commaNode = new ASTNode();
        commaNode.setType(ASTNodeType.COMMA);
        ASTNode tauNode = new ASTNode();
        tauNode.setType(ASTNodeType.TAU);
        ASTNode childNode = node.getChild();
        while (childNode != null) {
          processCommaAndTau(childNode, commaNode, tauNode);
          childNode = childNode.getSibling();
        }
        commaNode.setSibling(tauNode);
        node.setChild(commaNode);
        node.setType(ASTNodeType.EQUAL);
        break;

      // standardizing rec
      case REC:
        childNode = node.getChild();
        if (childNode.getType() != ASTNodeType.EQUAL)
          throw new StandardizeException("REC: child is not EQUAL");
        ASTNode x = childNode.getChild();
        lambdaNode = new ASTNode();
        lambdaNode.setType(ASTNodeType.LAMBDA);
        lambdaNode.setChild(x);
        ASTNode yStarNode = new ASTNode();
        yStarNode.setType(ASTNodeType.YSTAR);
        yStarNode.setSibling(lambdaNode);
        gammaNode = new ASTNode();
        gammaNode.setType(ASTNodeType.GAMMA);
        gammaNode.setChild(yStarNode);
        ASTNode xWithSiblingGamma = new ASTNode();
        xWithSiblingGamma.setChild(x.getChild());
        xWithSiblingGamma.setSibling(gammaNode);
        xWithSiblingGamma.setType(x.getType());
        xWithSiblingGamma.setValue(x.getValue());
        node.setChild(xWithSiblingGamma);
        node.setType(ASTNodeType.EQUAL);
        break;

      // standardizing lambda
      case LAMBDA:
        childSibling = node.getChild().getSibling();
        node.getChild().setSibling(constructLambdaChain(childSibling));
        break;

      default:
        // CSE Optimization Rules are applied to the rest of the Node types.
        break;
    }
  }

  // populating comma and tau nodes
  private void processCommaAndTau(ASTNode equalNode, ASTNode commaNode, ASTNode tauNode) {
    if (equalNode.getType() != ASTNodeType.EQUAL)
      throw new StandardizeException("SIMULTDEF: one of the children is not EQUAL");
    ASTNode x = equalNode.getChild();
    ASTNode e = x.getSibling();
    setChild(commaNode, x);
    setChild(tauNode, e);
  }

  private void setChild(ASTNode parentNode, ASTNode childNode) {
    if (parentNode.getChild() == null)
      parentNode.setChild(childNode);
    else {
      ASTNode lastSibling = parentNode.getChild();
      while (lastSibling.getSibling() != null)
        lastSibling = lastSibling.getSibling();
      lastSibling.setSibling(childNode);
    }
    childNode.setSibling(null);
  }

  private ASTNode constructLambdaChain(ASTNode node) {
    if (node.getSibling() == null)
      return node;
    ASTNode lambdaNode = new ASTNode();
    lambdaNode.setType(ASTNodeType.LAMBDA);
    lambdaNode.setChild(node);
    if (node.getSibling().getSibling() != null)
      node.setSibling(constructLambdaChain(node.getSibling()));
    return lambdaNode;
  }

  // processinf for delta
  
  public Delta createDeltas() {
    pendingDeltaQueue = new ArrayDeque<PendingDelta>();
    deltaIndex = 0;
    currentDelta = createDelta(root);
    processPendingDeltaStack();
    return rootDelta;
  }

  private Delta createDelta(ASTNode startBodyNode) {
    PendingDelta pendingDelta = new PendingDelta();
    pendingDelta.startNode = startBodyNode;
    pendingDelta.body = new Stack<ASTNode>();
    pendingDeltaQueue.add(pendingDelta);

    Delta d = new Delta();
    d.setBody(pendingDelta.body);
    d.setIndex(deltaIndex++);
    currentDelta = d;

    if (startBodyNode == root)
      rootDelta = currentDelta;

    return d;
  }

  private void processPendingDeltaStack() {
    while (!pendingDeltaQueue.isEmpty()) {
      PendingDelta pendingDelta = pendingDeltaQueue.pop();
      buildDeltaBody(pendingDelta.startNode, pendingDelta.body);
    }
  }

  private void buildDeltaBody(ASTNode node, Stack<ASTNode> body) {
    if (node.getType() == ASTNodeType.LAMBDA) {
      Delta d = createDelta(node.getChild().getSibling());
      if (node.getChild().getType() == ASTNodeType.COMMA) {
        ASTNode commaNode = node.getChild();
        ASTNode childNode = commaNode.getChild();
        while (childNode != null) {
          d.addBoundVars(childNode.getValue());
          childNode = childNode.getSibling();
        }
      } else
        d.addBoundVars(node.getChild().getValue());
      body.push(d);
      return;
    } else if (node.getType() == ASTNodeType.CONDITIONAL) {
      ASTNode conditionNode = node.getChild();
      ASTNode thenNode = conditionNode.getSibling();
      ASTNode elseNode = thenNode.getSibling();

      Beta betaNode = new Beta();

      buildDeltaBody(thenNode, betaNode.getThenPart());
      buildDeltaBody(elseNode, betaNode.getElsePart());

      body.push(betaNode);

      buildDeltaBody(conditionNode, body);

      return;
    }

    body.push(node);
    ASTNode childNode = node.getChild();
    while (childNode != null) {
      buildDeltaBody(childNode, body);
      childNode = childNode.getSibling();
    }
  }

  private class PendingDelta {
    Stack<ASTNode> body;
    ASTNode startNode;
  }

  public boolean isStandardized() {
    return standardized;
  }

  // Printing

  public void print() {
    preOrderPrint(root, "");
  }

  private void preOrderPrint(ASTNode node, String printPrefix) {
    if (node == null)
      return;

    printASTNodeDetails(node, printPrefix);
    preOrderPrint(node.getChild(), printPrefix + ".");
    preOrderPrint(node.getSibling(), printPrefix);
  }

  private void printASTNodeDetails(ASTNode node, String printPrefix) {
    if (node.getType() == ASTNodeType.IDENTIFIER ||
        node.getType() == ASTNodeType.INTEGER) {
      System.out.printf(printPrefix + node.getType().getPrintName() + "\n", node.getValue());
    } else if (node.getType() == ASTNodeType.STRING)
      System.out.printf(printPrefix + node.getType().getPrintName() + "\n", node.getValue());
    else
      System.out.println(printPrefix + node.getType().getPrintName());
  }
}
