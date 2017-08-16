package parser;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;

import org.jooby.Jooby;

public class HeadersApp extends Jooby {

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
      return req.header("r1", "xss", "html").value(refval1);
    });

    get("/r2", req -> {
      int offset = req.header("offset").intValue(OFFSET);
      int max = req.header("max").intValue(MAX);
      return offset + max;
    });

    get("/r3", req -> {
      long offset = req.header("offset").longValue(LOFFSET);
      long max = req.header("max").longValue(LMAX);
      return offset + max;
    });

    get("/r4", req -> {
      boolean p1 = req.header("t").booleanValue(T);
      boolean p2 = req.header("f").booleanValue(false);
      return p1 && p2;
    });

    get("/r5", req -> {
      Optional<String> v1 = req.header("o1").toOptional();
      Optional<Integer> v2 = req.header("o2").toOptional(Integer.class);
      return v1.toString() + v2;
    });

    get("/r6", req -> {
      String v1 = req.header("e1").to(String.class);
      List<String> l1 = req.header("l1").toList();
      List<Integer> l2 = req.header("l2").toList(Integer.class);
      Set<String> s1 = req.header("s1").toSet();
      SortedSet<Long> s2 = req.header("s2").toSortedSet(Long.class);
      return v1 + l1 + l2 + s1 + s2;
    });

    get("/r7", req -> {
      Letter letter = req.header("enum1").toEnum(Letter.class);
      Letter a = req.header("enum2").toEnum(Letter.A);
      return letter.name() + a.name();
    });

  }
}
