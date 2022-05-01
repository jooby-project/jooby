package io.jooby;

import java.nio.file.Path;
import java.nio.file.Paths;

public class TestSupport {

  public static Path userdir(String... segments) {
    Path path = Paths.get(System.getProperty("user.dir"));
    for (String segment : segments) {
      path = path.resolve(segment);
    }
    return path;
  }
}
