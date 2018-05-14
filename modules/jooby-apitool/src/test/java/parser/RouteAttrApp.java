package parser;

import com.google.common.collect.Lists;
import org.jooby.Jooby;

public class RouteAttrApp extends Jooby {
  {
    use("/api/attr")
        .get(() -> Lists.newArrayList())
        .attr("foo", "bar")
        .consumes("json")
        .produces("json", "html");
  }
}
