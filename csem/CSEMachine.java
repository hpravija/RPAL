package csem;
import java.util.Stack;

import parser.AST;
import parser.ASTNode;
import parser.ASTNodeType;

public class CSEMachine {

  private Stack<ASTNode> valueStack;
  private Delta rootDelta;

  public CSEMachine(AST ast) {
    if (!ast.isStandardized())
      throw new RuntimeException("AST has NOT been standardized!");
    rootDelta = ast.createDeltas();
    rootDelta.setLinkedEnv(new Environment()); // primitive environment
    valueStack = new Stack<ASTNode>();
  }

  public void evaluateProgram() {
    processControlStack(rootDelta, rootDelta.getLinkedEnv());
  }

  private void processControlStack(Delta currentDelta, Environment currentEnv) {
    Stack<ASTNode> controlStack = new Stack<ASTNode>();
    controlStack.addAll(currentDelta.getBody());

    while (!controlStack.isEmpty())
      processCurrentNode(currentDelta, currentEnv, controlStack);
  }

  private void processCurrentNode(Delta currentDelta, Environment currentEnv, Stack<ASTNode> currentControlStack) {
    ASTNode node = currentControlStack.pop();
    if (applyBinaryOperation(node))
      return;
    else if (applyUnaryOperation(node))
      return;
    else {
      switch (node.getType()) {
        case IDENTIFIER:
          handleIdentifiers(node, currentEnv);
          break;
        case NIL:
        case TAU:
          createTuple(node);
          break;
        case BETA:
          handleBeta((Beta) node, currentControlStack);
          break;
        case GAMMA:
          applyGamma(currentDelta, node, currentEnv, currentControlStack);
          break;
        case DELTA:
          ((Delta) node).setLinkedEnv(currentEnv); // CSE rule 2
          valueStack.push(node);
          break;
        default:
          valueStack.push(node);
          break;
      }
    }
  }

  // CSE rule 6
  private boolean applyBinaryOperation(ASTNode rator) {
    switch (rator.getType()) {
      case PLUS:
      case MINUS:
      case MULT:
      case DIV:
      case EXP:
      case LS:
      case LE:
      case GR:
      case GE:
        binaryArithmeticOp(rator.getType());
        return true;
      case EQ:
      case NE:
        binaryLogicalEqNeOp(rator.getType());
        return true;
      case OR:
      case AND:
        binaryLogicalOrAndOp(rator.getType());
        return true;
      case AUG:
        augTuples();
        return true;
      default:
        return false;
    }
  }

  private void binaryArithmeticOp(ASTNodeType type) {
    ASTNode rand1 = valueStack.pop();
    ASTNode rand2 = valueStack.pop();

    ASTNode result = new ASTNode();
    result.setType(ASTNodeType.INTEGER);

    switch (type) {
      case PLUS:
        result.setValue(Integer.toString(Integer.parseInt(rand1.getValue()) + Integer.parseInt(rand2.getValue())));
        break;
      case MINUS:
        result.setValue(Integer.toString(Integer.parseInt(rand1.getValue()) - Integer.parseInt(rand2.getValue())));
        break;
      case MULT:
        result.setValue(Integer.toString(Integer.parseInt(rand1.getValue()) * Integer.parseInt(rand2.getValue())));
        break;
      case DIV:
        result.setValue(Integer.toString(Integer.parseInt(rand1.getValue()) / Integer.parseInt(rand2.getValue())));
        break;
      case EXP:
        result.setValue(
            Integer.toString((int) Math.pow(Integer.parseInt(rand1.getValue()), Integer.parseInt(rand2.getValue()))));
        break;
      case LS:
        if (Integer.parseInt(rand1.getValue()) < Integer.parseInt(rand2.getValue()))
          pushTrueNode();
        else
          pushFalseNode();
        return;
      case LE:
        if (Integer.parseInt(rand1.getValue()) <= Integer.parseInt(rand2.getValue()))
          pushTrueNode();
        else
          pushFalseNode();
        return;
      case GR:
        if (Integer.parseInt(rand1.getValue()) > Integer.parseInt(rand2.getValue()))
          pushTrueNode();
        else
          pushFalseNode();
        return;
      case GE:
        if (Integer.parseInt(rand1.getValue()) >= Integer.parseInt(rand2.getValue()))
          pushTrueNode();
        else
          pushFalseNode();
        return;
      default:
        break;
    }
    valueStack.push(result);
  }

