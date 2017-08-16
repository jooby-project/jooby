package parser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jooby.Jooby;

public class ComplexApp extends Jooby {

  {
    get("/c1", req -> {
      Map<String, Integer> hash = new HashMap<>();
      return hash;
    });

    use(new CompApp());

    get("/c2", req -> {
      List<Foo> list = req.param("l").toList(Foo.class);
      return list;
    });

    use(MvcRoutes.class);

    get("/c3", ExtR::staticRef);

    get("/4", () -> "OK");
  }
}
