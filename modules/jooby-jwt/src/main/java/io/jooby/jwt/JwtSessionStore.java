/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jwt;

import io.jooby.Context;
import io.jooby.Cookie;
import io.jooby.Session;
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
import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * A HTTP cookie session store using JSON Web Token. Usage:
 * <pre>{@code
 * {
 *   String key = "256 bit key"
 *   setSessionStore(new JwtSession(key));
 * }
 * }</pre>
 *
 * It uses <code>HMAC-SHA-256</code> for signing the cookie. Secret key and cookie option can be
 * specify programmatically or in your application configuration file.
 *
 * This session store delegates to {@link SessionStore#signed(SessionToken, Function, Function)}
 * using JSON Web Token library.
 *
 * @author edgar
 * @since 2.2.0
 */
public class JwtSessionStore implements SessionStore {

  private final SessionStore store;

  /**
   * Creates a JSON Web Token session store. It uses a cookie token: {@link SessionToken#SID}.
   *
   * @param key Secret key.
   */
  public JwtSessionStore(@Nonnull String key) {
    this(key, SessionToken.signedCookie(SessionToken.SID));
  }

  /**
   * Creates a JSON Web Token session store. Session token is usually a
   * {@link SessionToken#signedCookie(Cookie)}, {@link SessionToken#header(String)} or combination
   * of both.
   *
   * @param key Secret key.
   * @param token Session token.
   */
  public JwtSessionStore(@Nonnull String key, @Nonnull SessionToken token) {
    this(Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8)), token);
  }

  /**
   * Creates a JSON Web Token session store. Session token is usually a
   * {@link SessionToken#signedCookie(Cookie)}, {@link SessionToken#header(String)} or combination
   * of both.
   *
   * @param key Secret key.
   * @param token Session token.
   */
  public JwtSessionStore(@Nonnull Key key, @Nonnull SessionToken token) {
    this.store = SessionStore.signed(token, decoder(key), encoder(key));
  }

  @Nonnull @Override public Session newSession(@Nonnull Context ctx) {
    return store.newSession(ctx);
  }

  @Nullable @Override public Session findSession(@Nonnull Context ctx) {
    return store.findSession(ctx);
  }

  @Override public void deleteSession(@Nonnull Context ctx, @Nonnull Session session) {
    store.deleteSession(ctx, session);
  }

  @Override public void touchSession(@Nonnull Context ctx, @Nonnull Session session) {
    store.touchSession(ctx, session);
  }

  @Override public void saveSession(@Nonnull Context ctx, @Nonnull Session session) {
    store.saveSession(ctx, session);
  }

  @Override public void renewSessionId(@Nonnull Context ctx, @Nonnull Session session) {
    store.renewSessionId(ctx, session);
  }

  static SneakyThrows.Function<String, Map<String, String>> decoder(Key key) {
    return value -> {
      try {
        Jws<Claims> claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(value);
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
