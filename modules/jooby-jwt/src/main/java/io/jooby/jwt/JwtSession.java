package io.jooby.jwt;

import com.typesafe.config.Config;
import io.jooby.Cookie;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.SessionStore;
import io.jooby.SessionToken;
import io.jooby.SneakyThrows;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.annotation.Nonnull;
import java.security.Key;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A HTTP cookie session store using JSON Web Token. Usage:
 * <pre>{@code
 * {
 *   install(new JwtSession());
 * }
 * }</pre>
 *
 * It uses <code>HMAC-SHA-256</code> for signing the cookie. Secret key and cookie option can be
 * specify programmatically or in your application configuration file.
 *
 * @author edgar
 * @since 2.2.0
 */
public class JwtSession implements Extension {

  private final Key key;

  private final Cookie cookie;

  /**
   * Creates a JSON Web Token session store. The <code>session.secret</code> property must be
   * defined in your application configuration.
   *
   * Cookie details are created from <code>session.cookie</code> when present, otherwise uses
   * {@link SessionToken#SID}.
   */
  public JwtSession() {
    this.key = null;
    this.cookie = null;
  }

  /**
   * Creates a JSON Web Token session store. It uses the provided key unless the
   * <code>session.secret</code> property is present in your application configuration.
   *
   * Cookie details are created from <code>session.cookie</code> when present, otherwise uses
   * {@link SessionToken#SID}.
   *
   * @param key Key to use. Override it by <code>session.secret</code> property.
   */
  public JwtSession(@Nonnull String key) {
    this(key, SessionToken.SID);
  }

  /**
   * Creates a JSON Web Token session store. It uses the provided key unless the
   * <code>session.secret</code> property is present in your application configuration.
   *
   * Cookie details are created from <code>session.cookie</code> when present, otherwise uses
   * the provided cookie.
   *
   * See {@link Cookie#create(String, Config)}.
   *
   * @param key Key to use. Override it by <code>session.secret</code> property.
   * @param cookie Cookie to use. Override it by <code>session.cookie</code> property.
   */
  public JwtSession(@Nonnull String key, @Nonnull Cookie cookie) {
    this(Keys.hmacShaKeyFor(key.getBytes()), cookie);
  }

  /**
   * Creates a JSON Web Token session store. It uses the provided key unless the
   * <code>session.secret</code> property is present in your application configuration.
   *
   * Cookie details are created from <code>session.cookie</code> when present, otherwise uses
   * the provided cookie.
   *
   * See {@link Cookie#create(String, Config)}.
   *
   * @param key Key to use. Override it by <code>session.secret</code> property.
   * @param cookie Cookie to use. Override it by <code>session.cookie</code> property.
   */
  public JwtSession(@Nonnull Key key, @Nonnull Cookie cookie) {
    this.key = key;
    this.cookie = cookie;
  }

  @Override public void install(@Nonnull Jooby application) throws Exception {
    Config config = application.getConfig();
    Key key = config.hasPath("session.secret")
        ? Keys.hmacShaKeyFor(config.getString("session.secret").getBytes())
        : this.key;
    if (key == null) {
      throw new IllegalStateException("No secret session secret key");
    }
    Cookie cookie = Cookie.create("session.cookie", config)
        .orElse(Optional.ofNullable(this.cookie).orElse(SessionToken.SID));

    application.setSessionStore(SessionStore.cookie(cookie, decoder(key), encoder(key)));
  }

  static SneakyThrows.Function<String, Map<String, String>> decoder(Key key) {
    return value -> {
      try {
        Jws<Claims> claims = Jwts.parser().setSigningKey(key).parseClaimsJws(value);
        Map<String, String> attributes = new HashMap<>();
        for (Map.Entry<String, Object> entry : claims.getBody().entrySet()) {
          attributes.put(entry.getKey(), entry.getValue().toString());
        }
        return attributes;
      } catch (JwtException x) {
        return null;
      }
    };
  }

  static SneakyThrows.Function<Map<String, String>, String> encoder(Key key) {
    return attributes -> {
      JwtBuilder builder = Jwts.builder().signWith(key);
      for (Map.Entry<String, String> entry : attributes.entrySet()) {
        builder.claim(entry.getKey(), entry.getValue());
      }
      return builder.compact();
    };
  }
}
