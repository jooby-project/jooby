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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.jooby.Cookie.Definition;
import org.jooby.fn.ExSupplier;

/**
 * Give you access to the actual HTTP response. You can read/write headers and write HTTP body.
 *
 * @author edgar
 * @since 0.1.0
 */
public interface Response {

  /**
   * A forwarding response.
   *
   * @author edgar
   * @since 0.1.0
   */
  class Forwarding implements Response {

    /** The target response. */
    private Response response;

    /**
     * Creates a new {@link Forwarding} response.
     *
     * @param response A response object.
     */
    public Forwarding(final Response response) {
      this.response = requireNonNull(response, "A response is required.");
    }

    @Override
    public void download(final String filename, final InputStream stream) throws Exception {
      response.download(filename, stream);
    }

    @Override
    public void download(final String filename, final Reader reader) throws Exception {
      response.download(filename, reader);
    }

    @Override
    public void download(final File file) throws Exception {
      response.download(file);
    }

    @Override
    public void download(final String filename) throws Exception {
      response.download(filename);
    }

    @Override
    public Response cookie(final String name, final String value) {
      return response.cookie(name, value);
    }

    @Override
    public Response cookie(final Cookie cookie) {
      return response.cookie(cookie);
    }

    @Override
    public Response cookie(final Definition cookie) {
      return response.cookie(cookie);
    }

    @Override
    public Response clearCookie(final String name) {
      return response.clearCookie(name);
    }

    @Override
    public Mutant header(final String name) {
      return response.header(name);
    }

    @Override
    public Response header(final String name, final byte value) {
      return response.header(name, value);
    }

    @Override
    public Response header(final String name, final char value) {
      return response.header(name, value);
    }

    @Override
    public Response header(final String name, final Date value) {
      return response.header(name, value);
    }

    @Override
    public Response header(final String name, final double value) {
      return response.header(name, value);
    }

    @Override
    public Response header(final String name, final float value) {
      return response.header(name, value);
    }

    @Override
    public Response header(final String name, final int value) {
      return response.header(name, value);
    }

    @Override
    public Response header(final String name, final long value) {
      return response.header(name, value);
    }

    @Override
    public Response header(final String name, final short value) {
      return response.header(name, value);
    }

    @Override
    public Response header(final String name, final CharSequence value) {
      return response.header(name, value);
    }

    @Override
    public Charset charset() {
      return response.charset();
    }

    @Override
    public Response charset(final Charset charset) {
      return response.charset(charset);
    }

    @Override
    public Response length(final int length) {
      return response.length(length);
    }

    @Override
    public <T> T local(final String name) {
      return response.local(name);
    }

    @Override
    public Response local(final String name, final Object value) {
      return response.local(name, value);
    }

    @Override
    public Map<String, Object> locals() {
      return response.locals();
    }

    @Override
    public Optional<MediaType> type() {
      return response.type();
    }

    @Override
    public Response type(final MediaType type) {
      return response.type(type);
    }

    @Override
    public Response type(final String type) {
      return response.type(type);
    }

    @Override
    public void send(final Object body) throws Exception {
      response.send(body);
    }

    @Override
    public void send(final Body body) throws Exception {
      response.send(body);
    }

    @Override
    public Formatter format() {
      return response.format();
    }

    @Override
    public void redirect(final String location) throws Exception {
      response.redirect(location);
    }

    @Override
    public void redirect(final Status status, final String location) throws Exception {
      response.redirect(status, location);
    }

    @Override
    public Optional<Status> status() {
      return response.status();
    }

    @Override
    public Response status(final Status status) {
      return response.status(status);
    }

    @Override
    public Response status(final int status) {
      return response.status(status);
    }

    @Override
    public boolean committed() {
      return response.committed();
    }

    @Override
    public List<MediaType> viewableTypes() {
      return response.viewableTypes();
    }

    @Override
    public String toString() {
      return response.toString();
    }

    /**
     * Unwrap a response in order to find out the target instance.
     *
     * @param req A response.
     * @return A target instance (not a {@link Forwarding}).
     */
    public static Response unwrap(final @Nonnull Response res) {
      requireNonNull(res, "A response is required.");
      Response root = res;
      while (root instanceof Forwarding) {
        root = ((Forwarding) root).response;
      }
      return root;
    }
  }

