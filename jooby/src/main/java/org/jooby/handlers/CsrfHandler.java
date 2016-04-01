/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.handlers;

import static java.util.Objects.requireNonNull;

import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jooby.Err;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Session;
import org.jooby.Status;

import com.google.common.collect.ImmutableSet;

/**
 * <h1>Cross Site Request Forgery handler</h1>
 *
 * <pre>
 * {
 *   use("*", new CsrfHandler());
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
 * to the {@link #CsrfHandler(String)} constructor.
 * </p>
 *
 * <h2>Token verification</h2>
 * <p>
 * The {@link CsrfHandler} handler will read an existing token from {@link Session} (or created a
 * new one
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
public class CsrfHandler implements Route.Filter {

  private final Set<String> REQUIRE_ON = ImmutableSet.of("POST", "PUT", "DELETE", "PATCH");

  private String name;

  private Function<Request, String> generator;

  private Predicate<Request> requireToken;

  /**
   * Creates a new {@link CsrfHandler} handler and use the given name to save the token in the
   * {@link Session} and or extract the token from incoming requests.
   *
   * @param name Token's name.
   */
  public CsrfHandler(final String name) {
    this.name = requireNonNull(name, "Name is required.");
    tokenGen(req -> UUID.randomUUID().toString());
    requireTokenOn(req -> REQUIRE_ON.contains(req.method()));
  }

  /**
   * Creates a new {@link CsrfHandler} and use <code>csrf</code> as token name.
   */
  public CsrfHandler() {
    this("csrf");
  }

  /**
   * Set a custom token generator. Default generator use: {@link UUID#randomUUID()}.
   *
   * @param generator A custom token generator.
   * @return This filter.
   */
  public CsrfHandler tokenGen(final Function<Request, String> generator) {
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
  public CsrfHandler requireTokenOn(final Predicate<Request> requireToken) {
    this.requireToken = requireNonNull(requireToken, "RequireToken predicate is required.");
    return this;
  }

  @Override
  public void handle(final Request req, final Response rsp, final Route.Chain chain)
      throws Throwable {

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
