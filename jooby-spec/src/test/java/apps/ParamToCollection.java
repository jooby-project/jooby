package apps;

import org.jooby.Jooby;

public class ParamToCollection extends Jooby {

  {
    {
      get("/SingleGetCollectionParam", req -> {
        req.param("li").toList(Integer.class);
        req.param("ls").toList();
        req.param("ll").toList(LocalType.class);

        req.param("si").toSet(Integer.class);
        req.param("ss").toSet();
        req.param("sl").toSet(LocalType.class);

        req.param("ssi").toSortedSet(Integer.class);
        req.param("sss").toSortedSet();
        return "x";
      });
    }
  }

}
