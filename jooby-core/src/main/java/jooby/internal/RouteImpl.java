package jooby.internal;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import jooby.Filter;
import jooby.MediaType;
import jooby.Request;
import jooby.Response;
import jooby.Route;

import com.google.common.collect.ImmutableList;

public class RouteImpl implements Route, Filter {

  private static final List<MediaType> ALL = ImmutableList.of(MediaType.all);

  private Request.Verb verb;

  private String path;

  private String pattern;

  private String name;

  private Map<String, String> vars;

  private List<MediaType> consumes;

  private List<MediaType> produces;

  private Filter filter;

  public static RouteImpl notFound(final Request.Verb verb, final String path,
      final List<MediaType> produces) {
    return fromStatus((req, res, chain) -> {
      if (!res.committed()) {
        throw new Route.Err(Response.Status.NOT_FOUND, path);
      }
    }, verb, path, Response.Status.NOT_FOUND, produces);
  }

  public static RouteImpl fromStatus(final Filter filter, final Request.Verb verb,
      final String path, final Response.Status status, final List<MediaType> produces) {
    return new RouteImpl(filter, verb, path, path, status.value() + "", Collections.emptyMap(),
        ALL, produces);
  }

  public RouteImpl(final Filter filter, final Request.Verb verb, final String path,
      final String pattern, final String name, final Map<String, String> vars,
      final List<MediaType> consumes, final List<MediaType> produces) {
    this.filter = filter;
    this.verb = verb;
    this.path = path;
    this.pattern = pattern;
    this.name = name;
    this.vars = vars;
    this.consumes = consumes;
    this.produces = produces;
  }

  @Override
  public void handle(final Request request, final Response response, final Chain chain)
      throws Exception {
    filter.handle(request, response, chain);
  }

  @Override
  public String path() {
    return path;
  }

  @Override
  public Request.Verb verb() {
    return verb;
  }

  @Override
  public String pattern() {
    return pattern.substring(pattern.indexOf('/'));
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public Map<String, String> vars() {
    return vars;
  }

  @Override
  public List<MediaType> consume() {
    return consumes;
  }

  @Override
  public List<MediaType> produces() {
    return produces;
  }

  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder();
    buffer.append(verb()).append(" ").append(path()).append("\n");
    buffer.append("  pattern: ").append(pattern()).append("\n");
    buffer.append("  name: ").append(name()).append("\n");
    buffer.append("  vars: ").append(vars()).append("\n");
    buffer.append("  consume: ").append(consume()).append("\n");
    buffer.append("  produces: ").append(produces()).append("\n");
    return buffer.toString();
  }

}
