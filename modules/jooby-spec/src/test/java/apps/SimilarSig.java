package apps;

import org.jooby.Jooby;

public class SimilarSig extends Jooby {

  {

    // doc1
    get("/1", () -> {
      SomeLambda l = new SomeLambda();
      Object value = l.get("/x", req -> "Hi");
      return value;
    });

    // doc2
    get("/2", () -> {
      SomeLambda l = new SomeLambda();
      Object value = l.get("/x", req -> "Hi");
      return value;
    });
  }

}
