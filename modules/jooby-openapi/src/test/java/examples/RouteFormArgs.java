/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import java.util.List;
import java.util.Optional;

import io.jooby.Jooby;

public class RouteFormArgs extends Jooby {
  {
    get(
        "/",
        ctx -> {
          String str = ctx.form("str").value();
          int i = ctx.form("i").intValue();
          List<String> listStr = ctx.form("listStr").toList();
          List<Double> listType = ctx.form("listType").toList(Double.class);
          String defstr = ctx.form("defstr").value("200");
          int defint = ctx.form("defint").intValue(87);
          int defint0 = ctx.form("defint0").intValue(0);
          boolean defbool = ctx.form("defbool").booleanValue(true);
          Optional<String> defoptional = ctx.form("optstr").toOptional();
          Optional<Integer> intoptional = ctx.form("optint").toOptional(Integer.class);
          String str2 = ctx.form("optstr2").toOptional().orElse("optional");
          Integer toInt = ctx.form("toI").to(Integer.class);
          Letter l = ctx.form("letter").toEnum(Letter::valueOf);
          return "...";
        });
  }
}
