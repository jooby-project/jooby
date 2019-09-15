/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.converter;

import io.jooby.ValueNode;
import io.jooby.ValueConverter;

import io.jooby.SneakyThrows;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

public class URIConverter implements ValueConverter {
  @Override public boolean supports(Class type) {
    return type == URI.class || type == URL.class;
  }

  @Override public Object convert(ValueNode value, Class type) {
    try {
      URI uri = URI.create(value.value());
      if (type == URL.class) {
        return uri.toURL();
      }
      return uri;
    } catch (MalformedURLException x) {
      throw SneakyThrows.propagate(x);
    }
  }
}
