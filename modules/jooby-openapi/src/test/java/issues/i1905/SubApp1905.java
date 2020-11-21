package issues.i1905;

import io.jooby.Jooby;

public class SubApp1905 extends Jooby {
  {
    get("/sub", ctx -> "OK");
  }
}
