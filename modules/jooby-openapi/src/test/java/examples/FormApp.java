package examples;

import io.jooby.Jooby;

public class FormApp extends Jooby {

  {
    post("/single", ctx -> {
      ctx.form("name").value();
      return "...";
    });

    post("/multiple", ctx -> {
      ctx.form("firstname").value();
      ctx.form("lastname").value();
      return "...";
    });

    post("/bean", ctx -> {
      ABean form = ctx.form(ABean.class);
      return "...";
    });
  }
}
