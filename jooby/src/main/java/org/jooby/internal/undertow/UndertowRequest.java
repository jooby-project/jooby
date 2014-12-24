package org.jooby.internal.undertow;

import static java.util.Objects.requireNonNull;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormEncodedDataDefinition;
import io.undertow.server.handlers.form.MultiPartParserDefinition;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionManager;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.jooby.Body.Parser;
import org.jooby.Cookie;
import org.jooby.Err;
import org.jooby.MediaType;
import org.jooby.Mutant;
import org.jooby.Request;
import org.jooby.Route;
import org.jooby.Session;
import org.jooby.Status;
import org.jooby.Upload;
import org.jooby.fn.Collectors;
import org.jooby.internal.BodyConverterSelector;
import org.jooby.internal.BodyReaderImpl;
import org.jooby.internal.MutantImpl;
import org.jooby.internal.UploadMutant;
import org.jooby.internal.reqparam.BeanParamInjector;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.typesafe.config.Config;

public class UndertowRequest implements Request {

  interface ExchangeFn<T> {
    T get(HttpServerExchange exchange) throws IOException;
  }

  private static class MemoizingExchangeFn<T> implements ExchangeFn<T> {

    private ExchangeFn<T> fn;

    private T value;

    private T defaultValue;

    public MemoizingExchangeFn(final ExchangeFn<T> fn, final T defaultValue) {
      this.fn = fn;
      this.defaultValue = defaultValue;
    }

    @Override
    public T get(final HttpServerExchange exchange) throws IOException {
      if (value == null) {
        T form = fn.get(exchange);
        this.value = form == null ? defaultValue : form;
      }
      return this.value;
    }

  }

  private HttpServerExchange exchange;

  private Injector injector;

  // TODO: make route abstract? or throw UnsupportedException
  private Route route;

  private Map<String, Object> locals;

  private BodyConverterSelector selector;

  private MediaType type;

  private List<MediaType> accept;

  private Charset charset;

  private Locale locale;

  private final ExchangeFn<FormData> formParser;

