package apps;

import com.google.common.base.MoreObjects;

import java.util.Optional;

public class Param1096 {
  public String param1;
  public int param2;
  public String param3;
  public Optional<String> param4;
  public String param5;
  public Param1096b nested;

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("param1", param1)
        .add("param2", param2)
        .add("param3", param3)
        .add("param4", param4)
        .add("param5", param5)
        .toString();
  }
}
