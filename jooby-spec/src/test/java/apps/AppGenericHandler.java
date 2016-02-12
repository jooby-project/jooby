package apps;

import org.jooby.Jooby;

public class AppGenericHandler extends Jooby {

  {

    use("/1", (req, rsp) -> {});

    use("*", "/2", (req, rsp) -> {});
  }
}
