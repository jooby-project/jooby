package parser;

import java.util.List;

import org.jooby.Jooby;

public class FormAndBodyApp extends Jooby {

  {
    post("/f1", req -> {
      Foo foo = req.body(Foo.class);
      return foo;
    });

    post("/f2", req -> {
      Foo foo = req.form(Foo.class);
      return foo;
    });

    post("/f3", req -> {
      Foo foo = req.body().to(Foo.class);
      return foo;
    });

    post("/f4", req -> {
      List<Foo> foo = req.body().toList(Foo.class);
      return foo;
    });
  }

}
