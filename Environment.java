
import java.util.HashMap;
import java.util.Map;

public class Environment {
  private Environment parent;
  private Map<String, ASTNode> nameValueMap;

  public Environment() {
    nameValueMap = new HashMap<String, ASTNode>();
  }

  public Environment getParent() {
    return parent;
  }

  public void setParent(Environment parent) {
    this.parent = parent;
  }

  // Finding the binding of the given key in the environment's mappings
  public ASTNode lookup(String key) {
    ASTNode retValue = null;
    Map<String, ASTNode> map = nameValueMap;

    retValue = map.get(key);

    if (retValue != null)
      return retValue.accept(new NodeCopier());

    if (parent != null)
      return parent.lookup(key);
    else
      return null;
  }

  public void addMapping(String key, ASTNode value) {
    nameValueMap.put(key, value);
  }
}
