package io.jooby.json;

import io.jooby.Body;
import io.jooby.Context;
import io.jooby.MediaType;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    byte[] bytes = YassonModule.encode(ctx, user);
    assertEquals("{\"age\":2147483647,\"id\":-1,\"name\":\"Lorem €@!?\"}", new String(bytes, StandardCharsets.UTF_8));

    verify(ctx).setDefaultResponseType(MediaType.json);
  }

  @Test
  public void parse() throws IOException {
    byte[] bytes = "{\"age\":2147483647,\"id\":-1,\"name\":\"Lorem €@!?\"}".getBytes(StandardCharsets.UTF_8);
    Body body = mock(Body.class);
    when(body.stream()).thenReturn(new ByteArrayInputStream(bytes));

    Context ctx = mock(Context.class);
    when(ctx.body()).thenReturn(body);

    YassonModule YassonModule = new YassonModule();

    User user = (User) YassonModule.decode(ctx, User.class);

    assertEquals(-1, user.id);
    assertEquals(Integer.MAX_VALUE, user.age);
    assertEquals("Lorem €@!?", user.name);
  }
}
