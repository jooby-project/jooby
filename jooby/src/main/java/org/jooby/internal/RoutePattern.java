/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.internal;

import static java.util.Objects.requireNonNull;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javaslang.Tuple;
import javaslang.Tuple4;

public class RoutePattern {

  private static final Pattern GLOB = Pattern
      .compile("\\?|/\\*\\*(:(?:[^/]+)+?)?+|\\*|\\:((?:[^/]+)+?)|\\{((?:\\{[^/]+?(?:[^/]+?\\*\\*)\\}|[^/{}]|\\\\[{}])+?)\\}");

  private static final Pattern SLASH = Pattern.compile("//+");

  private final Function<String, RouteMatcher> matcher;

  private String pattern;

  private List<String> vars;

  private List<String> reverse;

  private boolean glob;

  public RoutePattern(final String verb, final String pattern) {
    requireNonNull(verb, "A HTTP verb is required.");
    requireNonNull(pattern, "A path pattern is required.");
    this.pattern = normalize(pattern);
    Tuple4<Function<String, RouteMatcher>, List<String>, List<String>, Boolean> result = rewrite(this,
        verb.toUpperCase(), this.pattern.replace("/**/", "/**"));
    matcher = result._1;
    vars = result._2;
    reverse = result._3;
    glob = result._4;
  }

  public boolean glob() {
    return glob;
  }

  public List<String> vars() {
    return vars;
  }

  public String pattern() {
    return pattern;
  }

  public String reverse(final Map<String, Object> vars) {
    return reverse.stream()
        .map(segment -> vars.getOrDefault(segment, segment).toString())
        .collect(Collectors.joining(""));
  }

  public String reverse(final Object... value) {
    List<String> vars = vars();
    Map<String, Object> hash = new HashMap<>();
    for (int i = 0; i < Math.min(vars.size(), value.length); i++) {
      hash.put(vars.get(i), value[i]);
    }
    return reverse(hash);
  }

  public RouteMatcher matcher(final String path) {
    requireNonNull(path, "A path is required.");
    return matcher.apply(path);
  }

  private static Tuple4<Function<String, RouteMatcher>, List<String>, List<String>, Boolean> rewrite(
      final RoutePattern owner, final String verb, final String pattern) {
    List<String> vars = new LinkedList<>();
    String rwrverb = verbs(verb);
    StringBuilder patternBuilder = new StringBuilder(rwrverb);
    Matcher matcher = GLOB.matcher(pattern);
    int end = 0;
    boolean regex = !rwrverb.equals(verb);
    List<String> reverse = new ArrayList<>();
    boolean glob = false;
    while (matcher.find()) {
      String head = pattern.substring(end, matcher.start());
      patternBuilder.append(Pattern.quote(head));
      reverse.add(head);
      String match = matcher.group();
      if ("?".equals(match)) {
        patternBuilder.append("([^/])");
        reverse.add(match);
        regex = true;
        glob = true;
      } else if ("*".equals(match)) {
        patternBuilder.append("([^/]*)");
        reverse.add(match);
        regex = true;
        glob = true;
      } else if (match.equals("/**")) {
        reverse.add(match);
        patternBuilder.append("($|/.*)");
        regex = true;
        glob = true;
      } else if (match.startsWith("/**:")) {
        reverse.add(match.substring(1));
        String varName = match.substring(4);
        patternBuilder.append("/(?<v").append(vars.size()).append(">($|.*))");
        vars.add(varName);
        regex = true;
        glob = true;
      } else if (match.startsWith(":")) {
        regex = true;
        String varName = match.substring(1);
        patternBuilder.append("(?<v").append(vars.size()).append(">[^/]+)");
        vars.add(varName);
        reverse.add(varName);
      } else if (match.startsWith("{") && match.endsWith("}")) {
        regex = true;
        int colonIdx = match.indexOf(':');
        if (colonIdx == -1) {
          String varName = match.substring(1, match.length() - 1);
          patternBuilder.append("(?<v").append(vars.size()).append(">[^/]+)");
          vars.add(varName);
          reverse.add(varName);
        } else {
          String varName = match.substring(1, colonIdx);
          String regexpr = match.substring(colonIdx + 1, match.length() - 1);
          patternBuilder.append("(?<v").append(vars.size()).append(">");
          patternBuilder.append("**".equals(regexpr) ? "($|.*)" : regexpr);
          patternBuilder.append(')');
          vars.add(varName);
          reverse.add(varName);
        }
      }
      end = matcher.end();
    }
    String tail = pattern.substring(end, pattern.length());
    reverse.add(tail);
    patternBuilder.append(Pattern.quote(tail));
    return Tuple.of(fn(owner, regex, regex ? patternBuilder.toString() : verb + pattern, vars),
        vars, reverse, glob);
  }

  private static String verbs(final String verb) {
    String[] verbs = verb.split("\\|");
    if (verbs.length == 1) {
      return verb.equals("*") ? "(?:[^/]*)" : verb;
    }
    return "(?:" + verb + ")";
  }

  private static Function<String, RouteMatcher> fn(final RoutePattern owner, final boolean complex,
      final String pattern, final List<String> vars) {
    return new Function<String, RouteMatcher>() {
      final Pattern regex = complex ? Pattern.compile(pattern) : null;

      @Override
      public RouteMatcher apply(final String fullpath) {
        String path = fullpath.substring(fullpath.indexOf('/'));
        return complex
            ? new RegexRouteMatcher(path, regex.matcher(fullpath), vars)
            : new SimpleRouteMatcher(pattern, path, fullpath);
      }
    };
  }

  public static String normalize(final String pattern) {
    if (pattern.equals("*")) {
      return "/**";
    }
    if (pattern.equals("/")) {
      return "/";
    }
    String normalized = SLASH.matcher(pattern).replaceAll("/");
    if (normalized.equals("/")) {
      return "/";
    }
    StringBuilder buffer = new StringBuilder();
    if (!normalized.startsWith("/")) {
      buffer.append("/");
    }
    buffer.append(normalized);
    if (normalized.endsWith("/")) {
      buffer.setLength(buffer.length() - 1);
    }
    return buffer.toString();
  }

  @Override
  public String toString() {
    return pattern;
  }

}
