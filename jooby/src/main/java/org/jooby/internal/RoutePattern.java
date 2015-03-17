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

import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RoutePattern {

  private static final Pattern GLOB = Pattern
      .compile("\\?|\\*\\*|\\*|\\:((?:[^/]+)+?)|\\{((?:\\{[^/]+?\\}|[^/{}]|\\\\[{}])+?)\\}");

  private static final Pattern SLASH = Pattern.compile("//+");

  private final Function<String, RouteMatcher> matcher;

  private String pattern;

  public RoutePattern(final String verb, final String pattern) {
    requireNonNull(verb, "A HTTP verb is required.");
    requireNonNull(pattern, "A path pattern is required.");
    this.pattern = normalize(pattern);
    this.matcher = rewrite(this, verb.toUpperCase() + this.pattern.replace("/**/", "/**"));
  }

  public String pattern() {
    return pattern;
  }

  public RouteMatcher matcher(final String path) {
    requireNonNull(path, "A path is required.");
    return matcher.apply(path);
  }

  private static Function<String, RouteMatcher> rewrite(final RoutePattern owner,
      final String pattern) {
    List<String> vars = new LinkedList<>();
    StringBuilder patternBuilder = new StringBuilder();
    Matcher matcher = GLOB.matcher(pattern);
    int end = 0;
    boolean regex = false;
    while (matcher.find()) {
      patternBuilder.append(quote(pattern, end, matcher.start()));
      String match = matcher.group();
      if ("?".equals(match)) {
        patternBuilder.append("([^/])");
        regex = true;
      } else if ("*".equals(match)) {
        patternBuilder.append("([^/]*)");
        regex = true;
      } else if (match.equals("**")) {
        patternBuilder.append("(.*)");
        regex = true;
      } else if (match.startsWith(":")) {
        regex = true;
        String varName = match.substring(1);
        patternBuilder.append("(?<v").append(vars.size()).append(">[^/]+)");
        vars.add(varName);
      } else if (match.startsWith("{") && match.endsWith("}")) {
        regex = true;
        int colonIdx = match.indexOf(':');
        if (colonIdx == -1) {
          String varName = match.substring(1, match.length() - 1);
          patternBuilder.append("(?<v").append(vars.size()).append(">[^/]+)");
          vars.add(varName);
        }
        else {
          String varName = match.substring(1, colonIdx);
          String regexpr = match.substring(colonIdx + 1, match.length() - 1);
          patternBuilder.append("(?<v").append(vars.size()).append(">");
          patternBuilder.append(regexpr);
          patternBuilder.append(')');
          vars.add(varName);
        }
      }
      end = matcher.end();
    }
    patternBuilder.append(quote(pattern, end, pattern.length()));
    return fn(owner, regex, regex ? patternBuilder.toString() : pattern, vars);
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

  private static String quote(final String s, final int start, final int end) {
    if (start == end) {
      return "";
    }
    return Pattern.quote(s.substring(start, end));
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
      buffer.setLength(buffer.length() - 1);;
    }
    return buffer.toString();
  }

  @Override
  public String toString() {
    return pattern;
  }
}
