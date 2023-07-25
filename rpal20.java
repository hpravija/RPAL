import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

// Main class
public class rpal20 {

  public static String fileName;

  public static void main(String[] args) {
    boolean listFlag = false;
    boolean astFlag = false;
    boolean stFlag = false;
    boolean noOutFlag = false;
    fileName = "";
    AST ast = null;

    for (String cmdOption : args) {
      if (cmdOption.equals("-help")) {
        printHelp();
        return;
      } else if (cmdOption.equals("-l"))
        listFlag = true;
      else if (cmdOption.equals("-ast"))
        astFlag = true;
      else if (cmdOption.equals("-st"))
        stFlag = true;
      else if (cmdOption.equals("-noout"))
        noOutFlag = true;
      else
        fileName = cmdOption;
    }

    // only prints the result
    if (!listFlag && !astFlag && !stFlag && !noOutFlag) {
      ast = buildAST(fileName, true);
      ast.standardize();
      evaluateST(ast);
      return;
    }

    if (listFlag) {
      if (fileName.isEmpty())
        throw new ParseException("Input a relavant file. Call RPAL20 with -help to clarify");
      printInputListing(fileName);
    }

    if (astFlag) {
      if (fileName.isEmpty())
        throw new ParseException("Input a relavant file. Call RPAL20 with -help to clarify");
      ast = buildAST(fileName, true);
      printAST(ast);
      if (noOutFlag)
        return;
      ast.standardize();
      evaluateST(ast);
    }

    if (stFlag) {
      if (fileName.isEmpty())
        throw new ParseException("Input a relavant file. Call RPAL20 with -help to clarify");
      ast = buildAST(fileName, true);
      ast.standardize();
      printAST(ast);
      if (noOutFlag)
        return;
      evaluateST(ast);
    }

    // -noout without -ast or -st produces no output
    if (noOutFlag && (!astFlag || !stFlag)) {
      if (fileName.isEmpty())
        throw new ParseException("Input a relavant file. Call RPAL20 with -help to clarify");
    }

  }

  private static void evaluateST(AST ast) {
    CSEMachine csem = new CSEMachine(ast);
    csem.evaluateProgram();
    System.out.println();
  }

  private static void printInputListing(String fileName) {
    BufferedReader buffer = null;
    try {
      buffer = new BufferedReader(new InputStreamReader(new FileInputStream(new File(fileName))));
      String s = "";
      while ((s = buffer.readLine()) != null) {
        System.out.println(s);
      }
    } catch (FileNotFoundException e) {
      throw new ParseException("File " + fileName + " not found.");
    } catch (IOException e) {
      throw new ParseException("Error reading from file " + fileName);
    } finally {
      try {
        if (buffer != null)
          buffer.close();
      } catch (IOException e) {
      }
    }
  }

  private static AST buildAST(String fileName, boolean printOutput) {
    AST ast = null;
    try {
      Scanner scanner = new Scanner(fileName);
      Parser parser = new Parser(scanner);
      ast = parser.buildAST();
    } catch (IOException e) {
      throw new ParseException("ERROR: Could not read from file: " + fileName);
    }
    return ast;
  }

  private static void printAST(AST ast) {
    ast.print();
  }

  private static void printHelp() {
    System.out.println("Usage: java rpal20 [OPTIONS] file");
    System.out.println("Without any switches, print only the result of evaluating the program");
    System.out.println("  -ast: print ast followed by the result");
    System.out.println("        of evaluating the program");
    System.out.println("        with -noout, print only the ast");
    System.out.println("   -st: print sst generated after the result");
    System.out.println("        of evaluating the program");
    System.out.println("        with -noout, prints only the sst");
    System.out.println("    -l: print source code");
  }

}
