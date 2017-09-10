package org.jooby.apitool;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import parser.RouteAttrApp;

import java.nio.file.Path;
import java.nio.file.Paths;

public class RouteAttrAppTest {

  @Test
  public void shouldExportAttributes() throws Exception {
    new RouteMethodAssert(new ApiParser(dir()).parseFully(new RouteAttrApp()))
        .next(r -> {
          r.returnType(java.util.ArrayList.class);
          r.pattern("/api/attr");
          r.description(null);
          r.summary(null);
          r.consumes("application/json");
          r.produces("application/json", "text/html");
          r.attributes(ImmutableMap.of("foo", "bar"));
        }).done();
  }

  private Path dir() {
    Path userdir = Paths.get(System.getProperty("user.dir"));
    if (!userdir.toString().endsWith("jooby-apitool")) {
      userdir = userdir.resolve("modules").resolve("jooby-apitool");
    }
    return userdir;
  }
}
