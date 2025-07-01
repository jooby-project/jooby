/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.graphiql;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class GraphiqlPageTest {
  @Test
  public void checkGraphiqlPage() throws Exception {
    var path = "http://localhost:8080/graphiql";
    var content = GraphiqlPage.index(path);
    assertTrue(content.contains(path));
  }
}
