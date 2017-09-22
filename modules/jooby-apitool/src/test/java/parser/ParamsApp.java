package parser;

import org.jooby.Jooby;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;

public class ParamsApp extends Jooby {

  public static enum Letter {
    A, B, C;
  }

  static final String refval1 = "REF_VAL";
  private static final int OFFSET = 0;
  private static final int MAX = 50;

  private static final long LOFFSET = 0;
  private static final long LMAX = 200;

  private static final boolean T = true;

  {

    get("/r1", req -> {
      return req.param("r1", "xss", "html").value(refval1);
    });

    get("/r2", req -> {
      int offset = req.param("offset").intValue(OFFSET);
      int max = req.param("max").intValue(MAX);
      return offset + max;
    });

    get("/r3", req -> {
      long offset = req.param("offset").longValue(LOFFSET);
      long max = req.param("max").longValue(LMAX);
      Optional<Long> omax = req.param("max2").toOptional(Long.class);
      return offset + max;
    });

    get("/r4", req -> {
      boolean p1 = req.param("t").booleanValue(T);
      boolean p2 = req.param("f").booleanValue(false);
      return p1 && p2;
    });

    get("/r5", req -> {
      Optional<String> v1 = req.param("o1").toOptional();
      Optional<Integer> v2 = req.param("o2").toOptional(Integer.class);
      return v1.toString() + v2;
    });

    get("/r6", req -> {
      String v1 = req.param("e1").to(String.class);
      List<String> l1 = req.param("l1").toList();
      List<Integer> l2 = req.param("l2").toList(Integer.class);
      Set<String> s1 = req.param("s1").toSet();
      SortedSet<Long> s2 = req.param("s2").toSortedSet(Long.class);
      return v1 + l1 + l2 + s1 + s2;
    });

    get("/r7", req -> {
      Letter letter = req.param("enum1").toEnum(Letter.class);
      Letter a = req.param("enum2").toEnum(Letter.A);
      return letter.name() + a.name();
    });

  }
}
