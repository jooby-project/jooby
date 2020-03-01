package examples;

import io.jooby.Jooby;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RouteQueryArgs extends Jooby {
  {
    get("/", ctx -> {
      String str = ctx.query("str").value();
      int i = ctx.query("i").intValue();
      List<String> listStr = ctx.query("listStr").toList();
      List<Double> listType = ctx.query("listType").toList(Double.class);
      String defstr = ctx.query("defstr").value("200");
      int defint = ctx.query("defint").intValue(87);
      int defint0 = ctx.query("defint0").intValue(0);
      boolean defbool = ctx.query("defbool").booleanValue(true);
      Optional<String> defoptional = ctx.query("optstr").toOptional();
      Optional<Integer> intoptional = ctx.query("optint").toOptional(Integer.class);
      String str2 = ctx.query("optstr2").toOptional().orElse("optional");
      Integer toInt = ctx.query("toI").to(Integer.class);
      Letter l = ctx.query("letter").toEnum(Letter::valueOf);
      Map<String, String> q = ctx.query("query").toMap();
      Map<String, List<String>> qa = ctx.query("queryList").toMultimap();
      ABean bean = ctx.query(ABean.class);
      return "...";
    });
  }
}
