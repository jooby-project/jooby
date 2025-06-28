/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.yasson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import io.jooby.Body;
import io.jooby.Context;
import io.jooby.MediaType;
import io.jooby.output.BufferOptions;
import io.jooby.output.BufferedOutputFactory;

public class YassonModuleTest {

  public static class User {
    public long id;
    public String name;
    public int age;
  }

  @Test
  public void render() {

    YassonModule YassonModule = new YassonModule();
    User user = new User();
    user.id = -1;
    user.name = "Lorem €@!?";
    user.age = Integer.MAX_VALUE;

    Context ctx = mock(Context.class);
    when(ctx.getOutputFactory()).thenReturn(BufferedOutputFactory.create(BufferOptions.small()));
    var buffer = YassonModule.encode(ctx, user);
    assertEquals(
        "{\"age\":2147483647,\"id\":-1,\"name\":\"Lorem €@!?\"}",
        buffer.asString(StandardCharsets.UTF_8));

    verify(ctx).setDefaultResponseType(MediaType.json);
  }

  @Test
  public void parse() throws IOException {
    byte[] bytes =
        "{\"age\":2147483647,\"id\":-1,\"name\":\"Lorem\"}".getBytes(StandardCharsets.UTF_8);
    Body body = mock(Body.class);
    when(body.stream()).thenReturn(new ByteArrayInputStream(bytes));

    Context ctx = mock(Context.class);
    when(ctx.body()).thenReturn(body);

    YassonModule YassonModule = new YassonModule();

    User user = (User) YassonModule.decode(ctx, User.class);

    assertEquals(-1, user.id);
    assertEquals(Integer.MAX_VALUE, user.age);
    assertEquals("Lorem", user.name.trim());
  }
}
