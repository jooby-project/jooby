package org.jooby.internal;

import static java.util.Objects.requireNonNull;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import org.jooby.BodyConverter;
import org.jooby.Cookie;
import org.jooby.MediaType;
import org.jooby.MediaTypeProvider;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Session;
import org.jooby.Upload;
import org.jooby.Variant;
import org.jooby.internal.jetty.JoobySession;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.typesafe.config.Config;

public class RequestImpl implements Request {

  private Injector injector;

  private BodyConverterSelector selector;

  private Charset charset;

  private Locale locale;

  private List<MediaType> accept;

  private MediaType type;

  // TODO: make route abstract? or throw UnsupportedException
  private Route route;

  private HttpServletRequest request;

  public RequestImpl(
      final HttpServletRequest request,
      final Injector injector,
      final Route route,
      final BodyConverterSelector selector,
      final MediaType contentType,
      final List<MediaType> accept,
      final Charset charset,
      final Locale locale) {
    this.injector = requireNonNull(injector, "An injector is required.");
    this.request = requireNonNull(request, "The request is required.");
    this.route = requireNonNull(route, "A route is required.");
    this.selector = requireNonNull(selector, "A message converter selector is required.");
    this.type = requireNonNull(contentType, "A contentType is required.");
    this.accept = requireNonNull(accept, "An accept is required.");
    this.charset = requireNonNull(charset, "A charset is required.");
    this.locale = requireNonNull(locale, "A locale is required.");
  }

  @Override
  public String path() {
    return route().path();
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
    List<MediaType> candidates = new LinkedList<>(types);
    Collections.sort(candidates);
    return MediaType.matcher(accept).first(candidates);
  }

  @Override
  public Variant param(final String name) throws Exception {
    requireNonNull(name, "Parameter's name is missing.");
    List<String> values = params(name);
    if (values.isEmpty()) {
      List<Upload> files = reqUploads(name);
      if (files.size() > 0) {
        return new GetUpload(name, files);
      }
    }
    MediaType type = MediaType.all;
    if (type().name().startsWith(MediaType.multipart.name())) {
      Part part = request.getPart(name);
      if (part != null) {
        type = Optional.ofNullable(part.getContentType()).map(MediaType::valueOf)
            .orElse(MediaType.all);
      }
    }
    return newVariant(name, values, type);
  }

  private Variant newVariant(final String name, final List<String> values, final MediaType type) {
    return new VariantImpl(injector, name, values, type, charset);
  }

  @Override
  public Map<String, Variant> params() throws Exception {
    Set<String> names = paramNames();
    if (names.size() == 0) {
      return Collections.emptyMap();
    }
    Map<String, Variant> params = new LinkedHashMap<>();
    for (String name : paramNames()) {
      params.put(name, param(name));
    }
    return params;
  }

  @Override
  public String ip() {
    return request.getRemoteAddr();
  }

  @Override
  public String hostname() {
    return request.getRemoteHost();
  }

  @Override
  public String protocol() {
    return request.getProtocol();
  }

  @Override
  public Session session() {
    JoobySession session = (JoobySession) request.getSession(true);
    return session;
  }

  private Set<String> paramNames() {
    Set<String> names = new LinkedHashSet<>();
    // path var
    for (String name : route().vars().keySet()) {
      names.add(name);
    }
    // param names
    Enumeration<String> e = request.getParameterNames();
    while (e.hasMoreElements()) {
      names.add(e.nextElement());
    }
    return names;
  }

  @Override
  public Variant header(final String name) {
    requireNonNull(name, "Header's name is missing.");
    return newVariant(name, enumToList(request.getHeaders(name)), MediaType.all);
  }

  @Override
  public Map<String, Variant> headers() {
    Map<String, Variant> headers = new LinkedHashMap<>();
    Enumeration<String> names = request.getHeaderNames();
    while (names.hasMoreElements()) {
      String name = names.nextElement();
      headers.put(name, header(name));
    }
    return headers;
  }

  @Override
  public <T> T body(final TypeLiteral<T> type) throws Exception {
    BodyConverter mapper = selector.forRead(type, Arrays.asList(this.type))
        .orElseThrow(() -> new Route.Err(Response.Status.UNSUPPORTED_MEDIA_TYPE));
    return mapper.read(type, new BodyReaderImpl(charset, () -> request.getInputStream()));
  }

  @Override
  public Cookie cookie(final String name) {
    return cookies(request).stream().filter(c -> c.name().equals(name)).findFirst().orElse(null);
  }

  @Override
  public List<Cookie> cookies() {
    return cookies(request);
  }

  private static List<Cookie> cookies(final HttpServletRequest request) {
    javax.servlet.http.Cookie[] cookies = request.getCookies();
    if (cookies == null || cookies.length == 0) {
      return Collections.emptyList();
    }
    return Arrays.stream(cookies)
        .map(c -> new Cookie.Definition(c.getName(), c.getValue())
            .comment(c.getComment())
            .domain(c.getDomain())
            .httpOnly(c.isHttpOnly())
            .maxAge(c.getMaxAge())
            .path(c.getPath())
            .secure(c.getSecure())
            .toCookie()
        )
        .collect(Collectors.toList());
  }

  @Override
  public <T> T getInstance(final Key<T> key) {
    return injector.getInstance(key);
  }

  public void destroy() {
    this.selector = null;
    this.type = null;
    this.request = null;
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
  public Route route() {
    return route;
  }

  void route(final Route route) {
    this.route = requireNonNull(route, "A route is required.");
  }

  @Override
  public String toString() {
    return route().toString();
  }

  protected List<Upload> reqUploads(final String name) throws Exception {
    if (!type().name().startsWith(MediaType.multipart.name())) {
      return Collections.emptyList();
    }
    Collection<Part> parts = request.getParts();
    if (parts == null || parts.size() == 0) {
      return Collections.emptyList();
    }
    Config config = getInstance(Config.class);
    MediaTypeProvider typeProvider = getInstance(MediaTypeProvider.class);
    String workDir = config.getString("java.io.tmpdir");
    return FluentIterable
        .from(parts)
        .filter(p -> p.getSubmittedFileName() != null && p.getName().equals(name))
        .transform(
            p -> {
              Upload upload = new PartUpload(injector, p, typeProvider.forPath(p
                  .getSubmittedFileName()), charset, workDir);
              return upload;
            })
        .toList();
  }

  private List<String> params(final String name) {
    String var = route().vars().get(name);
    if (var != null) {
      return ImmutableList.<String> builder().add(var).addAll(reqParams(name)).build();
    }
    return reqParams(name);
  }

  protected List<String> reqParams(final String name) {
    String[] values = request.getParameterValues(name);
    if (values == null) {
      return Collections.emptyList();
    }
    return ImmutableList.copyOf(values);
  }

  private List<String> enumToList(final Enumeration<String> e) {
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    while (e.hasMoreElements()) {
      builder.add(e.nextElement());
    }
    return builder.build();
  }

}
