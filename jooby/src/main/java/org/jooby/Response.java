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
import java.nio.charset.Charset;
import java.util.Optional;

import org.jooby.Cookie.Definition;

import com.google.common.collect.ImmutableList;

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
    protected final Response rsp;

    /**
     * Creates a new {@link Forwarding} response.
     *
     * @param response A response object.
     */
    public Forwarding(final Response response) {
      this.rsp = requireNonNull(response, "A response is required.");
    }

    @Override
    public void download(final String filename, final InputStream stream) throws Exception {
      rsp.download(filename, stream);
    }

    @Override
    public void download(final File file) throws Exception {
      rsp.download(file);
    }

    @Override
    public void download(final String filename, final File file) throws Exception {
      rsp.download(filename, file);
    }

    @Override
    public void download(final String filename) throws Exception {
      rsp.download(filename);
    }

    @Override
    public void download(final String filename, final String location) throws Exception {
      rsp.download(filename, location);
    }

    @Override
    public Response cookie(final String name, final String value) {
      rsp.cookie(name, value);
      return this;
    }

    @Override
    public Response cookie(final Cookie cookie) {
      rsp.cookie(cookie);
      return this;
    }

    @Override
    public Response cookie(final Definition cookie) {
      rsp.cookie(cookie);
      return this;
    }

    @Override
    public Response clearCookie(final String name) {
      rsp.clearCookie(name);
      return this;
    }

    @Override
    public Mutant header(final String name) {
      return rsp.header(name);
    }

    @Override
    public Response header(final String name, final Object value) {
      rsp.header(name, value);
      return this;
    }

    @Override
    public Response header(final String name, final Object... values) {
      rsp.header(name, values);
      return this;
    }

    @Override
    public Response header(final String name, final Iterable<Object> values) {
      rsp.header(name, values);
      return this;
    }

    @Override
    public Charset charset() {
      return rsp.charset();
    }

    @Override
    public Response charset(final Charset charset) {
      rsp.charset(charset);
      return this;
    }

    @Override
    public Response length(final long length) {
      rsp.length(length);
      return this;
    }

    @Override
    public Optional<MediaType> type() {
      return rsp.type();
    }

    @Override
    public Response type(final MediaType type) {
      rsp.type(type);
      return this;
    }

    @Override
    public Response type(final String type) {
      rsp.type(type);
      return this;
    }

    @Override
    public void send(final Object result) throws Exception {
      // Special case: let the default response to deal with Object refs.
      // once resolved it will call the Result version.
      Response.super.send(result);
    }

    @Override
    public void send(final Result result) throws Exception {
      rsp.send(result);
    }

    @Override
    public void end() {
      rsp.end();
    }

    @Override
    public void redirect(final String location) throws Exception {
      rsp.redirect(location);
    }

    @Override
    public void redirect(final Status status, final String location) throws Exception {
      rsp.redirect(status, location);
    }

    @Override
    public Optional<Status> status() {
      return rsp.status();
    }

    @Override
    public Response status(final Status status) {
      rsp.status(status);
      return this;
    }

    @Override
    public Response status(final int status) {
      rsp.status(status);
      return this;
    }

    @Override
    public boolean committed() {
      return rsp.committed();
    }

    @Override
    public String toString() {
      return rsp.toString();
    }

    /**
     * Unwrap a response in order to find out the target instance.
     *
     * @param rsp A response.
     * @return A target instance (not a {@link Response.Forwarding}).
     */
    public static Response unwrap(final Response rsp) {
      requireNonNull(rsp, "A response is required.");
      Response root = rsp;
      while (root instanceof Forwarding) {
        root = ((Forwarding) root).rsp;
      }
      return root;
    }
  }

  /**
   * Transfer the file at path as an "attachment". Typically, browsers will prompt the user for
   * download. The <code>Content-Disposition</code> "filename=" parameter (i.e. the one that will
   * appear in the browser dialog) is set to filename.
   *
   * @param filename A file name to use.
   * @param stream A stream to attach.
   * @throws Exception If something goes wrong.
   */
  void download(String filename, InputStream stream) throws Exception;

  /**
   * Transfer the file at path as an "attachment". Typically, browsers will prompt the user for
   * download. The <code>Content-Disposition</code> "filename=" parameter (i.e. the one that will
   * appear in the browser dialog) is set to filename by default.
   *
   * @param location Classpath location of the file.
   * @throws Exception If something goes wrong.
   */
  default void download(final String location) throws Exception {
    download(location, location);
  }

  /**
   * Transfer the file at path as an "attachment". Typically, browsers will prompt the user for
   * download. The <code>Content-Disposition</code> "filename=" parameter (i.e. the one that will
   * appear in the browser dialog) is set to filename by default.
   *
   * @param filename A file name to use.
   * @param location classpath location of the file.
   * @throws Exception If something goes wrong.
   */
  void download(final String filename, final String location) throws Exception;

  /**
   * Transfer the file at path as an "attachment". Typically, browsers will prompt the user for
   * download. The <code>Content-Disposition</code> "filename=" parameter (i.e. the one that will
   * appear in the browser dialog) is set to filename by default.
   *
   * @param file A file to use.
   * @throws Exception If something goes wrong.
   */
  default void download(final File file) throws Exception {
    download(file.getName(), file);
  }

  /**
   * Transfer the file at path as an "attachment". Typically, browsers will prompt the user for
   * download. The <code>Content-Disposition</code> "filename=" parameter (i.e. the one that will
   * appear in the browser dialog) is set to filename.
   *
   * @param filename A file name to use.
   * @param file A file to use.
   * @throws Exception If something goes wrong.
   */
  default void download(final String filename, final File file) throws Exception {
    length(file.length());
    download(filename, new FileInputStream(file));
  }

  /**
   * Adds the specified cookie to the response.
   *
   * @param name A cookie's name.
   * @param value A cookie's value.
   * @return This response.
   */
  default Response cookie(final String name, final String value) {
    return cookie(new Cookie.Definition(name, value).toCookie());
  }

  /**
   * Adds the specified cookie to the response.
   *
   * @param cookie A cookie definition.
   * @return This response.
   */
  default Response cookie(final Cookie.Definition cookie) {
    requireNonNull(cookie, "A cookie is required.");
    return cookie(cookie.toCookie());
  }

  /**
   * Adds the specified cookie to the response.
   *
   * @param cookie A cookie.
   * @return This response.
   */
  Response cookie(Cookie cookie);

  /**
   * Discard a cookie from response. Discard is done by setting maxAge=0.
   *
   * @param name Cookie's name.
   * @return This response.
   */
  Response clearCookie(String name);

  /**
   * Get a header with the given name.
   *
   * @param name A name.
   * @return A HTTP header.
   */
  Mutant header(String name);

  /**
   * Sets a response header with the given name and value. If the header had already been set,
   * the new value overwrites the previous one.
   *
   * @param name Header's name.
   * @param value Header's value.
   * @return This response.
   */
  Response header(String name, Object value);

  /**
   * Sets a response header with the given name and value. If the header had already been set,
   * the new value overwrites the previous one.
   *
   * @param name Header's name.
   * @param values Header's value.
   * @return This response.
   */
  default Response header(final String name, final Object... values) {
    return header(name, ImmutableList.builder().add(values).build());
  }

  /**
   * Sets a response header with the given name and value. If the header had already been set,
   * the new value overwrites the previous one.
   *
   * @param name Header's name.
   * @param values Header's value.
   * @return This response.
   */
  Response header(String name, Iterable<Object> values);

  /**
   * If charset is not set this method returns charset defined in the request body. If the request
   * doesn't specify a character encoding, this method return the global charset:
   * <code>application.charset</code>.
   *
   * @return A current charset.
   */
  Charset charset();

  /**
   * Set the {@link Charset} to use and set the <code>Content-Type</code> header with the current
   * charset.
   *
   * @param charset A charset.
   * @return This response.
   */
  Response charset(Charset charset);

  /**
   * Set the length of the response and set the <code>Content-Length</code> header.
   *
   * @param length Length of response.
   * @return This response.
   */
  Response length(long length);

  /**
   * @return Get the response type.
   */
  Optional<MediaType> type();

  /**
   * Set the response media type and set the <code>Content-Type</code> header.
   *
   * @param type A media type.
   * @return This response.
   */
  Response type(MediaType type);

  /**
   * Set the response media type and set the <code>Content-Type</code> header.
   *
   * @param type A media type.
   * @return This response.
   */
  default Response type(final String type) {
    return type(MediaType.valueOf(type));
  }

  /**
   * Responsible of writing the given body into the HTTP response.
   *
   * @param result The HTTP body.
   * @throws Exception If the response write fails.
   */
  default void send(final Object result) throws Exception {
    requireNonNull(result, "A response message is required.");
    if (result instanceof Result) {
      send((Result) result);
    } else {
      // wrap body
      Result b = Results.with(result);
      status().ifPresent(b::status);
      type().ifPresent(b::type);
      send(b);
    }
  }

  /**
   * Responsible of writing the given body into the HTTP response.
   *
   * @param result A HTTP response.
   * @throws Exception If the response write fails.
   */
  void send(Result result) throws Exception;

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
   * @param location Either a relative or absolute location.
   * @throws Exception If redirection fails.
   */
  default void redirect(final String location) throws Exception {
    redirect(Status.FOUND, location);
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
   * @param status A redirect status.
   * @param location Either a relative or absolute location.
   * @throws Exception If redirection fails.
   */
  void redirect(Status status, String location) throws Exception;

  /**
   * @return A HTTP status or empty if status was not set yet.
   */
  Optional<Status> status();

  /**
   * Set the HTTP response status.
   *
   * @param status A HTTP status.
   * @return This response.
   */
  Response status(Status status);

  /**
   * Set the HTTP response status.
   *
   * @param status A HTTP status.
   * @return This response.
   */
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
   * Ends current request/response cycle by releasing any existing resources and committing the
   * response into the channel.
   *
   * This method is automatically call it from a send method, so you are not force to call this
   * method per each request/response cycle.
   *
   * It's recommended for quickly ending the response without any data:
   *
   * <pre>
   *   rsp.status(304).end();
   * </pre>
   *
   * Keep in mind that an explicit call to this method will stop the execution of handlers. So,
   * any handler further in the chain won't be executed once end has been called.
   */
  void end();

}
