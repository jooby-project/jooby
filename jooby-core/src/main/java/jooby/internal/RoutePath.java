package jooby.internal;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jooby.internal.mvc.Routes;

public class RoutePath {

  interface Segment {
    boolean matches(String segment);
  }

  private static final Pattern GLOB = Pattern
      .compile("\\?|\\*\\*/?|\\*|\\{((?:\\{[^/]+?\\}|[^/{}]|\\\\[{}])+?)\\}");

  private static final String ANY_DIR = "**";

  private final Function<String, RouteMatcher> matcher;

  public RoutePath(final String method, final String pattern) {
    this.matcher = rewrite(pattern(method, Routes.normalize(pattern)));
  }


  public RouteMatcher matcher(final String path) {
    return matcher.apply(path);
  }

  private static Function<String, RouteMatcher> rewrite(final String pattern) {
    List<String> vars = new LinkedList<>();
    StringBuilder patternBuilder = new StringBuilder();
    Matcher matcher = GLOB.matcher(pattern);
    int end = 0;
    boolean complex = false;
    while (matcher.find()) {
      patternBuilder.append(quote(pattern, end, matcher.start()));
      String match = matcher.group();
      if ("?".equals(match)) {
        patternBuilder.append("[^/]");
        complex = true;
      } else if ("*".equals(match)) {
        patternBuilder.append("[^/]*");
        complex = true;
      } else if ("**/".equals(match)) {
        patternBuilder.append("(.*/)*");
        complex = true;
      } else if (match.startsWith("{") && match.endsWith("}")) {
        complex = true;
        int colonIdx = match.indexOf(':');
        if (colonIdx == -1) {
          patternBuilder.append("([^/]*)");
          vars.add(matcher.group(1));
        }
        else {
          String regex = match.substring(colonIdx + 1, match.length() - 1);
          patternBuilder.append('(');
          patternBuilder.append(regex);
          patternBuilder.append(')');
          vars.add(match.substring(1, colonIdx));
        }
      }
      end = matcher.end();
    }
    patternBuilder.append(quote(pattern, end, pattern.length()));
    return fn(complex, complex ? patternBuilder.toString() : pattern, vars);
  }

  private static Function<String, RouteMatcher> fn(final boolean complex, final String pattern,
      final List<String> vars) {
    return new Function<String, RouteMatcher>() {
      final Pattern regex = complex ? Pattern.compile(pattern) : null;

      @Override
      public RouteMatcher apply(final String path) {
        return complex
            ? new RegexRouteMatcher(regex.matcher(path), vars)
            : new SimpleRouteMatcher(pattern, path);
      }

      @Override
      public String toString() {
        return pattern;
      }
    };
  }

  private static String quote(final String s, final int start, final int end) {
    if (start == end) {
      return "";
    }
    return Pattern.quote(s.substring(start, end));
  }

  private static String pattern(final String method, final String pattern) {
    StringBuilder buffer = new StringBuilder(method.toUpperCase());
    if (!pattern.startsWith("/")) {
      buffer.append("/");
    }
    buffer.append(pattern);
    if (pattern.endsWith(ANY_DIR)) {
      buffer.append("/*");
    }
    return buffer.toString();
  }

  @Override
  public String toString() {
    return matcher.toString();
  }
}
