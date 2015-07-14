package org.jooby;

import static java.util.Objects.requireNonNull;

import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableSet;

/**
 * <h1>Cross Site Request Forgery handler</h1>
 *
 * <pre>
 * {
 *   use("*", new Csrf());
 * }
 * </pre>
 *
 * <p>
 * This filter require a token on <code>POST</code>, <code>PUT</code>, <code>PATCH</code> and
 * <code>DELETE</code> requests. A custom policy might be provided via:
 * {@link #requireTokenOn(Predicate)}.
 * </p>
 *
 * <p>
 * Default token generator, use a {@link UUID#randomUUID()}. A custom token generator might be
 * provided via: {@link #tokenGen(Function)}.
 * </p>
 *
 * <p>
 * Default token name is: <code>csrf</code>. If you want to use a different name, just pass the name
 * to the {@link #Csrf(String)} constructor.
 * </p>
 *
 * <h2>Token verification</h2>
 * <p>
 * The {@link Csrf} handler will read an existing token from {@link Session} (or created a new one
 * is necessary) and make available as a request local variable via:
 * {@link Request#set(String, Object)}.
 * </p>
 *
 * <p>
 * If the incoming request require a token verification, it will extract the token from:
 * </p>
 * <ol>
 * <li>HTTP header</li>
 * <li>HTTP parameter</li>
 * </ol>
 *
 * <p>
 * If the extracted token doesn't match the existing token (from {@link Session}) a <code>403</code>
 * will be thrown.
 * </p>
 *
 * @author edgar
 * @since 0.8.1
 */
public class Csrf implements Route.Filter {

  private final Set<String> REQUIRE_ON = ImmutableSet.of("POST", "PUT", "DELETE", "PATCH");

  private String name;

  private Function<Request, String> generator;

  private Predicate<Request> requireToken;

  /**
   * Creates a new {@link Csrf} handler and use the given name to save the token in the
   * {@link Session} and or extract the token from incoming requests.
   *
   * @param name Token's name.
   */
  public Csrf(final String name) {
    this.name = requireNonNull(name, "Name is required.");
    tokenGen(req -> UUID.randomUUID().toString());
    requireTokenOn(req -> REQUIRE_ON.contains(req.method()));
  }

  /**
   * Creates a new {@link Csrf} and use <code>csrf</code> as token name.
   */
  public Csrf() {
    this("csrf");
  }

  /**
   * Set a custom token generator. Default generator use: {@link UUID#randomUUID()}.
   *
   * @param generator A custom token generator.
   * @return This filter.
   */
  public Csrf tokenGen(final Function<Request, String> generator) {
    this.generator = requireNonNull(generator, "Generator is required.");
    return this;
  }

  /**
   * Decided whenever or not an incoming request require token verification. Default predicate
   * requires verification on: <code>POST</code>, <code>PUT</code>, <code>PATCH</code> and
   * <code>DELETE</code> requests.
   *
   * @param requireToken Predicate to use.
   * @return This filter.
   */
  public Csrf requireTokenOn(final Predicate<Request> requireToken) {
    this.requireToken = requireNonNull(requireToken, "RequireToken predicate is required.");
    return this;
  }

  @Override
  public void handle(final Request req, final Response rsp, final Route.Chain chain)
      throws Exception {

    /**
     * Get or generate a token
     */
    Session session = req.session();
    String token = session.get(name).toOptional().orElseGet(() -> {
      String newToken = generator.apply(req);
      session.set(name, newToken);
      return newToken;
    });

    req.set(name, token);

    if (requireToken.test(req)) {
      String candidate = req.header(name).toOptional()
          .orElseGet(() -> req.param(name).toOptional().orElse(null));
      if (!token.equals(candidate)) {
        throw new Err(Status.FORBIDDEN, "Invalid Csrf token: " + candidate);
      }
    }

    chain.next(req, rsp);
  }
}
