package jooby.internal;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

public class RegexRouteMatcher implements RouteMatcher {

  private final Matcher matcher;

  private final List<String> varNames;

  private final Map<String, String> vars = new HashMap<>();

  private final String path;

  public RegexRouteMatcher(final String path, final Matcher matcher,
      final List<String> varNames) {
    this.path = requireNonNull(path, "A path is required.");
    this.matcher = requireNonNull(matcher, "A matcher is required.");
    this.varNames = requireNonNull(varNames, "The varNames are required.");
  }

  @Override
  public String path() {
    return path;
  }

  @Override
  public boolean matches() {
    boolean matches = matcher.matches();
    if (matches && varNames.size() > 0) {
      int varCount = matcher.groupCount();
      for (int idx = 0; idx < varCount; idx++) {
        vars.put(varNames.get(idx), matcher.group(idx + 1));
      }
    }
    return matches;
  }

  @Override
  public Map<String, String> vars() {
    return vars;
  }

}
