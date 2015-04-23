package org.jooby.jdbi;

import java.util.List;

import org.jooby.test.ServerFeature;
import org.junit.Test;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.logging.FormattedLog;
import org.skife.jdbi.v2.util.StringMapper;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class JdbiInClauseFeature extends ServerFeature {

  {

    use(ConfigFactory.empty()
        .withValue("db.url", ConfigValueFactory.fromAnyRef("jdbc:mysql://localhost/test"))
        .withValue("db.user", ConfigValueFactory.fromAnyRef("root")));

    use(new Jdbi().doWith((dbi, conf) -> {
      dbi.setSQLLog(new FormattedLog() {

        @Override
        protected void log(final String msg) {
          System.out.println(msg);
        }

        @Override
        protected boolean isEnabled() {
          return true;
        }
      });
    }));

    get("/named", req -> {
      try (Handle h = req.require(Handle.class)) {
        h.execute("drop table members");

        h.execute("create table members (id int primary key, name varchar(100))");

        h.execute("insert into members (id, name) values (?, ?)", 1, "Jooby");
        h.execute("insert into members (id, name) values (?, ?)", 2, "Rock");

        List<String> name = h.createQuery("select name from members where id in (:id)")
            .bind("id", req.param("id").toList(Integer.class))
            .map(StringMapper.FIRST)
            .list();

        return name;
      }
    });

    get("/pos", req -> {
      try (Handle h = req.require(Handle.class)) {
        h.execute("drop table members");

        h.execute("create table members (id int primary key, name varchar(100))");

        h.execute("insert into members (id, name) values (?, ?)", 1, "Jooby");
        h.execute("insert into members (id, name) values (?, ?)", 2, "Rock");

        List<String> name = h.createQuery("select name from members where id in (?)")
            .bind(0, req.param("id").toList(Integer.class).toArray(new Integer[0]))
            .map(StringMapper.FIRST)
            .list();

        return name;
      }
    });
  }

  @Test
  public void named() throws Exception {
    request()
        .get("/named?id=1&id=2")
        .expect("[Jooby, Rock]");
  }

  @Test
  public void pos() throws Exception {
    request()
        .get("/pos?id=1&id=2")
        .expect("[Jooby, Rock]");
  }
}
