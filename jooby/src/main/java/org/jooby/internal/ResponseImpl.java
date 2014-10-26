package org.jooby.internal;

import static java.util.Objects.requireNonNull;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.jooby.BodyConverter;
import org.jooby.BodyReader;
import org.jooby.BodyWriter;
import org.jooby.MediaType;
import org.jooby.MediaTypeProvider;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Variant;
import org.jooby.fn.ExSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;

public class ResponseImpl implements Response {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private HttpServletResponse response;

  private Injector injector;

  private BodyConverterSelector selector;

  private Charset charset;

  private Response.Status status;

  private MediaType type;

  private boolean committed;

  private MediaTypeProvider typeProvider;

  private final Map<String, Object> locals;

  private Route route;

  private SetHeaderImpl setHeader;

  private Optional<String> referer;

  public ResponseImpl(final HttpServletResponse response, final Injector injector,
      final Route route, final Map<String, Object> locals, final BodyConverterSelector selector,
      final MediaTypeProvider typeProvider, final Charset charset, final Optional<String> referer) {
    this.response = requireNonNull(response, "A response is required.");
    this.injector = requireNonNull(injector, "An injector is required.");
    this.route = requireNonNull(route, "A route is required.");
    this.locals = requireNonNull(locals, "The locals is required.");
    this.selector = requireNonNull(selector, "A message converter selector is required.");
    this.typeProvider = requireNonNull(typeProvider, "A type's provider is required.");
    this.charset = requireNonNull(charset, "A charset is required.");
    this.referer = requireNonNull(referer, "A referer is required.");

    this.setHeader = new SetHeaderImpl((name, value) -> response.setHeader(name, value));
  }

  @Override
  public void download(final String filename, final Reader reader) throws Exception {
    requireNonNull(filename, "A file's name is required.");
    requireNonNull(reader, "A reader is required.");

    contentDisposition(filename);
    send(reader, FallbackBodyConverter.COPY_TEXT);
  }

  @Override
  public void download(final String filename, final InputStream stream) throws Exception {
    requireNonNull(filename, "A file's name is required.");
    requireNonNull(stream, "A stream is required.");

    contentDisposition(filename);
    send(stream, FallbackBodyConverter.COPY_BYTES);
  }

  private void contentDisposition(final String filename) {
    String basename = filename;
    int last = filename.lastIndexOf('/');
    if (last >= 0) {
      basename = basename.substring(last + 1);
    }
    header("Content-Disposition", "attachment; filename=" + basename);
    header("Transfer-Encoding", "chunked");
    type(typeProvider.forPath(basename));
  }

  @Override
  public Response cookie(final org.jooby.Cookie cookie) {
    requireNonNull(cookie, "A cookie is required.");
    Cookie c = new Cookie(cookie.name(), cookie.value());
    String comment = cookie.comment();
    if (comment != null) {
      c.setComment(comment);
    }
    String domain = cookie.domain();
    if (domain != null) {
      c.setDomain(domain);
    }
    c.setHttpOnly(cookie.httpOnly());
    c.setMaxAge(cookie.maxAge());
    c.setPath(cookie.path());
    c.setSecure(cookie.secure());
    c.setVersion(cookie.version());
    response.addCookie(c);

    return this;
  }

  @Override
  public Response clearCookie(final String name) {
    requireNonNull(name, "A cookie's name is required.");
    Cookie cookie = new Cookie(name, null);
    cookie.setMaxAge(0);
    response.addCookie(cookie);

    return this;
  }

  @Override
  public Variant header(final String name) {
    requireNonNull(name, "A header's name is required.");

    return new VariantImpl(injector, name, ImmutableList.copyOf(response.getHeaders(name)),
        MediaType.all, charset);
  }

  @Override
  public Response header(final String name, final byte value) {
    setHeader.header(name, value);
    return this;
  }

  @Override
  public Response header(final String name, final char value) {
    setHeader.header(name, value);
    return this;
  }

  @Override
  public Response header(final String name, final double value) {
    setHeader.header(name, value);
    return this;
  }

  @Override
  public Response header(final String name, final float value) {
    setHeader.header(name, value);
    return this;
  }

  @Override
  public Response header(final String name, final int value) {
    setHeader.header(name, value);
    return this;
  }

  @Override
  public Response header(final String name, final long value) {
    setHeader.header(name, value);
    return this;
  }

  @Override
  public Response header(final String name, final Date value) {
    setHeader.header(name, value);
    return this;
  }

  @Override
  public Response header(final String name, final short value) {
    setHeader.header(name, value);
    return this;
  }

  @Override
  public Response header(final String name, final CharSequence value) {
    setHeader.header(name, value);
    return this;
  }

  @Override
  public boolean committed() {
    return committed || response.isCommitted();
  }

