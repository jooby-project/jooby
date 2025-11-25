/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.asciidoc;

import java.util.*;

import io.jooby.internal.openapi.OperationExt;
import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import io.swagger.v3.oas.models.Operation;

public enum Filters implements Filter {
  curl {
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

        var options = new LinkedHashMap<String, Set<String>>();
        options.put("-X", Set.of(null));
        operation
            .getProduces()
            .forEach(
                produces -> {
                  options
                      .computeIfAbsent("-H", (key) -> new LinkedHashSet<>())
                      .add("'Accept: " + produces + "'");
                });

        // Convert to map so can override any generated option
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
          var values = options.computeIfAbsent(key, k -> new LinkedHashSet<>());
          if (value != null) {
            values.add(value);
          }
        }
        return snippetResolver.apply(
            "curl", Map.of("url", operation.getPattern(), "options", toString(options)));
      } catch (PebbleException pebbleException) {
        throw pebbleException;
      } catch (Exception exception) {
        throw new PebbleException(exception, name() + " failed to generate output");
      }
    }

    private String toString(Map<String, Set<String>> options) {
      if (options.isEmpty()) {
        return "";
      }
      var sb = new StringBuilder();
      var separator = " ";
      for (var e : options.entrySet()) {
        var values = e.getValue();
        if (values.isEmpty()) {
          sb.append(e.getKey()).append(separator);
        } else {
          for (var value : e.getValue()) {
            sb.append(e.getKey()).append(separator);
            sb.append(value).append(separator);
          }
        }
      }
      sb.deleteCharAt(sb.length() - separator.length());
      return sb.toString();
    }

    @Override
    public List<String> getArgumentNames() {
      return null;
    }
  };

  public static Map<String, Filter> fn() {
    Map<String, Filter> functions = new HashMap<>();
    for (var value : values()) {
      functions.put(value.name(), value);
    }
    return functions;
  }
}
