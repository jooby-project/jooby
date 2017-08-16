package parser;

import org.jooby.Jooby;

public class CompApp extends Jooby {

  {
    use(new Comp1());

    /**
     * M1 API.
     *
     * @param m1 M1 argument.
     * @return Number 33.
     */
    get("/m1", req -> {
      return req.param("m1").byteValue((byte) 33);
    });

    use("/comp2", new Comp2());
  }

}
