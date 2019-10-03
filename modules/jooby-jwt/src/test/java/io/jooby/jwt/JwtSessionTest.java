package io.jooby.jwt;

import com.typesafe.config.Config;
import io.jooby.Cookie;
import io.jooby.Jooby;
import io.jooby.SessionStore;
import org.junit.jupiter.api.Test;

import java.security.Key;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JwtSessionTest {
  @Test
  public void shouldCreateWithKeyAndCookie() throws Exception {
    Key key = mock(Key.class);
    Cookie cookie = mock(Cookie.class);

    Config config = mock(Config.class);
    when(config.hasPath("session.secret")).thenReturn(false);
    when(config.hasPath("session.cookie")).thenReturn(false);

    Jooby application = mock(Jooby.class);
    when(application.getConfig()).thenReturn(config);
    when(application.setSessionStore(any(SessionStore.class))).thenReturn(application);

    new JwtSession(key, cookie)
        .install(application);
  }

  @Test
  public void shouldCreateWithStringKeyAndCookie() throws Exception {
    String key = "7a85c3b6-3ef0-4625-82d3-a1da36094804";
    Cookie cookie = mock(Cookie.class);

    Config config = mock(Config.class);
    when(config.hasPath("session.secret")).thenReturn(false);
    when(config.hasPath("session.cookie")).thenReturn(false);

    Jooby application = mock(Jooby.class);
    when(application.getConfig()).thenReturn(config);
    when(application.setSessionStore(any(SessionStore.class))).thenReturn(application);

    new JwtSession(key, cookie)
        .install(application);
  }

  @Test
  public void shouldCreateWithStringKey() throws Exception {
    String key = "7a85c3b6-3ef0-4625-82d3-a1da36094804";

    Config config = mock(Config.class);
    when(config.hasPath("session.secret")).thenReturn(false);
    when(config.hasPath("session.cookie")).thenReturn(false);

    Jooby application = mock(Jooby.class);
    when(application.getConfig()).thenReturn(config);
    when(application.setSessionStore(any(SessionStore.class))).thenReturn(application);

    new JwtSession(key)
        .install(application);
  }

  @Test
  public void shouldCreateFromConfig() throws Exception {
    String key = "7a85c3b6-3ef0-4625-82d3-a1da36094804";

    Config config = mock(Config.class);
    when(config.hasPath("session.secret")).thenReturn(true);
    when(config.getString("session.secret")).thenReturn(key);
    when(config.hasPath("session.cookie")).thenReturn(true);
    when(config.getString("session.cookie")).thenReturn("sid");

    Jooby application = mock(Jooby.class);
    when(application.getConfig()).thenReturn(config);
    when(application.setSessionStore(any(SessionStore.class))).thenReturn(application);

    new JwtSession()
        .install(application);
  }

  @Test
  public void shouldCreateFromConfigKeyMissing() throws Exception {
    Config config = mock(Config.class);
    when(config.hasPath("session.secret")).thenReturn(false);
    when(config.hasPath("session.cookie")).thenReturn(false);

    Jooby application = mock(Jooby.class);
    when(application.getConfig()).thenReturn(config);
    when(application.setSessionStore(any(SessionStore.class))).thenReturn(application);

    assertThrows(IllegalStateException.class, () -> new JwtSession()
        .install(application)
    );
  }
}
