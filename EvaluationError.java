public class EvaluationError {

  public static void printError(int sourceLineNumber, String message) {
    System.out.println(rpal20.fileName + ":" + sourceLineNumber + ": " + message);
    System.exit(1);
  }

}
