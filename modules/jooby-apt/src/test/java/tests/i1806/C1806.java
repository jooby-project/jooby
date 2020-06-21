package tests.i1806;

import io.jooby.annotations.GET;
import io.jooby.annotations.QueryParam;

import java.util.List;

public class C1806 {
  @GET("/1806/c")
  public List<String> sayHi(@QueryParam List<String> names) {
    return names;
  }

}