  public UndertowRequest(final HttpServerExchange exchange,
      final Injector injector,
      final Route route,
      final Map<String, Object> locals,
      final BodyConverterSelector selector,
      final MediaType contentType,
      final List<MediaType> accept,
      final Charset charset,
      final Locale locale) {
    this.exchange = requireNonNull(exchange, "An exchange is required.");
    this.injector = requireNonNull(injector, "An injector is required.");
    this.route = requireNonNull(route, "A route is required.");
    this.locals = requireNonNull(locals, "The locals is required.");
    this.selector = requireNonNull(selector, "A message converter selector is required.");
    this.type = requireNonNull(contentType, "A contentType is required.");
    this.accept = requireNonNull(accept, "An accept header is required.");
    this.charset = requireNonNull(charset, "A charset is required.");
    this.locale = requireNonNull(locale, "A locale is required.");
    formParser = formParser(contentType, injector.getInstance(Config.class));
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> Optional<T> get(final String name) {
    requireNonNull(name, "A local's name is required.");
    return Optional.ofNullable((T) locals.get(name));
  }

  @Override
  public Map<String, Object> attributes() {
    return ImmutableMap.copyOf(locals);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> Optional<T> unset(final String name) {
    requireNonNull(name, "A local's name is required.");
    return Optional.ofNullable((T) locals.remove(name));
  }

  @Override
  public MediaType type() {
    return type;
  }

  @Override
  public List<MediaType> accept() {
    return accept;
  }

  @Override
  public Optional<MediaType> accepts(final List<MediaType> types) {
    requireNonNull(types, "Media types are required.");
    return MediaType.matcher(accept).first(types);
  }

  @Override
  public Map<String, Mutant> params() throws Exception {
    List<String> names = new ArrayList<>();
    // path vars
    names.addAll(route.vars().keySet());
    // query params
    names.addAll(exchange.getQueryParameters().keySet());

    // post params
    FormData form = formParser.get(exchange);
    form.forEach(name -> names.add(name));

    Map<String, Mutant> params = new LinkedHashMap<>();
    for (String name : names) {
      params.put(name, param(name));
    }
    return params;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T params(final Class<T> beanType) throws Exception {
    return (T) BeanParamInjector.createAndInject(this, beanType);
  }

  @Override
  public Mutant param(final String name) throws Exception {
    requireNonNull(name, "Parameter's name is missing.");
    String pathvalue = route.vars().get(name);
    Builder<String> builder = ImmutableList.builder();
    // path params
    if (pathvalue != null) {
      builder.add(pathvalue);
    }
    // query params
    Deque<String> qparams = exchange.getQueryParameters().get(name);
    if (qparams != null) {
      qparams.forEach(builder::add);
    }
    // form params
    FormData form = formParser.get(exchange);
    Deque<MediaType> types = new ArrayDeque<MediaType>(2);
    List<Upload> uploads = new ArrayList<Upload>();
    types.add(MediaType.all);
    Optional.ofNullable(form.get(name)).ifPresent(values -> {
      values.forEach(value -> {
        if (!value.isFile()) {
          Optional.ofNullable(value.getHeaders()).ifPresent(headers ->
              Optional.ofNullable(headers.get(Headers.CONTENT_TYPE))
                  .ifPresent(type -> types.addLast(MediaType.valueOf(type.getFirst())))
          );
          builder.add(value.getValue());
        } else {
          uploads.add(new UndertowUpload(injector, value, charset));
        }
      });
    });
    // TODO: FIXME, uploads should have his own method and we should NOT guess what user want.
    if (uploads.size() > 0) {
      return new UploadMutant(name, uploads);
    }
    return newVariant(name, builder.build(), types.getLast());
  }

  @Override
  public Mutant header(final String name) {
    requireNonNull(name, "Header's name is missing.");
    HeaderValues values = exchange.getRequestHeaders().get(name);
    return newVariant(name, values, MediaType.all);
  }

  @Override
  public Map<String, Mutant> headers() {
    Map<String, Mutant> result = new LinkedHashMap<>();
    HeaderMap headers = exchange.getRequestHeaders();
    headers.getHeaderNames().forEach(name -> {
      result.put(name.toString(),
          newVariant(name.toString(), headers.get(name), MediaType.all));
    });
    return result;
  }

  @Override
  public Optional<Cookie> cookie(final String name) {
    requireNonNull(name, "Cookie's name is missing.");
    Map<String, io.undertow.server.handlers.Cookie> cookies = exchange.getRequestCookies();
    io.undertow.server.handlers.Cookie cookie = cookies.get(name);
    if (cookie == null) {
      return Optional.empty();
    }

    return Optional.of(cookie(cookie));
  }

  @Override
  public List<Cookie> cookies() {
    return exchange.getRequestCookies().values().stream()
        .map(UndertowRequest::cookie)
        .collect(Collectors.toList());
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T body(final TypeLiteral<T> type) throws Exception {
    if (length() > 0) {
      Optional<Parser> parser = selector.parser(type, ImmutableList.of(this.type));
      if (parser.isPresent()) {
        return parser.get().parse(type, new BodyReaderImpl(charset,
            () -> exchange.getInputStream()));
      }
      if (MediaType.form.matches(type()) || MediaType.multipart.matches(type())) {
        return (T) BeanParamInjector.createAndInject(this, type.getRawType());
      }
      throw new Err(Status.UNSUPPORTED_MEDIA_TYPE);
    }
    throw new Err(Status.BAD_REQUEST, "no body");
  }

  @Override
  public <T> T getInstance(final Key<T> key) {
    return injector.getInstance(key);
  }

  @Override
  public Charset charset() {
    return charset;
  }

  @Override
  public Locale locale() {
    return locale;
  }

  @Override
  public long length() {
    return exchange.getRequestContentLength();
  }

  @Override
  public String ip() {
    InetSocketAddress sourceAddress = exchange.getSourceAddress();
    if (sourceAddress == null) {
      return "";
    }
    InetAddress address = sourceAddress.getAddress();
    return address == null ? "" : address.getHostAddress();
  }

  @Override
  public Route route() {
    return route;
  }

  @Override
  public String hostname() {
    InetSocketAddress sourceAddress = exchange.getSourceAddress();
    if (sourceAddress == null) {
      return "";
    }
    InetAddress address = sourceAddress.getAddress();
    return address == null ? "" : address.getHostName();
  }

  @Override
  public Session session() {
    return ifSession().orElseGet(() -> {
      SessionManager sm = exchange.getAttachment(SessionManager.ATTACHMENT_KEY);
      SessionConfig sessionConfig = exchange.getAttachment(SessionConfig.ATTACHMENT_KEY);
      Session session = (Session) sm.createSession(exchange, sessionConfig);
      return session;
    });
  }

  @Override
  public Optional<Session> ifSession() {
    SessionManager sm = exchange.getAttachment(SessionManager.ATTACHMENT_KEY);
    SessionConfig sessionConfig = exchange.getAttachment(SessionConfig.ATTACHMENT_KEY);
    Session session = (Session) sm.getSession(exchange, sessionConfig);
    return Optional.ofNullable(session);
  }

  @Override
  public String protocol() {
    return exchange.getProtocol().toString();
  }

  @Override
  public boolean secure() {
    return exchange.getRequestScheme().equalsIgnoreCase("https");
  }

  @Override
  public Request set(final String name, final Object value) {
    requireNonNull(name, "A local's name is required.");
    requireNonNull(value, "A local's value is required.");
    locals.put(name, value);
    return this;
  }

  @Override
  public Request unset() {
    locals.clear();
    return this;
  }


  public void route(final Route route) {
    this.route = requireNonNull(route, "A route is required.");
  }

  @Override
  public String toString() {
    return route().toString();
  }

  private ExchangeFn<FormData> formParser(final MediaType type, final Config config) {
    final ExchangeFn<FormData> parser;
    if (MediaType.form.name().equals(type.name())) {
      parser = (exchange) ->
          new FormEncodedDataDefinition()
              .setDefaultEncoding(charset.name())
              .create(exchange)
              .parseBlocking();
    } else if (MediaType.multipart.name().equals(type.name())) {
      parser = (exchange) -> new MultiPartParserDefinition()
          .setTempFileLocation(new File(config.getString("application.tmpdir")))
          .setDefaultEncoding(charset.name())
          .create(exchange)
          .parseBlocking();

    } else {
      parser = (exchange) -> new FormData(0);
    }
    return new MemoizingExchangeFn<FormData>(parser, new FormData(0));
  }

  private static Cookie cookie(final io.undertow.server.handlers.Cookie c) {
    Cookie.Definition cookie = new Cookie.Definition(c.getName(), c.getValue());
    Optional.ofNullable(c.getComment()).ifPresent(cookie::comment);
    Optional.ofNullable(c.getDomain()).ifPresent(cookie::domain);
    Optional.ofNullable(c.getPath()).ifPresent(cookie::path);
    Optional.ofNullable(c.getMaxAge()).ifPresent(cookie::maxAge);
    cookie.httpOnly(c.isHttpOnly());
    cookie.secure(c.isSecure());

    return cookie.toCookie();
  }

  private Mutant newVariant(final String name, final List<String> values, final MediaType type) {
    return new MutantImpl(injector, name, values, type, charset);
  }

}
