package jooby;

import static java.util.Objects.requireNonNull;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

/**
 * Give you access at the current HTTP request in order to read parameters, headers and body.
 *
 * @author edgar
 * @since 0.1.0
 * @see RequestModule
 * @see Variant
 * @see RouteInterceptor
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
  MediaType type();

  /**
   * @return The value of the <code>Accept header</code>. Default is: {@literal*}/{@literal*}.
   */
  @Nonnull
  List<MediaType> accept();

  default Optional<MediaType> accepts(@Nonnull final String... types) {
    return accepts(MediaType.valueOf(types));
  }

  default Optional<MediaType> accepts(@Nonnull final MediaType... types) {
    return accepts(ImmutableList.copyOf(types));
  }

  Optional<MediaType> accepts(@Nonnull List<MediaType> types);

  /**
   * Get all the available parameter. A HTTP parameter can be provided in any of
   * these forms:
   *
   * <ul>
   * <li>Path parameter, like: <code>/path/:name</code> or <code>/path/{name}</code></li>
   * <li>Query parameter, like: <code>?name=jooby</code></li>
   * <li>Body parameter when <code>Content-Type</code> is
   * <code>application/x-www-form-urlencoded</code> or <code>multipart/form-data</code></li>
   * </ul>
   *
   * @return All the parameters.
   */
  @Nonnull
  Map<String, Variant> params() throws Exception;

  /**
   * Get a HTTP request parameter under the given name. A HTTP parameter can be provided in any of
   * these forms:
   * <ul>
   * <li>Path parameter, like: <code>/path/:name</code> or <code>/path/{name}</code></li>
   * <li>Query parameter, like: <code>?name=jooby</code></li>
   * <li>Body parameter when <code>Content-Type</code> is
   * <code>application/x-www-form-urlencoded</code> or <code>multipart/form-data</code></li>
   * </ul>
   *
   * The order of precedence is: <code>path</code>, <code>query</code> and <code>body</code>. For
   * example a pattern like: <code>GET /path/:name</code> for <code>/path/jooby?name=rocks</code>
   * produces:
   *
   * <pre>
   *  assertEquals("jooby", req.param(name).stringValue());
   *
   *  assertEquals("jooby", req.param(name).toList(String.class).get(0));
   *  assertEquals("rocks", req.param(name).toList(String.class).get(1));
   * </pre>
   *
   * Uploads can be retrieved too when <code>Content-Type</code> is <code>multipart/form-data</code>
   * see {@link Upload} for more information.
   *
   * @param name A parameter's name.
   * @return A HTTP request parameter.
   * @throws Exception If retrieval fails.
   * @see {@link Variant}
   */
  @Nonnull
  Variant param(@Nonnull String name) throws Exception;

  /**
   * Get a HTTP header.
   *
   * @param name A header's name.
   * @return A HTTP request header.
   * @see {@link Variant}
   */
  @Nonnull
  Variant header(@Nonnull String name);

  /**
   * @return All the headers.
   */
  @Nonnull
  Map<String, Variant> headers();

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

  String ip();

  Route route();

  String hostname();

  default boolean xhr() {
    return header("X-Requested-With")
        .toOptional(String.class)
        .map("XMLHttpRequest"::equalsIgnoreCase)
        .orElse(Boolean.FALSE);
  }

  String protocol();

  default boolean secure() {
    return "https".equalsIgnoreCase(protocol());
  }

}
