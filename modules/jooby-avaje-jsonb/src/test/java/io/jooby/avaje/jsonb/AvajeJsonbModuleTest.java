/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.avaje.jsonb;

import static io.avaje.jsonb.Types.listOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.jooby.test.MockContext;

/**
 * @author ZY (kzou227@qq.com)
 */
@SuppressWarnings("unchecked")
class AvajeJsonbModuleTest {

  @Test
  void decode() throws Exception {
    var decoder = new AvajeJsonbModule();
    var ctx = new MockContext();
    ctx.setBody("[1,2,3]");

    var o = (List<Integer>) decoder.decode(ctx, listOf(Integer.class));
    assertTrue(o.containsAll(List.of(1, 2, 3)));
  }

  @Test
  void encode() {
    var decoder = new AvajeJsonbModule();
    var ctx = new MockContext();
    var o = List.of(1, 2, 3);
    var json = decoder.encode(ctx, o);
    assertEquals("[1,2,3]", json.toString(StandardCharsets.UTF_8));
  }
}
