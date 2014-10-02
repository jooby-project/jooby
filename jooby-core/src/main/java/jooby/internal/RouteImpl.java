package jooby.internal;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import jooby.Filter;
import jooby.HttpStatus;
import jooby.MediaType;
import jooby.Request;
import jooby.Response;
import jooby.Route;
import jooby.RouteChain;
import jooby.RouteDefinition;
import jooby.RouteMatcher;

public class RouteImpl implements Route {

  private String verb;

  private String path;

  private String pattern;

  private String name;

  private int index;

  private Map<String, String> vars;

  private List<MediaType> consumes;

  private List<MediaType> produces;

  public static Route fromDefinition(final String verb, final RouteDefinition definition,
      final RouteMatcher matcher) {
    return new RouteImpl(verb, matcher.path(), matcher.pattern().pattern(), definition.name(),
        definition.index(), matcher.vars(), definition.consumes(), definition.produces()) {
      @Override
      public void handle(final Request req, final Response res, final RouteChain chain)
          throws Exception {
        ((RouteDefinitionImpl) definition).handle(req, res, chain);
      }
    };
  }

  public static Route notFound(final String verb, final String path) {
    return new RouteImpl(verb, path, path, HttpStatus.NOT_FOUND.value() + "",
        -1, Collections.emptyMap(), Collections.emptyList(), Collections.emptyList());
  }

  public static Route fromStatus(final String verb, final String path, final HttpStatus status,
      final Filter filter) {
    return new RouteImpl(verb, path, path, status.value() + "", -1, Collections.emptyMap(),
        Collections.emptyList(), Collections.emptyList()) {
      @Override
      public void handle(final Request req, final Response res, final RouteChain chain)
          throws Exception {
        filter.handle(req, res, chain);
      }
    };
  }

  private RouteImpl(final String verb, final String path, final String pattern, final String name,
      final int index, final Map<String, String> vars, final List<MediaType> consumes,
      final List<MediaType> produces) {
    this.verb = verb;
    this.path = path;
    this.pattern = pattern;
    this.name = name;
    this.index = index;
    this.vars = vars;
    this.consumes = consumes;
    this.produces = produces;
  }

  public void handle(final Request req, final Response res, final RouteChain chain)
      throws Exception {
  }

  @Override
  public String path() {
    return path;
  }

  @Override
  public String verb() {
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
  public int index() {
    return index;
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
    buffer.append("  index: ").append(index()).append("\n");
    buffer.append("  name: ").append(name()).append("\n");
    buffer.append("  vars: ").append(vars()).append("\n");
    buffer.append("  consume: ").append(consume()).append("\n");
    buffer.append("  produces: ").append(produces()).append("\n");
    return buffer.toString();
  }
}
