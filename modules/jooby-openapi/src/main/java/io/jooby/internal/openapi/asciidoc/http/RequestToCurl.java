/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.asciidoc.http;

import java.util.*;

import com.google.common.base.Splitter;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Router;
import io.jooby.internal.openapi.asciidoc.*;

public class RequestToCurl implements ToSnippet {
  private static final CharSequence Accept = new HeaderName("Accept");
  private static final CharSequence ContentType = new HeaderName("Content-Type");

  private final AsciiDocContext context;
  private final HttpRequest request;

  public RequestToCurl(AsciiDocContext context, HttpRequest request) {
    this.context = context;
    this.request = request;
  }

  @Override
  public String render(Map<String, Object> args) {
    var options = args(args);
    var method = removeOption(options, "-X", request.getMethod()).toUpperCase();
    var language = removeOption(options, "language", null);
    /* Accept/Content-Type: */
    var addAccept = true;
    var addContentType = true;
    if (options.containsKey("-H")) {
      var headers = parseHeaders(options.get("-H"));
      addAccept = !headers.containsKey(Accept);
      addContentType = !headers.containsKey(ContentType);
    }
    if (addAccept) {
      request.getProduces().forEach(value -> options.put("-H", "'Accept: " + value + "'"));
    }
    if (addContentType
        && Set.of(Router.PATCH, Router.PUT, Router.POST, Router.DELETE).contains(method)) {
      request.getConsumes().forEach(value -> options.put("-H", "'Content-Type: " + value + "'"));
    }
    /* Body */
    var formUrlEncoded =
        request.formUrlEncoded(
            (schema, field) -> {
              var option = "--data-urlencode";
              var value = field.getValue();
              if ("binary".equals(schema.getFormat())) {
                option = "-F";
                value = "@/file%1$s.extension";
              }
              return Map.entry(option, "\"" + field.getKey() + "=" + value + "\"");
            });
    if (formUrlEncoded.isEmpty()) {
      var body = request.getBody();
      if (body != AsciiDocContext.EMPTY_SCHEMA) {
        options.put("-d", "'" + context.toJson(context.schemaProperties(body), false) + "'");
      }
    } else {
      formUrlEncoded.forEach(options::put);
    }

    /* Method */
    var url = request.getPath() + request.getQueryString();
    options.put("-X", method + " '" + url + "'");
    return toString(options, language);
  }

  private String toString(Multimap<String, String> options, String language) {
    var curl = "curl";
    var sb = new StringBuilder();
    sb.append("[source");
    if (language != null) {
      sb.append(", ").append(language);
    }
    sb.append("]\n----\n").append(curl);
    var separator = "\\\n";
    var tabSize = 1;
    for (var entry : options.entries()) {
      var k = entry.getKey();
      var v = entry.getValue();
      sb.append(" ".repeat(tabSize));
      sb.append(k);
      if (v != null && !v.isEmpty()) {
        sb.append(" ").append(v);
      }
      sb.append(separator);
      tabSize = curl.length() + 1;
    }
    sb.setLength(sb.length() - separator.length());
    sb.append("\n----");
    return sb.toString();
  }

  private Multimap<CharSequence, String> parseHeaders(Collection<String> headers) {
    Multimap<CharSequence, String> result = LinkedHashMultimap.create();
    for (var line : headers) {
      if (line.startsWith("'") && line.endsWith("'")) {
        line = line.substring(1, line.length() - 1);
      }
      var header = Splitter.on(':').trimResults().omitEmptyStrings().splitToList(line);
      if (header.size() != 2) {
        throw new IllegalArgumentException("Invalid header: " + line);
      }
      result.put(new HeaderName(header.get(0)), header.get(1));
    }
    return result;
  }

  @NonNull private static String removeOption(
      Multimap<String, String> options, String name, String defaultValue) {
    return Optional.of(options.removeAll(name))
        .map(Collection::iterator)
        .filter(Iterator::hasNext)
        .map(Iterator::next)
        .orElse(defaultValue);
  }

  private Multimap<String, String> args(Map<String, Object> args) {
    Multimap<String, String> result = LinkedHashMultimap.create();
    var optionList = new ArrayList<>(args.values());
    for (int i = 0; i < optionList.size(); ) {
      var key = optionList.get(i).toString();
      String value = null;
      if (i + 1 < optionList.size()) {
        var next = optionList.get(i + 1);
        if (next.toString().startsWith("-")) {
          i += 1;
        } else {
          value = next.toString();
          i += 2;
        }
      } else {
        i += 1;
      }
      result.put(key, value == null ? "" : value);
    }
    return result;
  }

  private record HeaderName(String value) implements CharSequence {

    @Override
    public int length() {
      return value.length();
    }

    @Override
    public boolean equals(Object obj) {
      return value.equalsIgnoreCase(obj.toString());
    }

    @Override
    public int hashCode() {
      return value.toLowerCase().hashCode();
    }

    @Override
    public char charAt(int index) {
      return value.charAt(index);
    }

    @NonNull @Override
    public CharSequence subSequence(int start, int end) {
      return value.subSequence(start, end);
    }

    @Override
    @NonNull public String toString() {
      return value;
    }
  }
}
