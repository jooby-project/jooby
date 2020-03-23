package issues.i1592;

import io.jooby.Jooby;

public class App1592 extends Jooby {

  {
    post("/nested", ctx -> ctx.body(FairData.class));
  }
}
