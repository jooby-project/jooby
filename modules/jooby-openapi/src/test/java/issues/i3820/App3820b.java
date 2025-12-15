/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3820;

import java.util.ArrayList;
import java.util.List;

import io.jooby.Jooby;

public class App3820b extends Jooby {
  {
    get(
        "/strings",
        ctx -> {
          List<String> strings = new ArrayList<>();
          return strings;
        });
  }
}
