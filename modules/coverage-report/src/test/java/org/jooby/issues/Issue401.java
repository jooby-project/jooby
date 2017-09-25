package org.jooby.issues;

import org.jooby.jdbc.Jdbc;
import org.jooby.jdbi.Jdbi;
import org.jooby.test.ServerFeature;
import org.junit.Test;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.util.StringColumnMapper;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class Issue401 extends ServerFeature {

  {
    use(ConfigFactory.empty().withValue("db", ConfigValueFactory.fromAnyRef("mem")));

    use(new Jdbc());
    use(new Jdbi());

    onStart(registry -> {
      try (Handle h = registry.require(Handle.class)) {
        h.execute("create table members (id int primary key, name varchar(100))");

        h.execute("insert into members (id, name) values (?, ?)", 1, "a");
        h.execute("insert into members (id, name) values (?, ?)", 2, "b");
        h.execute("insert into members (id, name) values (?, ?)", 3, "c");
        h.execute("insert into members (id, name) values (?, ?)", 4, "d");
        h.execute("insert into members (id, name) values (?, ?)", 5, "e");
        h.execute("insert into members (id, name) values (?, ?)", 6, "f");
        h.execute("insert into members (id, name) values (?, ?)", 7, "g");
        h.execute("insert into members (id, name) values (?, ?)", 8, "h");
        h.execute("insert into members (id, name) values (?, ?)", 9, "i");
      }
    });

    get("/401/pos", req -> {
      try (Handle h = req.require(Handle.class)) {
        return h
            .createQuery("select name from members where id in (?) order by name limit ? offset ?")
            .bind(0, req.param("id").toList(Integer.class))
            .bind(1, 5)
            .bind(2, 0)
            .map(StringColumnMapper.INSTANCE)
            .list();
      }
    });

    get("/401/named", req -> {
      try (Handle h = req.require(Handle.class)) {
        return h.createQuery(
            "select name from members where id in (:id) order by name limit :limit offset :offset")
            .bind("id", req.param("id").toList(Integer.class))
            .bind("offset", 0)
            .bind("limit", 5)
            .map(StringColumnMapper.INSTANCE)
            .list();
      }
    });

  }

  @Test
  public void posParamShouldWork() throws Exception {
    request()
        .get("/401/pos?id=1&id=2&id=3&id=4&id=5&id=6&id=7")
        .expect("[a, b, c, d, e]");
  }

  @Test
  public void namedParamShouldWork() throws Exception {
    request()
        .get("/401/named?id=1&id=2&id=3&id=4&id=5&id=6&id=7")
        .expect("[a, b, c, d, e]");
  }

}
