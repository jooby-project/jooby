package apps;

import org.jooby.Jooby;

public class VarApp {

  public static void main(String[] args) {
    Jooby app = new Jooby();

    /**
     * Tag doc.
     */
    app.get("/tag/:id", req -> {
      req.param("id").value();
      return new Tag();
    });

    app.start(args);
  }
}