  private void binaryLogicalEqNeOp(ASTNodeType type) {
    ASTNode rand1 = valueStack.pop();
    ASTNode rand2 = valueStack.pop();

    if (rand1.getType() == ASTNodeType.TRUE || rand1.getType() == ASTNodeType.FALSE) {
      compareTruthValues(rand1, rand2, type);
      return;
    }

    if (rand1.getType() == ASTNodeType.STRING)
      compareStrings(rand1, rand2, type);
    else if (rand1.getType() == ASTNodeType.INTEGER)
      compareIntegers(rand1, rand2, type);

  }

  private void compareTruthValues(ASTNode rand1, ASTNode rand2, ASTNodeType type) {
    if (rand1.getType() == rand2.getType())
      if (type == ASTNodeType.EQ)
        pushTrueNode();
      else
        pushFalseNode();
    else if (type == ASTNodeType.EQ)
      pushFalseNode();
    else
      pushTrueNode();
  }

  private void compareStrings(ASTNode rand1, ASTNode rand2, ASTNodeType type) {
    if (rand1.getValue().equals(rand2.getValue()))
      if (type == ASTNodeType.EQ)
        pushTrueNode();
      else
        pushFalseNode();
    else if (type == ASTNodeType.EQ)
      pushFalseNode();
    else
      pushTrueNode();
  }

  private void compareIntegers(ASTNode rand1, ASTNode rand2, ASTNodeType type) {
    if (Integer.parseInt(rand1.getValue()) == Integer.parseInt(rand2.getValue()))
      if (type == ASTNodeType.EQ)
        pushTrueNode();
      else
        pushFalseNode();
    else if (type == ASTNodeType.EQ)
      pushFalseNode();
    else
      pushTrueNode();
  }

  private void binaryLogicalOrAndOp(ASTNodeType type) {
    ASTNode rand1 = valueStack.pop();
    ASTNode rand2 = valueStack.pop();

    if ((rand1.getType() == ASTNodeType.TRUE || rand1.getType() == ASTNodeType.FALSE) &&
        (rand2.getType() == ASTNodeType.TRUE || rand2.getType() == ASTNodeType.FALSE)) {
      orAndTruthValues(rand1, rand2, type);
      return;
    }

  }

  private void orAndTruthValues(ASTNode rand1, ASTNode rand2, ASTNodeType type) {
    if (type == ASTNodeType.OR) {
      if (rand1.getType() == ASTNodeType.TRUE || rand2.getType() == ASTNodeType.TRUE)
        pushTrueNode();
      else
        pushFalseNode();
    } else {
      if (rand1.getType() == ASTNodeType.TRUE && rand2.getType() == ASTNodeType.TRUE)
        pushTrueNode();
      else
        pushFalseNode();
    }
  }

  private void augTuples() {
    ASTNode rand1 = valueStack.pop();
    ASTNode rand2 = valueStack.pop();


    ASTNode childNode = rand1.getChild();
    if (childNode == null)
      rand1.setChild(rand2);
    else {
      while (childNode.getSibling() != null)
        childNode = childNode.getSibling();
      childNode.setSibling(rand2);
    }
    rand2.setSibling(null);

    valueStack.push(rand1);
  }

  // CSE rule 7
  private boolean applyUnaryOperation(ASTNode rator) {
    switch (rator.getType()) {
      case NOT:
        not();
        return true;
      case NEG:
        neg();
        return true;
      default:
        return false;
    }
  }

  private void not() {
    ASTNode rand = valueStack.pop();

    if (rand.getType() == ASTNodeType.TRUE)
      pushFalseNode();
    else
      pushTrueNode();
  }

  private void neg() {
    ASTNode rand = valueStack.pop();

    ASTNode result = new ASTNode();
    result.setType(ASTNodeType.INTEGER);
    result.setValue(Integer.toString(-1 * Integer.parseInt(rand.getValue())));
    valueStack.push(result);
  }

