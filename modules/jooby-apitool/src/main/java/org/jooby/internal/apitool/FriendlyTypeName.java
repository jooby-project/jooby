package org.jooby.internal.apitool;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.ArrayType;
import com.fasterxml.jackson.databind.type.CollectionLikeType;
import com.fasterxml.jackson.databind.type.MapLikeType;
import com.fasterxml.jackson.databind.type.SimpleType;
import io.swagger.util.Json;

import java.lang.reflect.Type;

public class FriendlyTypeName {

  public static  String name(Type type) {
    return name(Json.mapper().constructType(type));
  }

  public static String name(JavaType type) {
    if (type instanceof ArrayType) {
      return "Array[" + name(type.getContentType()) + "]";
    } else if (type instanceof CollectionLikeType) {
      return type.getRawClass().getSimpleName() + "[" + name(type.getContentType()) + "]";
    } else if (type instanceof MapLikeType) {
      return "Map[" + name(type.getKeyType()) + ":" + name(type.getContentType()) + "]";
    } else if (type instanceof SimpleType) {
      String name = type.getRawClass().getSimpleName();
      StringBuilder args = new StringBuilder();
      for (int i = 0; i < type.containedTypeCount(); i++) {
        args.append(name(type.containedType(i))).append(",");
      }
      if (args.length() > 0) {
        args.insert(0, '[');
        args.setCharAt(args.length() - 1, ']');
      }
      return name + args;
    }
    return type.getRawClass().getSimpleName();
  }
}
