package jooby;

import java.nio.charset.Charset;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import com.google.common.annotations.Beta;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

/**
 * HTTP Request.
 *
 * @author edgar
 * @since 0.1.0
 */
@Beta
@ThreadSafe
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
  String path();

  /**
   * @return The value of the <code>Content-Type</code> header. Default is: {@literal*}/{@literal*}.
   */
  MediaType contentType();

  /**
   * @return The value of the <code>Accept header</code>. Default is: {@literal*}/{@literal*}.
   */
  List<MediaType> accept();

  HttpField param(@Nonnull String name) throws Exception;

  HttpField header(@Nonnull String name);

  Cookie cookie(String name);

  default <T> T body(final Class<T> type) throws Exception {
    return body(TypeLiteral.get(type));
  }

  <T> T body(TypeLiteral<T> type) throws Exception;

  <T> T get(Class<T> type);

  <T> T get(Key<T> key);

  void destroy();

  Charset charset();

}
