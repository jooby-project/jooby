/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.handler;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.Route;
import io.jooby.Router;
import io.jooby.Session;
import io.jooby.exception.InvalidCsrfToken;

/**
 * Cross Site Request Forgery handler.
 *
 * <pre>
 * {
 *   before(new CsrfHandler());
 * }
 * </pre>
 *
 * <p>This filter require a token on <code>POST</code>, <code>PUT</code>, <code>PATCH</code> and
 * <code>DELETE</code> requests. A custom policy might be provided via: {@link
 * #setRequestFilter(Predicate)}.
 *
 * <p>Default token generator, use a {@link UUID#randomUUID()}. A custom token generator might be
 * provided via: {@link #setTokenGenerator(Function)}.
 *
 * <p>Default token name is: <code>csrf</code>. If you want to use a different name, just pass the
 * name to the {@link #CsrfHandler(String)} constructor.
 *
 * <h2>Token verification</h2>
 *
 * <p>The {@link CsrfHandler} handler will read an existing token from {@link Session} (or created a
 * new one is necessary) and make available as a request local variable via: {@link
 * Context#setAttribute(String, Object)}.
 *
 * <p>If the incoming request require a token verification, it will extract the token from:
 *
 * <ol>
 *   <li>HTTP header
 *   <li>HTTP cookie
 *   <li>HTTP parameter (query or form)
 * </ol>
 *
 * <p>If the extracted token doesn't match the existing token (from {@link Session}) a <code>403
 * </code> will be thrown.
 *
 * @author edgar
 * @since 2.5.2
 */
public class CsrfHandler implements Route.Before {

  /**
   * Default request filter. Requires an existing session and only check for POST, DELETE, PUT and
   * PATCH methods.
   */
  public static final Predicate<Context> DEFAULT_FILTER =
      ctx -> {
        return Router.POST.equals(ctx.getMethod())
            || Router.DELETE.equals(ctx.getMethod())
            || Router.PATCH.equals(ctx.getMethod())
            || Router.PUT.equals(ctx.getMethod());
      };

  /** UUID token generator. */
  public static final Function<Context, String> DEFAULT_GENERATOR =
      ctx -> UUID.randomUUID().toString();

  private final String name;

  private Function<Context, String> generator = DEFAULT_GENERATOR;

  private Predicate<Context> filter = DEFAULT_FILTER;

  /**
   * Creates a new {@link CsrfHandler} handler and use the given name to save the token in the
   * {@link Session} and or extract the token from incoming requests.
   *
   * @param name Token's name.
   */
  public CsrfHandler(String name) {
    this.name = name;
  }

  /**
   * Creates a new {@link CsrfHandler} handler and use the given name to save the token in the
   * {@link Session} and or extract the token from incoming requests.
   */
  public CsrfHandler() {
    this("csrf");
  }

  @Override
  public void apply(@NonNull Context ctx) throws Exception {

    Session session = ctx.session();
    String token =
        session
            .get(name)
            .toOptional()
            .orElseGet(
                () -> {
                  String newToken = generator.apply(ctx);
                  session.put(name, newToken);
                  return newToken;
                });

    ctx.setAttribute(name, token);

    if (filter.test(ctx)) {
      String clientToken =
          Stream.of(
                  ctx.header(name).valueOrNull(),
                  ctx.cookie(name).valueOrNull(),
                  ctx.form(name).valueOrNull(),
                  ctx.query(name).valueOrNull())
              .filter(Objects::nonNull)
              .findFirst()
              .orElse(null);
      if (!token.equals(clientToken)) {
        throw new InvalidCsrfToken(clientToken);
      }
    }
  }

  /**
   * Set a custom token generator. Default generator use: {@link UUID#randomUUID()}.
   *
   * @param generator A custom token generator.
   * @return This filter.
   */
  public @NonNull CsrfHandler setTokenGenerator(@NonNull Function<Context, String> generator) {
    this.generator = generator;
    return this;
  }

  /**
   * Decided whenever or not an incoming request require token verification. Default predicate
   * requires verification on: <code>POST</code>, <code>PUT</code>, <code>PATCH</code> and <code>
   * DELETE</code> requests.
   *
   * @param filter Predicate to use.
   * @return This filter.
   */
  public @NonNull CsrfHandler setRequestFilter(@NonNull Predicate<Context> filter) {
    this.filter = filter;
    return this;
  }
}
