package app;

import org.jooby.Jooby;
import org.jooby.Results;
import org.jooby.thymeleaf.Thl;

public class ThymeleafApp extends Jooby{
  {
    use(new Thl("", ".html"));

    get("/", req -> {
      return Results.html("thl")
          .put("who", req.param("who").value("World"))
          .put("unsafe", "<p>paragraph</p>");
    });
  }

  public static void main(String[] args) {
    run(ThymeleafApp::new, args);
  }
}