  /**
   * Handle content negotiation. For example:
   *
   * <pre>
   *  {{
   *      get("/", (req, resp) -> {
   *        Object model = ...;
   *        resp.when("text/html", () -> Viewable.of("view", model))
   *            .when("application/json", () -> model)
   *            .send();
   *      });
   *  }}
   * </pre>
   *
   * The example above will render a view when accept header is "text/html" or just send a text
   * version of model when the accept header is "application/json".
   *
   * @author edgar
   * @since 0.1.0
   */
  interface Formatter {

    /**
     * Add a new when clause for a custom media-type.
     *
     * @param mediaType A media type to test for.
     * @param supplier An object provider.
     * @return The current {@link Formatter}.
     */
    default @Nonnull Formatter when(final String mediaType,
        final @Nonnull ExSupplier<Object> supplier) {
      return when(MediaType.valueOf(mediaType), supplier);
    }

    /**
     * Add a new when clause for a custom media-type.
     *
     * @param mediaType A media type to test for.
     * @param provider An object provider.
     * @return A {@link Formatter}.
     */
    @Nonnull
    Formatter when(MediaType mediaType, @Nonnull ExSupplier<Object> supplier);

    /**
     * Send the response.
     *
     * @throws Exception If something fails.
     */
    void send() throws Exception;
  }

  /**
   * Transfer the file at path as an "attachment". Typically, browsers will prompt the user for
   * download. The <code>Content-Disposition</code> "filename=" parameter (i.e. the one that will
   * appear in the browser dialog) is set to path by default. However, you may provide an override
   * filename.
   *
   * @param filename A file name to use.
   * @param stream A stream to attach.
   * @throws Exception If something goes wrong.
   */
  void download(@Nonnull String filename, @Nonnull InputStream stream) throws Exception;

  /**
   * Transfer the file at path as an "attachment". Typically, browsers will prompt the user for
   * download. The <code>Content-Disposition</code> "filename=" parameter (i.e. the one that will
   * appear in the browser dialog) is set to path by default. However, you may provide an override
   * filename.
   *
   * @param filename A file name to use.
   * @param reader A reader to attach.
   * @throws Exception If something goes wrong.
   */
  void download(@Nonnull String filename, @Nonnull Reader reader) throws Exception;

  /**
   * Transfer the file at path as an "attachment". Typically, browsers will prompt the user for
   * download. The <code>Content-Disposition</code> "filename=" parameter (i.e. the one that will
   * appear in the browser dialog) is set to path by default. However, you may provide an override
   * filename.
   *
   * @param filename A file name to use.
   * @throws Exception If something goes wrong.
   */
  default void download(final @Nonnull String filename) throws Exception {
    download(filename, getClass().getResourceAsStream(filename));
  }

  /**
   * Transfer the file at path as an "attachment". Typically, browsers will prompt the user for
   * download. The <code>Content-Disposition</code> "filename=" parameter (i.e. the one that will
   * appear in the browser dialog) is set to path by default. However, you may provide an override
   * filename.
   *
   * @param file A file to use.
   * @throws Exception If something goes wrong.
   */
  default void download(final @Nonnull File file) throws Exception {
    download(file.getName(), new FileInputStream(file));
  }

  /**
   * Adds the specified cookie to the response.
   *
   * @param name A cookie's name.
   * @param value A cookie's value.
   * @return This response.
   */
  default Response cookie(final @Nonnull String name, final @Nonnull String value) {
    return cookie(new Cookie.Definition(name, value).toCookie());
  }

  /**
   * Adds the specified cookie to the response.
   *
   * @param cookie A cookie definition.
   * @return This response.
   */
  default Response cookie(final @Nonnull Cookie.Definition cookie) {
    requireNonNull(cookie, "A cookie is required.");
    return cookie(cookie.toCookie());
  }

  /**
   * Adds the specified cookie to the response.
   *
   * @param cookie A cookie.
   * @return This response.
   */
  Response cookie(@Nonnull Cookie cookie);

  /**
   * Discard a cookie from response. Discard is done by setting maxAge=0.
   *
   * @param name Cookie's name.
   * @return This response.
   */
  Response clearCookie(@Nonnull String name);

  /**
   * Get a header with the given name.
   *
   * @param name A name.
   * @return A HTTP header.
   */
  @Nonnull
  Mutant header(@Nonnull String name);

  /**
   * Sets a response header with the given name and value. If the header had already been set,
   * the new value overwrites the previous one.
   *
   * @param name Header's name.
   * @param value Header's value.
   * @return This response.
   */
  Response header(@Nonnull String name, char value);

  /**
   * Sets a response header with the given name and value. If the header had already been set,
   * the new value overwrites the previous one.
   *
   * @param name Header's name.
   * @param value Header's value.
   * @return This response.
   */
  Response header(@Nonnull String name, byte value);

