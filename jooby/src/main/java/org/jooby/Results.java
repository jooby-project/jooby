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
package org.jooby;

import static java.util.Objects.requireNonNull;

import java.util.function.Supplier;

/**
 * A {@link Result} builder with some utility static methods (nothing more).
 *
 * @author edgar
 * @since 0.5.0
 */
public class Results {

  /**
   * Set the result
   *
   * @param entity A result value.
   * @return A new result.
   */
  public static Result with(final Object entity) {
    return new Result().set(entity);
  }

  /**
   * Set the result
   *
   * @param entity A result value.
   * @param status A HTTP status.
   * @return A new result.
   */
  public static Result with(final Object entity, final Status status) {
    return new Result().status(status).set(entity);
  }

  /**
   * Set the result
   *
   * @param entity A result value.
   * @param status A HTTP status.
   * @return A new result.
   */
  public static Result with(final Object entity, final int status) {
    return with(entity, Status.valueOf(status));
  }

  /**
   * Set the response status.
   *
   * @param status A status!
   * @return A new result.
   */
  public static Result with(final Status status) {
    requireNonNull(status, "A HTTP status is required.");
    return new Result().status(status);
  }

  /**
   * Set the response status.
   *
   * @param status A status!
   * @return A new result.
   */
  public static Result with(final int status) {
    requireNonNull(status, "A HTTP status is required.");
    return new Result().status(status);
  }

  /**
   * @return A new result with {@link Status#OK}.
   */
  public static Result ok() {
    return with(Status.OK);
  }

  /**
   * @param view View to render.
   * @return A new view.
   */
  public static View html(final String view) {
    return new View(view);
  }

  /**
   * @param entity A result content!
   * @return A new json result.
   */
  public static Result json(final Object entity) {
    return with(entity, 200).type(MediaType.json);
  }

  /**
   * @param entity A result content!
   * @return A new json result.
   */
  public static Result xml(final Object entity) {
    return with(entity, 200).type(MediaType.xml);
  }

  /**
   * @param entity A result content!
   * @return A new result with {@link Status#OK} and given content.
   */
  public static Result ok(final Object entity) {
    return ok().set(entity);
  }

  /**
   * @return A new result with {@link Status#ACCEPTED}.
   */
  public static Result accepted() {
    return with(Status.ACCEPTED);
  }

  /**
   * @param content A result content!
   * @return A new result with {@link Status#ACCEPTED}.
   */
  public static Result accepted(final Object content) {
    return accepted().set(content);
  }

  /**
   * @return A new result with {@link Status#NO_CONTENT}.
   */
  public static Result noContent() {
    return with(Status.NO_CONTENT);
  }

  /**
   * Redirect to the given url with status code defaulting to {@link Status#FOUND}.
   *
   * <pre>
   *  rsp.redirect("/foo/bar");
   *  rsp.redirect("http://example.com");
   *  rsp.redirect("http://example.com");
   *  rsp.redirect("../login");
   * </pre>
   *
   * Redirects can be a fully qualified URI for redirecting to a different site:
   *
   * <pre>
   *   rsp.redirect("http://google.com");
   * </pre>
   *
   * Redirects can be relative to the root of the host name. For example, if you were
   * on <code>http://example.com/admin/post/new</code>, the following redirect to /admin would
   * land you at <code>http://example.com/admin</code>:
   *
   * <pre>
   *   rsp.redirect("/admin");
   * </pre>
   *
   * Redirects can be relative to the current URL. A redirection of post/new, from
   * <code>http://example.com/blog/admin/</code> (notice the trailing slash), would give you
   * <code>http://example.com/blog/admin/post/new.</code>
   *
   * <pre>
   *   rsp.redirect("post/new");
   * </pre>
   *
   * Redirecting to post/new from <code>http://example.com/blog/admin</code> (no trailing slash),
   * will take you to <code>http://example.com/blog/post/new</code>.
   *
   * <p>
   * If you found the above behavior confusing, think of path segments as directories (have trailing
   * slashes) and files, it will start to make sense.
   * </p>
   *
   * Pathname relative redirects are also possible. If you were on
   * <code>http://example.com/admin/post/new</code>, the following redirect would land you at
   * <code>http//example.com/admin</code>:
   *
   * <pre>
   *   rsp.redirect("..");
   * </pre>
   *
   * A back redirection will redirect the request back to the <code>Referer</code>, defaulting to
   * <code>/</code> when missing.
   *
   * <pre>
   *   rsp.redirect("back");
   * </pre>
   *
   * @param location A location.
   * @return A new result.
   */
  public static Result redirect(final String location) {
    return redirect(Status.FOUND, location);
  }

