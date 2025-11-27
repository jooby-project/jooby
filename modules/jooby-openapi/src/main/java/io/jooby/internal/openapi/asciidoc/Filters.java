/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.asciidoc;

import java.util.*;

import com.google.common.base.Splitter;
import com.google.common.collect.*;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.internal.openapi.OperationExt;
import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import io.swagger.v3.oas.models.Operation;

public enum Filters implements Filter {
  curl {
    private static final CharSequence Accept = new HeaderName("Accept");
    private static final CharSequence ContentType = new HeaderName("Content-Type");

    @Override
    public Object apply(
        Object input,
        Map<String, Object> args,
        PebbleTemplate self,
        EvaluationContext context,
        int lineNumber)
        throws PebbleException {
      try {
        if (!(input instanceof OperationExt operation)) {
          throw new IllegalArgumentException(
              "Argument must be " + Operation.class.getName() + ". Got: " + input);
        }
        var snippetResolver = (SnippetResolver) context.getVariable("snippetResolver");
        var options = args(args);
        var method =
            Optional.of(options.removeAll("-X"))
                .map(Collection::iterator)
                .filter(Iterator::hasNext)
                .map(Iterator::next)
                .orElse(operation.getMethod())
                .toUpperCase();
        /* Accept/Content-Type: */
        var addAccept = true;
        var addContentType = true;
        if (options.containsKey("-H")) {
          var headers = parseHeaders(options.get("-H"));
          addAccept = !headers.containsKey(Accept);
          addContentType = !headers.containsKey(ContentType);
        }
        if (addAccept) {
          operation.getProduces().forEach(value -> options.put("-H", "'Accept: " + value + "'"));
        }
        if (addContentType && !READ_METHODS.contains(method)) {
          operation
              .getConsumes()
              .forEach(value -> options.put("-H", "'Content-Type: " + value + "'"));
        }
        /* Method */
        if (!options.containsKey("-X")) {
          options.put("-X", method);
        }
        return snippetResolver.apply(
            name(), Map.of("url", operation.getPattern(), "options", toString(options)));
      } catch (PebbleException pebbleException) {
        throw pebbleException;
      } catch (Exception exception) {
        throw new PebbleException(exception, name() + " failed to generate output");
      }
    }

    @Override
    public List<String> getArgumentNames() {
      return null;
    }
  };

  protected Multimap<CharSequence, String> parseHeaders(Collection<String> headers) {
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

  protected static final Set<String> READ_METHODS = Set.of("GET", "HEAD");

  protected String toString(Multimap<String, String> options) {
    if (options.isEmpty()) {
      return "";
    }
    var sb = new StringBuilder();
    var separator = " ";
    options.forEach(
        (k, v) -> {
          sb.append(k).append(separator);
          if (v != null && !v.isEmpty()) {
            sb.append(v).append(separator);
          }
        });
    sb.deleteCharAt(sb.length() - separator.length());
    return sb.toString();
  }

  protected Multimap<String, String> args(Map<String, Object> args) {
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

  public static Map<String, Filter> fn() {
    Map<String, Filter> functions = new HashMap<>();
    for (var value : values()) {
      functions.put(value.name(), value);
    }
    return functions;
  }

  protected record HeaderName(String value) implements CharSequence {

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
    public String toString() {
      return value;
    }
  }
}
