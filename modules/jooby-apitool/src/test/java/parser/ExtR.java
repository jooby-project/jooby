package parser;

import java.util.Arrays;
import java.util.List;

import org.jooby.Request;

public class ExtR {

  public List<String> external(final Request req) {
    String p = req.param("foo").value("bar");
    return Arrays.asList(p);
  }

  public List<String> external(final Foo foo) {
    return Arrays.asList();
  }

  public Foo returnType(final Request req) throws Exception {
    return req.body(Foo.class);
  }

  public static List<String> staticRef(final Request req) {
    String p = req.param("foo").value("bar");
    return Arrays.asList(p);
  }
}
