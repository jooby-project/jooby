/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jwt;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
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

/**
 * A HTTP cookie session store using JSON Web Token. Usage:
 *
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
 * <p>This session store delegates to {@link SessionStore#signed(SessionToken, Function, Function)}
 * using JSON Web Token library.
 *
 * @author edgar
 * @since 2.2.0
 */
public class JwtSessionStore implements SessionStore {

  private final SessionStore store;

  /**
   * Creates a JSON Web Token session store. Session token is usually a {@link
   * SessionToken#signedCookie(Cookie)}, {@link SessionToken#header(String)} or combination of both.
   *
   * @param token Session token.
   * @param key Secret key.
   */
  public JwtSessionStore(@NonNull SessionToken token, @NonNull String key) {
    this(token, Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8)));
  }

  /**
   * Creates a JSON Web Token session store. Session token is usually a {@link
   * SessionToken#signedCookie(Cookie)}, {@link SessionToken#header(String)} or combination of both.
   *
   * @param token Session token.
   * @param key Secret key.
   */
  public JwtSessionStore(@NonNull SessionToken token, @NonNull SecretKey key) {
    this.store = SessionStore.signed(token, decoder(key), encoder(key));
  }

  @NonNull @Override
  public Session newSession(@NonNull Context ctx) {
    return store.newSession(ctx);
  }

  @Nullable @Override
  public Session findSession(@NonNull Context ctx) {
    return store.findSession(ctx);
  }

  @Override
  public void deleteSession(@NonNull Context ctx, @NonNull Session session) {
    store.deleteSession(ctx, session);
  }

  @Override
  public void touchSession(@NonNull Context ctx, @NonNull Session session) {
    store.touchSession(ctx, session);
  }

  @Override
  public void saveSession(@NonNull Context ctx, @NonNull Session session) {
    store.saveSession(ctx, session);
  }

  @Override
  public void renewSessionId(@NonNull Context ctx, @NonNull Session session) {
    store.renewSessionId(ctx, session);
  }

  static SneakyThrows.Function<String, Map<String, String>> decoder(SecretKey key) {
    return value -> {
      try {
        Jws<Claims> claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(value);
        Map<String, String> attributes = new HashMap<>();
        for (Map.Entry<String, Object> entry : claims.getPayload().entrySet()) {
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