  /**
   * Redirect to the given url with status code defaulting to {@link Status#FOUND}.
   *
   * <pre>
   *  rsp.redirect("/foo/bar");
   *  rsp.redirect("http://example.com");
   *  rsp.redirect("http://example.com");
   *  rsp.redirect("../login");
   * </pre>
   *
   * Redirects can be a fully qualified URI for redirecting to a different site:
   *
   * <pre>
   *   rsp.redirect("http://google.com");
   * </pre>
   *
   * Redirects can be relative to the root of the host name. For example, if you were
   * on <code>http://example.com/admin/post/new</code>, the following redirect to /admin would
   * land you at <code>http://example.com/admin</code>:
   *
   * <pre>
   *   rsp.redirect("/admin");
   * </pre>
   *
   * Redirects can be relative to the current URL. A redirection of post/new, from
   * <code>http://example.com/blog/admin/</code> (notice the trailing slash), would give you
   * <code>http://example.com/blog/admin/post/new.</code>
   *
   * <pre>
   *   rsp.redirect("post/new");
   * </pre>
   *
   * Redirecting to post/new from <code>http://example.com/blog/admin</code> (no trailing slash),
   * will take you to <code>http://example.com/blog/post/new</code>.
   *
   * <p>
   * If you found the above behavior confusing, think of path segments as directories (have trailing
   * slashes) and files, it will start to make sense.
   * </p>
   *
   * Pathname relative redirects are also possible. If you were on
   * <code>http://example.com/admin/post/new</code>, the following redirect would land you at
   * <code>http//example.com/admin</code>:
   *
   * <pre>
   *   rsp.redirect("..");
   * </pre>
   *
   * A back redirection will redirect the request back to the <code>Referer</code>, defaulting to
   * <code>/</code> when missing.
   *
   * <pre>
   *   rsp.redirect("back");
   * </pre>
   *
   * @param location A location.
   * @return A new result.
   */
  public static Result tempRedirect(final String location) {
    return redirect(Status.TEMPORARY_REDIRECT, location);
  }

  /**
   * Redirect to the given url with status code defaulting to {@link Status#FOUND}.
   *
   * <pre>
   *  rsp.redirect("/foo/bar");
   *  rsp.redirect("http://example.com");
   *  rsp.redirect("http://example.com");
   *  rsp.redirect("../login");
   * </pre>
   *
   * Redirects can be a fully qualified URI for redirecting to a different site:
   *
   * <pre>
   *   rsp.redirect("http://google.com");
   * </pre>
   *
   * Redirects can be relative to the root of the host name. For example, if you were
   * on <code>http://example.com/admin/post/new</code>, the following redirect to /admin would
   * land you at <code>http://example.com/admin</code>:
   *
   * <pre>
   *   rsp.redirect("/admin");
   * </pre>
   *
   * Redirects can be relative to the current URL. A redirection of post/new, from
   * <code>http://example.com/blog/admin/</code> (notice the trailing slash), would give you
   * <code>http://example.com/blog/admin/post/new.</code>
   *
   * <pre>
   *   rsp.redirect("post/new");
   * </pre>
   *
   * Redirecting to post/new from <code>http://example.com/blog/admin</code> (no trailing slash),
   * will take you to <code>http://example.com/blog/post/new</code>.
   *
   * <p>
   * If you found the above behavior confusing, think of path segments as directories (have trailing
   * slashes) and files, it will start to make sense.
   * </p>
   *
   * Pathname relative redirects are also possible. If you were on
   * <code>http://example.com/admin/post/new</code>, the following redirect would land you at
   * <code>http//example.com/admin</code>:
   *
   * <pre>
   *   rsp.redirect("..");
   * </pre>
   *
   * A back redirection will redirect the request back to the <code>Referer</code>, defaulting to
   * <code>/</code> when missing.
   *
   * <pre>
   *   rsp.redirect("back");
   * </pre>
   *
   * @param location A location.
   * @return A new result.
   */
  public static Result moved(final String location) {
    return redirect(Status.MOVED_PERMANENTLY, location);
  }

