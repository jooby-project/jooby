package org.jooby.internal.apitool;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.google.inject.util.Types;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class TypeJsonDeserializer extends JsonDeserializer<Type> {
  @Override public Type deserialize(final JsonParser p, final DeserializationContext ctxt)
      throws IOException {
    String type = p.getValueAsString();
    if (type != null) {
      ClassLoader loader = getClass().getClassLoader();
      try {
        return BytecodeRouteParser.loadType(loader, type);
      } catch (Exception x) {
        return parse(loader, type, 0).get(0);
      }
    }
    return null;
  }

  static List<Type> parse(final ClassLoader loader, final String type) {
    return parse(loader, type, 0);
  }

  private static List<Type> parse(final ClassLoader loader, final String type, final int start) {
    List<Type> types = new ArrayList<>();
    StringBuilder singleType = new StringBuilder();
    for (int i = start; i < type.length(); i++) {
      char ch = type.charAt(i);
      if (ch == '<') {
        Type owner = BytecodeRouteParser.loadType(loader, singleType.toString());
        List<Type> parameters = parse(loader, type, i + 1);
        return Arrays.asList(
            Types.newParameterizedType(owner, parameters.toArray(new Type[parameters.size()])));
      } else if (ch == ',') {
        Type element = BytecodeRouteParser.loadType(loader, singleType.toString());
        types.add(element);
        singleType.setLength(0);
      } else if (ch == '>') {
        Type element = BytecodeRouteParser.loadType(loader, singleType.toString());
        types.add(element);
        break;
      } else {
        singleType.append(ch);
      }
    }
    return types;
  }
}
