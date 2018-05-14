package parser;

import org.jooby.Jooby;
import org.jooby.Results;

public class MethodRefApp extends Jooby {

  {
    ExtR ext = new ExtR();

    get("/e1", ext::external);

    get("/l1", req -> {
      return Results.ok();
    });

    get("/e2", ext::returnType);
  }

}