  @Override
  public Charset charset() {
    return charset;
  }

  @Override
  public Response charset(final Charset charset) {
    this.charset = requireNonNull(charset, "A charset is required.");
    response.setCharacterEncoding(charset.name());
    return this;
  }

  @Override
  public Response length(final int length) {
    response.setContentLength(length);
    return this;
  }

  @Override
  public Map<String, Object> locals() {
    return ImmutableMap.copyOf(locals);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T local(final String name) {
    return (T) locals.get(name);
  }

  @Override
  public Response local(final String name, final Object value) {
    requireNonNull(name, "Name is required.");
    locals.put(name, value);
    return this;
  }

  @Override
  public void send(final Body body) throws Exception {
    requireNonNull(body, "A body is required.");
    Optional<Object> content = body.content();
    BodyConverter converter = content.isPresent()
        ? selector.forWrite(content.get(), route.produces())
            .orElseThrow(() -> new Route.Err(Response.Status.NOT_ACCEPTABLE))
        : noop(route.produces());
    send(body, converter);
  }

  private static BodyConverter noop(final List<MediaType> types) {
    return new BodyConverter() {

      @Override
      public void write(final Object body, final BodyWriter writer) throws Exception {
        writer.bytes(out -> out.close());
      }

      @Override
      public List<MediaType> types() {
        return types;
      }

      @Override
      public <T> T read(final TypeLiteral<T> type, final BodyReader reader) throws Exception {
        throw new UnsupportedOperationException();
      }

      @Override
      public boolean canWrite(final Class<?> type) {
        return true;
      }

      @Override
      public boolean canRead(final TypeLiteral<?> type) {
        return false;
      }
    };
  }

  @Override
  public void send(final Body body, final BodyConverter converter) throws Exception {
    requireNonNull(body, "A response message is required.");
    requireNonNull(converter, "A converter is required.");

    if (response.isCommitted()) {
      log.warn("  message ignored, response was committed already");
      return;
    }

    type(body.type().orElse(converter.types().get(0)));

    status(body.status().orElse(Response.Status.OK));

    Runnable setHeaders = () -> body.headers().forEach((name, value) -> header(name, value));

    // byte version of http body
    ExSupplier<OutputStream> stream = () -> {
      setHeaders.run();
      return response.getOutputStream();
    };

    // text version of http body
    ExSupplier<Writer> writer = () -> {
      charset(charset);
      setHeaders.run();
      return new OutputStreamWriter(response.getOutputStream(), charset);
    };

    Optional<Object> content = body.content();
    if (content.isPresent()) {
      Object message = content.get();
      if (message instanceof Status) {
        // override status when message is a status
        status((Status) message);
      }
      converter.write(message, new BodyWriterImpl(charset, stream, writer));
    } else {
      // dump any pending header
      setHeaders.run();
    }
  }

  @Override
  public Formatter format() {
    final Map<MediaType, ExSupplier<Object>> strategies = new LinkedHashMap<>();
    List<MediaType> types = new LinkedList<>();

    return new Formatter() {

      @Override
      public Formatter when(final MediaType type, final ExSupplier<Object> supplier) {
        requireNonNull(type, "A media type is required.");
        requireNonNull(supplier, "A supplier fn is required.");
        strategies.put(type, supplier);
        types.add(type);
        return this;
      }

      @Override
      public void send() throws Exception {
        List<MediaType> produces = route.produces();

        ExSupplier<Object> provider = MediaType
            .matcher(produces)
            .first(types)
            .map(it -> strategies.get(it))
            .orElseThrow(
                () -> new Route.Err(Response.Status.NOT_ACCEPTABLE, Joiner.on(", ").join(produces))
            );

        Object result = provider.get();
        if (result instanceof Exception) {
          throw (Exception) result;
        }
        ResponseImpl.this.send(result);
      }

    };
  }

  @Override
  public void redirect(final Response.Status status, final String location) throws Exception {
    requireNonNull(status, "A status is required.");
    requireNonNull(location, "A location is required.");
    status(status);

    response.sendRedirect("back".equals(location) ? referer.orElse("/") : location);
  }

  @Override
  public Optional<Response.Status> status() {
    return Optional.ofNullable(status);
  }

  @Override
  public Response status(final Response.Status status) {
    this.status = requireNonNull(status, "A status is required.");
    this.committed = true;
    response.setStatus(status.value());
    return this;
  }

  @Override
  public Optional<MediaType> type() {
    return Optional.ofNullable(type);
  }

  @Override
  public Response type(final MediaType type) {
    this.type = requireNonNull(type, "Content-Type is required.");
    response.setContentType(type.name());
    return this;
  }

  void route(final Route route) {
    this.route = route;
  }

  @Override
  public String toString() {
    return route.toString();
  }
}