  // CSE rule 3
  private void applyGamma(Delta currentDelta, ASTNode node, Environment currentEnv,
      Stack<ASTNode> currentControlStack) {
    ASTNode rator = valueStack.pop();
    ASTNode rand = valueStack.pop();

    if (rator.getType() == ASTNodeType.DELTA) {
      Delta nextDelta = (Delta) rator;
      Environment newEnv = new Environment();
      newEnv.setParent(nextDelta.getLinkedEnv());

      // CSE rule 4
      if (nextDelta.getBoundVars().size() == 1) {
        newEnv.addMapping(nextDelta.getBoundVars().get(0), rand);
      }
      // CSE rule 11
      else {

        for (int i = 0; i < nextDelta.getBoundVars().size(); i++) {
          newEnv.addMapping(nextDelta.getBoundVars().get(i), getNthTupleChild((Tuple) rand, i + 1));
        }
      }

      processControlStack(nextDelta, newEnv);
      return;
    } else if (rator.getType() == ASTNodeType.YSTAR) {
      // CSE rule 12

      Eta etaNode = new Eta();
      etaNode.setDelta((Delta) rand);
      valueStack.push(etaNode);
      return;
    } else if (rator.getType() == ASTNodeType.ETA) {
      // CSE rule 13
      valueStack.push(rand);
      valueStack.push(rator);
      valueStack.push(((Eta) rator).getDelta());
      currentControlStack.push(node);
      currentControlStack.push(node);
      return;
    } else if (rator.getType() == ASTNodeType.TUPLE) {
      tupleSelection((Tuple) rator, rand);
      return;
    } else if (evaluateReservedIdentifiers(rator, rand, currentControlStack))
      return;
  }

  private boolean evaluateReservedIdentifiers(ASTNode rator, ASTNode rand, Stack<ASTNode> currentControlStack) {
    switch (rator.getValue()) {
      case "Isinteger":
        checkTypeAndPushTrueOrFalse(rand, ASTNodeType.INTEGER);
        return true;
      case "Isstring":
        checkTypeAndPushTrueOrFalse(rand, ASTNodeType.STRING);
        return true;
      case "Isdummy":
        checkTypeAndPushTrueOrFalse(rand, ASTNodeType.DUMMY);
        return true;
      case "Isfunction":
        checkTypeAndPushTrueOrFalse(rand, ASTNodeType.DELTA);
        return true;
      case "Istuple":
        checkTypeAndPushTrueOrFalse(rand, ASTNodeType.TUPLE);
        return true;
      case "Istruthvalue":
        if (rand.getType() == ASTNodeType.TRUE || rand.getType() == ASTNodeType.FALSE)
          pushTrueNode();
        else
          pushFalseNode();
        return true;
      case "Stem":
        stem(rand);
        return true;
      case "Stern":
        stern(rand);
        return true;
      case "Conc":
      case "conc":
        conc(rand, currentControlStack);
        return true;
      case "Print":
      case "print":
        printNodeValue(rand);
        pushDummyNode();
        return true;
      case "ItoS":
        itos(rand);
        return true;
      case "Order":
        order(rand);
        return true;
      case "Null":
        isNullTuple(rand);
        return true;
      default:
        return false;
    }
  }

  private void checkTypeAndPushTrueOrFalse(ASTNode rand, ASTNodeType type) {
    if (rand.getType() == type)
      pushTrueNode();
    else
      pushFalseNode();
  }

  private void pushTrueNode() {
    ASTNode trueNode = new ASTNode();
    trueNode.setType(ASTNodeType.TRUE);
    trueNode.setValue("true");
    valueStack.push(trueNode);
  }

  private void pushFalseNode() {
    ASTNode falseNode = new ASTNode();
    falseNode.setType(ASTNodeType.FALSE);
    falseNode.setValue("false");
    valueStack.push(falseNode);
  }

  private void pushDummyNode() {
    ASTNode falseNode = new ASTNode();
    falseNode.setType(ASTNodeType.DUMMY);
    valueStack.push(falseNode);
  }

  private void stem(ASTNode rand) {

    if (rand.getValue().isEmpty())
      rand.setValue("");
    else
      rand.setValue(rand.getValue().substring(0, 1));

    valueStack.push(rand);
  }

