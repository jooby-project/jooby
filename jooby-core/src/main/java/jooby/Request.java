package jooby;

import static java.util.Objects.requireNonNull;

import java.nio.charset.Charset;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.annotations.Beta;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

/**
 * Give you access at the current HTTP request in order to read parameters, headers and body.
 *
 * <h1>Accessing to parameters or headers</h1>
 *
 * @author edgar
 * @since 0.1.0
 * @see RequestModule.
 * @see HttpField
 */
@Beta
public interface Request {

  /**
   * Given:
   *
   * <pre>
   *  http://domain.com/some/path.html -> /some/path.html
   *  http://domain.com/a.html         -> /a.html
   * </pre>
   *
   * @return The request URL pathname.
   */
  @Nonnull
  String path();

  /**
   * @return The value of the <code>Content-Type</code> header. Default is: {@literal*}/{@literal*}.
   */
  @Nonnull
  MediaType contentType();

  /**
   * @return The value of the <code>Accept header</code>. Default is: {@literal*}/{@literal*}.
   */
  @Nonnull
  List<MediaType> accept();

  /**
   * @return All the parameter names.
   */
  @Nonnull
  List<String> parameterNames();

  /**
   * Get a HTTP request parameter under the given name. A HTTP parameter can be provided in any of
   * these forms:
   * <ul>
   * <li>Part of the HTTP query, like: <code>?name=jooby</code></li>
   * <li>Path variables, like: <code>/path/:name</code> or <code>/path/{name}</code></li>
   * <li>POST/PUT parameter when <code>Content-Type</code> is
   * <code>application/x-www-form-urlencoded</code></li>
   * <li>POST/PUT parameter when <code>Content-Type</code> is <code>multipart/form-data</code></li>
   * </ul>
   *
   * Uploads can be retrieved too if <code>Content-Type</code> is <code>multipart/form-data</code>
   * see {@link Upload} for more information.
   *
   * @param name A parameter's name.
   * @return A HTTP request parameter.
   * @throws Exception If retrieval fails.
   * @see {@link HttpField}
   */
  @Nonnull
  HttpField param(@Nonnull String name) throws Exception;

  /**
   * Get a HTTP request header with the given name.
   *
   * @param name A header's name.
   * @return A HTTP request header.
   * @throws Exception If retrieval fails.
   * @see {@link HttpField}
   */
  @Nonnull
  HttpField header(@Nonnull String name);

  /**
   * @return All the header names.
   */
  @Nonnull
  List<String> headerNames();

  /**
   * Get a cookie with the given name or <code>null</code> if there is no cookie.
   *
   * @param name Cookie's name.
   * @return A cookie with the given name or <code>null</code> if there is no cookie.
   */
  @Nullable
  Cookie cookie(@Nonnull String name);

  /**
   * @return All the cookies.
   */
  @Nonnull
  List<Cookie> cookies();

  /**
   * Convert the HTTP request body into the given type.
   *
   * @param type The body type.
   * @return The HTTP body as an object.
   * @throws Exception If body can't be converted or there is no HTTP body.
   * @see {@link BodyConverter#read(TypeLiteral, BodyReader)}
   */
  @Nonnull
  default <T> T body(@Nonnull final Class<T> type) throws Exception {
    requireNonNull(type, "A body type is required.");
    return body(TypeLiteral.get(type));
  }

  /**
   * Convert the HTTP request body into the given type.
   *
   * @param type The body type.
   * @return The HTTP body as an object.
   * @throws Exception If body can't be converted or there is no HTTP body.
   * @see {@link BodyConverter#read(TypeLiteral, BodyReader)}
   */
  @Nonnull
  <T> T body(@Nonnull TypeLiteral<T> type) throws Exception;

  /**
   * Creates a new instance (if need it) and inject required dependencies. Request scoped object
   * can registered using a {@link RequestModule}.
   *
   * @param type A body type.
   * @return A ready to use object.
   * @see RequestModule
   */
  @Nonnull
  default <T> T getInstance(@Nonnull final Class<T> type) {
    return getInstance(Key.get(type));
  }

  /**
   * Creates a new instance (if need it) and inject required dependencies. Request scoped object
   * can registered using a {@link RequestModule}.
   *
   * @param type A body type.
   * @return A ready to use object.
   * @see RequestModule
   */
  @Nonnull
  default <T> T getInstance(@Nonnull final TypeLiteral<T> type) {
    return getInstance(Key.get(type));
  }

  /**
   * Creates a new instance (if need it) and inject required dependencies. Request scoped object
   * can registered using a {@link RequestModule}.
   *
   * @param key A body key.
   * @return A ready to use object.
   * @see RequestModule
   */
  @Nonnull
  <T> T getInstance(@Nonnull Key<T> key);

  /**
   * The charset in used for the current HTTP request. If the request doesn't specify a character
   * encoding, this method return the global charset defined by: <code>application.charset</code>.
   *
   * @return A current charset.
   */
  @Nonnull
  Charset charset();

}
