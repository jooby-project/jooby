package io.jooby.run;

import io.jooby.Server;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServerRefTest {
  @Test
  public void stop() throws Exception {
    Server server = mock(Server.class);
    when(server.stop()).thenReturn(server);

    ServerRef ref = new ServerRef();
    ref.accept(server);

    ServerRef.stop();
  }

  @Test
  @DisplayName("ServerRef.stop is invoked using reflection in JoobyRun")
  public void stopMethodName() throws Exception {
    Method stop = ServerRef.class.getDeclaredMethod(JoobyRun.SERVER_REF_STOP);
    assertTrue(Modifier.isPublic(stop.getModifiers()));
    assertTrue(Modifier.isStatic(stop.getModifiers()));
    assertEquals(0, stop.getParameterCount());
  }

  @Test
  @DisplayName("ServerRef must have a default constructor which is required by Jooby hook")
  public void makeDefaultConstructorExists() throws Exception {
    Constructor<ServerRef> constructor = ServerRef.class.getDeclaredConstructor();
    assertTrue(Modifier.isPublic(constructor.getModifiers()));
    assertEquals(0, constructor.getParameterCount());
  }
}
