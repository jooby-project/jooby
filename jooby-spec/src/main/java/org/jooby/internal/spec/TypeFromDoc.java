package org.jooby.internal.spec;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.javaparser.ast.Node;
import com.google.inject.util.Types;

public class TypeFromDoc {

  private static final Pattern TYPE = Pattern.compile("\\{@link\\s+([^\\}]+)\\}");


  public static Optional<Type> parse(final Node node, final Context ctx, final String text) {
    Matcher matcher = TYPE.matcher(text);
    Type type = null;
    while (matcher.find()) {
      String link = matcher.group(1).trim();
      String stype = link.split("\\s+")[0];
      Optional<Type> resolvedType = ctx.resolveType(node, stype);
      if (resolvedType.isPresent()) {
        Type ittype = resolvedType.get();
        if (type != null) {
          type = Types.newParameterizedType(type, ittype);
        } else {
          type = ittype;
        }
      }
    }
    return Optional.ofNullable(type);
  }
}