  private void stern(ASTNode rand) {

    if (rand.getValue().isEmpty() || rand.getValue().length() == 1)
      rand.setValue("");
    else
      rand.setValue(rand.getValue().substring(1));

    valueStack.push(rand);
  }

  private void conc(ASTNode rand1, Stack<ASTNode> currentControlStack) {
    currentControlStack.pop();
    ASTNode rand2 = valueStack.pop();

    ASTNode result = new ASTNode();
    result.setType(ASTNodeType.STRING);
    result.setValue(rand1.getValue() + rand2.getValue());

    valueStack.push(result);
  }

  private void itos(ASTNode rand) {
    rand.setType(ASTNodeType.STRING);
    valueStack.push(rand);
  }

  private void order(ASTNode rand) {

    ASTNode result = new ASTNode();
    result.setType(ASTNodeType.INTEGER);
    result.setValue(Integer.toString(getNumChildren(rand)));

    valueStack.push(result);
  }

  private void isNullTuple(ASTNode rand) {

    if (getNumChildren(rand) == 0)
      pushTrueNode();
    else
      pushFalseNode();
  }

  // CSE rule 10
  private void tupleSelection(Tuple rator, ASTNode rand) {

    ASTNode result = getNthTupleChild(rator, Integer.parseInt(rand.getValue()));

    valueStack.push(result);
  }

  // Taking nth element of the tuple
  private ASTNode getNthTupleChild(Tuple tupleNode, int n) {
    ASTNode childNode = tupleNode.getChild();
    for (int i = 1; i < n; ++i) { // index starting from 1
      if (childNode == null)
        break;
      childNode = childNode.getSibling();
    }
    return childNode;
  }

  private void handleIdentifiers(ASTNode node, Environment currentEnv) {
    if (currentEnv.lookup(node.getValue()) != null) // CSE rule 1
      valueStack.push(currentEnv.lookup(node.getValue()));
    else if (isReservedIdentifier(node.getValue()))
      valueStack.push(node);
  }

  // CSE rule 9
  private void createTuple(ASTNode node) {
    int numChildren = getNumChildren(node);
    Tuple tupleNode = new Tuple();
    if (numChildren == 0) {
      valueStack.push(tupleNode);
      return;
    }

    ASTNode childNode = null, tempNode = null;
    for (int i = 0; i < numChildren; ++i) {
      if (childNode == null)
        childNode = valueStack.pop();
      else if (tempNode == null) {
        tempNode = valueStack.pop();
        childNode.setSibling(tempNode);
      } else {
        tempNode.setSibling(valueStack.pop());
        tempNode = tempNode.getSibling();
      }
    }
    tempNode.setSibling(null);
    tupleNode.setChild(childNode);
    valueStack.push(tupleNode);
  }

  // CSE rule 8
  private void handleBeta(Beta node, Stack<ASTNode> currentControlStack) {
    ASTNode conditionResultNode = valueStack.pop();

    if (conditionResultNode.getType() == ASTNodeType.TRUE)
      currentControlStack.addAll(node.getThenPart());
    else
      currentControlStack.addAll(node.getElsePart());
  }

  private int getNumChildren(ASTNode node) {
    int numChildren = 0;
    ASTNode childNode = node.getChild();
    while (childNode != null) {
      numChildren++;
      childNode = childNode.getSibling();
    }
    return numChildren;
  }

  private void printNodeValue(ASTNode rand) {
    String evaluationResult = rand.getValue();
    evaluationResult = evaluationResult.replace("\\t", "\t");
    evaluationResult = evaluationResult.replace("\\n", "\n");
    System.out.print(evaluationResult);
  }

  private boolean isReservedIdentifier(String value) {
    switch (value) {
      case "Isinteger":
      case "Isstring":
      case "Istuple":
      case "Isdummy":
      case "Istruthvalue":
      case "Isfunction":
      case "ItoS":
      case "Order":
      case "Conc":
      case "conc":
      case "Stern":
      case "Stem":
      case "Null":
      case "Print":
      case "print":
      case "neg":
        return true;
    }
    return false;
  }

}