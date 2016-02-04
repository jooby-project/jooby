package apps;

import java.util.ArrayList;
import java.util.List;

import org.jooby.Jooby;

public class ReturnNewGenericObjectVar extends Jooby {

  {
    get("/", req -> {
      List<LocalType> var = new ArrayList<>();
      return var;
    });
  }

}