  /**
   * Sets a response header with the given name and value. If the header had already been set,
   * the new value overwrites the previous one.
   *
   * @param name Header's name.
   * @param value Header's value.
   * @return This response.
   */
  Response header(@Nonnull String name, short value);

  /**
   * Sets a response header with the given name and value. If the header had already been set,
   * the new value overwrites the previous one.
   *
   * @param name Header's name.
   * @param value Header's value.
   * @return This response.
   */
  Response header(@Nonnull String name, int value);

  /**
   * Sets a response header with the given name and value. If the header had already been set,
   * the new value overwrites the previous one.
   *
   * @param name Header's name.
   * @param value Header's value.
   * @return This response.
   */
  Response header(@Nonnull String name, long value);

  /**
   * Sets a response header with the given name and value. If the header had already been set,
   * the new value overwrites the previous one.
   *
   * @param name Header's name.
   * @param value Header's value.
   * @return This response.
   */
  Response header(@Nonnull String name, float value);

  /**
   * Sets a response header with the given name and value. If the header had already been set,
   * the new value overwrites the previous one.
   *
   * @param name Header's name.
   * @param value Header's value.
   * @return This response.
   */
  Response header(@Nonnull String name, double value);

  /**
   * Sets a response header with the given name and value. If the header had already been set,
   * the new value overwrites the previous one.
   *
   * @param name Header's name.
   * @param value Header's value.
   * @return This response.
   */
  Response header(@Nonnull String name, CharSequence value);

  /**
   * Sets a response header with the given name and value. If the header had already been set,
   * the new value overwrites the previous one.
   *
   * @param name Header's name.
   * @param value Header's value.
   * @return This response.
   */
  Response header(@Nonnull String name, Date value);

  /**
   * If charset is not set this method returns charset defined in the request body. If the request
   * doesn't specify a character encoding, this method return the global charset:
   * <code>application.charset</code>.
   *
   * @return A current charset.
   */
  @Nonnull
  Charset charset();

  /**
   * Set the {@link Charset} to use and set the <code>Content-Type</code> header with the current
   * charset.
   *
   * @param charset A charset.
   * @return This response.
   */
  @Nonnull
  Response charset(@Nonnull Charset charset);

  /**
   * Set the length of the response and set the <code>Content-Length</code> header.
   *
   * @param length Length of response.
   * @return This response.
   */
  @Nonnull
  Response length(int length);

  /**
   * @return Get the response type.
   */
  @Nonnull
  Optional<MediaType> type();

  /**
   * Set the response media type and set the <code>Content-Type</code> header.
   *
   * @param type A media type.
   * @return This response.
   */
  @Nonnull
  Response type(@Nonnull MediaType type);

  /**
   * Set the response media type and set the <code>Content-Type</code> header.
   *
   * @param type A media type.
   * @return This response.
   */
  default @Nonnull Response type(@Nonnull final String type) {
    return type(MediaType.valueOf(type));
  }

  /**
   * Responsible of writing the given body into the HTTP response. The {@link BodyConverter} that
   * best matches the <code>Accept</code> header will be selected for writing the response.
   *
   * @param body The HTTP body.
   * @throws Exception If the response write fails.
   */
  default void send(@Nonnull final Object body) throws Exception {
    requireNonNull(body, "A response message is required.");
    if (body instanceof Body) {
      send((Body) body);
    } else {
      // wrap body
      Body b = Body.body(body);
      status().ifPresent(b::status);
      type().ifPresent(b::type);
      send(b);
    }
  }

  /**
   * Responsible of writing the given body into the HTTP response. The {@link Body.Formatter} that
   * best matches the <code>Accept</code> header will be selected for writing the response.
   *
   * @param body A HTTP body.
   * @param formatter A convert to use.
   * @throws Exception If the response write fails.
   */
  void send(@Nonnull Body body) throws Exception;

  /**
   * Performs content-negotiation on the Accept HTTP header on the request object. It select a
   * handler for the request, based on the acceptable types ordered by their quality values.
   * If the header is not specified, the first callback is invoked. When no match is found,
   * the server responds with 406 "Not Acceptable", or invokes the default callback: {@code ** / *}.
   *
   * <pre>
   *   get("/jsonOrHtml", (req, res) ->
   *     res.format()
   *         .when("text/html", () -> Viewable.of("view", model))
   *         .when("application/json", () -> model)
   *         .when("*", () -> Status.NOT_ACCEPTABLE)
   *         .send()
   *   );
   * </pre>
   *
   * @return A response formatter.
   */
  @Nonnull
  Formatter format();

