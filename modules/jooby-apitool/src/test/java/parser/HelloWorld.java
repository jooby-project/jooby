package parser;

import com.google.common.collect.ImmutableMap;
import org.jooby.Jooby;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HelloWorld extends Jooby {

  public static class InnerFoo {

  }

  {

    get("/map-level", req -> {
      Map<String, Foo> foos = new HashMap<>();
      return foos;
    });

    get("/two-level", req -> {
      GenFoo<GenFoo<Foo>> foo = new GenFoo<>();
      return foo;
    });

    get("/", req -> {
      String foo = req.param("foo").value("x");
      String bar = req.param("bar").value("y");

      return foo.hashCode() + bar.hashCode();
    });

    get("/primitives-param", req -> {
      boolean bool = req.param("bool").booleanValue(true);
      char cval = req.param("c").charValue();
      byte bval = req.param("b").byteValue();
      short sval = req.param("s").shortValue();
      int ival = req.param("i").intValue(1);
      long lval = req.param("l").longValue(0L);
      float fval = req.param("f").floatValue();
      double dval = req.param("d").doubleValue(.54);
      String strval = req.param("str").value(null);

      System.out.printf("%s", bval, sval, ival, fval, lval, dval, cval, strval, bool);
      return 1;
    });

    get("/static-str", req -> {
      return "foo";
    });

    get("/object-type", req -> {
      ExtR extR = new ExtR();
      extR.external(req);
      return extR;
    });

    get("/gen-type", req -> {
      GenFoo<Foo> foo = new GenFoo<>();
      System.out.println(foo);
      return foo;
    });

    get("/inline-map", req -> {
      return ImmutableMap.of("k", "v");
    });

    get("/api", req -> {
      int v = req.param("x").intValue();
      int count = req.param("count").intValue();
      List<String> stringlist = new ArrayList<>();
      for (int i = 0; i < count; i++) {
        stringlist.add(Integer.toString(i + v));
      }
      return stringlist;
    });

  }
}