  /**
   * Redirect to the given url with status code defaulting to {@link Status#FOUND}.
   *
   * <pre>
   *  rsp.redirect("/foo/bar");
   *  rsp.redirect("http://example.com");
   *  rsp.redirect("http://example.com");
   *  rsp.redirect("../login");
   * </pre>
   *
   * Redirects can be a fully qualified URI for redirecting to a different site:
   *
   * <pre>
   *   rsp.redirect("http://google.com");
   * </pre>
   *
   * Redirects can be relative to the root of the host name. For example, if you were
   * on <code>http://example.com/admin/post/new</code>, the following redirect to /admin would
   * land you at <code>http://example.com/admin</code>:
   *
   * <pre>
   *   rsp.redirect("/admin");
   * </pre>
   *
   * Redirects can be relative to the current URL. A redirection of post/new, from
   * <code>http://example.com/blog/admin/</code> (notice the trailing slash), would give you
   * <code>http://example.com/blog/admin/post/new.</code>
   *
   * <pre>
   *   rsp.redirect("post/new");
   * </pre>
   *
   * Redirecting to post/new from <code>http://example.com/blog/admin</code> (no trailing slash),
   * will take you to <code>http://example.com/blog/post/new</code>.
   *
   * <p>
   * If you found the above behavior confusing, think of path segments as directories (have trailing
   * slashes) and files, it will start to make sense.
   * </p>
   *
   * Pathname relative redirects are also possible. If you were on
   * <code>http://example.com/admin/post/new</code>, the following redirect would land you at
   * <code>http//example.com/admin</code>:
   *
   * <pre>
   *   rsp.redirect("..");
   * </pre>
   *
   * A back redirection will redirect the request back to the <code>Referer</code>, defaulting to
   * <code>/</code> when missing.
   *
   * <pre>
   *   rsp.redirect("back");
   * </pre>
   *
   * @param location A location.
   * @return A new result.
   */
  public static Result seeOther(final String location) {
    return redirect(Status.SEE_OTHER, location);
  }

  /**
   * Performs content-negotiation on the Accept HTTP header on the request object. It select a
   * handler for the request, based on the acceptable types ordered by their quality values.
   * If the header is not specified, the first callback is invoked. When no match is found,
   * the server responds with 406 "Not Acceptable", or invokes the default callback: {@code ** / *}.
   *
   * <pre>
   *   get("/jsonOrHtml", () {@literal ->}
   *     Results
   *         .when("text/html", () {@literal ->} View.of("view", "model", model))
   *         .when("application/json", () {@literal ->} model)
   *         .when("*", () {@literal ->} Status.NOT_ACCEPTABLE)
   *   );
   * </pre>
   *
   * @param type A media type.
   * @param supplier A result supplier.
   * @return A new result.
   */
  public static Result when(final String type, final Supplier<Object> supplier) {
    return new Result().when(type, supplier);
  }

  /**
   * Performs content-negotiation on the Accept HTTP header on the request object. It select a
   * handler for the request, based on the acceptable types ordered by their quality values.
   * If the header is not specified, the first callback is invoked. When no match is found,
   * the server responds with 406 "Not Acceptable", or invokes the default callback: {@code ** / *}.
   *
   * <pre>
   *   get("/jsonOrHtml", () {@literal ->}
   *     Results
   *         .when("text/html", () {@literal ->} View.of("view", "model", model))
   *         .when("application/json", () {@literal ->} model)
   *         .when("*", () {@literal ->} Status.NOT_ACCEPTABLE)
   *   );
   * </pre>
   *
   * @param type A media type.
   * @param supplier A result supplier.
   * @return A new result.
   */
  public static Result when(final MediaType type, final Supplier<Object> supplier) {
    return new Result().when(type, supplier);
  }

  /**
   * Produces a redirect (302) status code and set the <code>Location</code> header too.
   *
   * @param status A HTTP redirect status.
   * @param location A location.
   * @return A new result.
   */
  private static Result redirect(final Status status, final String location) {
    requireNonNull(location, "A location is required.");
    return with(status).header("location", location);
  }

}