  /**
   * Redirect to the given url with status code defaulting to {@link Status#FOUND}.
   *
   * <pre>
   *  res.redirect("/foo/bar");
   *  res.redirect("http://example.com");
   *  res.redirect("http://example.com");
   *  res.redirect("../login");
   * </pre>
   *
   * Redirects can be a fully qualified URI for redirecting to a different site:
   *
   * <pre>
   *   res.redirect("http://google.com");
   * </pre>
   *
   * Redirects can be relative to the root of the host name. For example, if you were
   * on <code>http://example.com/admin/post/new</code>, the following redirect to /admin would
   * land you at <code>http://example.com/admin</code>:
   *
   * <pre>
   *   res.redirect("/admin");
   * </pre>
   *
   * Redirects can be relative to the current URL. A redirection of post/new, from
   * <code>http://example.com/blog/admin/</code> (notice the trailing slash), would give you
   * <code>http://example.com/blog/admin/post/new.</code>
   *
   * <pre>
   *   res.redirect("post/new");
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
   *   res.redirect("..");
   * </pre>
   *
   * A back redirection will redirect the request back to the <code>Referer</code>, defaulting to
   * <code>/</code> when missing.
   *
   * <pre>
   *   res.redirect("back");
   * </pre>
   *
   * @param location Either a relative or absolute location.
   * @throws Exception If redirection fails.
   */
  default void redirect(final @Nonnull String location) throws Exception {
    redirect(Status.FOUND, location);
  }

  /**
   * Redirect to the given url with status code defaulting to {@link Status#FOUND}.
   *
   * <pre>
   *  res.redirect("/foo/bar");
   *  res.redirect("http://example.com");
   *  res.redirect("http://example.com");
   *  res.redirect("../login");
   * </pre>
   *
   * Redirects can be a fully qualified URI for redirecting to a different site:
   *
   * <pre>
   *   res.redirect("http://google.com");
   * </pre>
   *
   * Redirects can be relative to the root of the host name. For example, if you were
   * on <code>http://example.com/admin/post/new</code>, the following redirect to /admin would
   * land you at <code>http://example.com/admin</code>:
   *
   * <pre>
   *   res.redirect("/admin");
   * </pre>
   *
   * Redirects can be relative to the current URL. A redirection of post/new, from
   * <code>http://example.com/blog/admin/</code> (notice the trailing slash), would give you
   * <code>http://example.com/blog/admin/post/new.</code>
   *
   * <pre>
   *   res.redirect("post/new");
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
   *   res.redirect("..");
   * </pre>
   *
   * A back redirection will redirect the request back to the <code>Referer</code>, defaulting to
   * <code>/</code> when missing.
   *
   * <pre>
   *   res.redirect("back");
   * </pre>
   *
   * @param status A redirect status.
   * @param location Either a relative or absolute location.
   * @throws Exception If redirection fails.
   */
  void redirect(@Nonnull Status status, @Nonnull String location) throws Exception;

  /**
   * @return A HTTP status or empty if status was not set yet.
   */
  @Nonnull
  Optional<Status> status();

  /**
   * Set the HTTP response status.
   *
   * @param status A HTTP status.
   * @return This response.
   */
  @Nonnull
  Response status(@Nonnull Status status);

  /**
   * Set the HTTP response status.
   *
   * @param status A HTTP status.
   * @return This response.
   */
  @Nonnull
  default Response status(final int status) {
    return status(Status.valueOf(status));
  }

  /**
   * Returns a boolean indicating if the response has been committed. A committed response has
   * already had its status code and headers written.
   *
   * @return a boolean indicating if the response has been committed
   */
  boolean committed();

  /**
   * Response local variables are scoped to the request, and therefore only available to the view(s)
   * rendered during that request / response cycle.
   *
   * @return Immutable version of local variables.
   */
  @Nonnull
  Map<String, Object> locals();

  /**
   * Get a local variable by it's name.
   *
   * @param name A var's name
   * @return A local.
   */
  @Nonnull
  <T> T local(@Nonnull String name);

  /**
   * Put a local using a var's name. It override any existing local.
   *
   * @param name A var's name
   * @param value A var's value.
   * @return This response.
   */
  Response local(@Nonnull String name, @Nonnull Object value);

  /**
   * @return All the media types reported by all the {@link View.Engine}. Useful to identify and
   *         apply content negotiation.
   */
  List<MediaType> viewableTypes();

}
