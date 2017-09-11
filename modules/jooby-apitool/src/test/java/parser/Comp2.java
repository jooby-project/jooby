package parser;

import org.jooby.Jooby;

public class Comp2 extends Jooby {

  {
    get("/c2", req -> {
      return req.param("c2").charValue();
    });
  }
}
