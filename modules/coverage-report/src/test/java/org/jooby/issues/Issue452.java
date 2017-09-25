package org.jooby.issues;

import org.jooby.jdbc.Jdbc;
import org.jooby.jdbi.Jdbi;
import org.jooby.test.ServerFeature;
import org.junit.Test;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.util.StringColumnMapper;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class Issue452 extends ServerFeature {

  {

    use(ConfigFactory.empty().withValue("db.url", ConfigValueFactory
        .fromAnyRef("jdbc:h2:mem:mdb?DB_CLOSE_DELAY=-1&useEncoding=true&characterEncoding=UTF-8")));

    use(new Jdbc());
    use(new Jdbi());

    get("/452", req -> {
      try (Handle h = req.require("db", Handle.class)) {
        h.execute("create table something (id int primary key, name varchar(100))");

        h.execute("insert into something (id, name) values (?, ?)", 1, "Jooby");

        String name = h.createQuery("select name from something where id = :id")
            .bind("id", 1)
            .map(StringColumnMapper.INSTANCE)
            .first();

        return name;
      }
    });

  }

  @Test
  public void mustSupportURLParams() throws Exception {
    request()
        .get("/452")
        .expect("Jooby");
  }
}
