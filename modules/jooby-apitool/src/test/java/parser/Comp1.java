package parser;

import org.jooby.Jooby;

public class Comp1 extends Jooby {

  {
    /**
     * Comp1 doc.
     *
     * @param c1 Char value.
     * @return Char value.
     */
    get("/c1", req -> {
      return req.param("c1").charValue();
    });
  }
}
