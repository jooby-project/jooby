package org.jooby.issues;

import java.util.List;

import org.jooby.jdbc.Jdbc;
import org.jooby.jdbi.Jdbi;
import org.jooby.test.ServerFeature;
import org.junit.Test;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.util.StringColumnMapper;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class Issue196 extends ServerFeature {

  {

    use(ConfigFactory.empty()
        .withValue("db", ConfigValueFactory.fromAnyRef("mem")));

    use(new Jdbc());
    use(new Jdbi());

    get("/npe", req -> {
      try (Handle h = req.require(Handle.class)) {
        h.execute("create table members (id int primary key, name varchar(100) default null)");

        h.execute("insert into members (id) values (?)", 1);

        List<String> ids = h.createQuery("select id from members where name is null")
            .bind("name", (String) null)
            .map(StringColumnMapper.INSTANCE)
            .list();

        return ids;
      }
    });

  }

  @Test
  public void iterableFactoryShouldAllowNullValues() throws Exception {
    request()
        .get("/npe")
        .expect("[1]");
  }

}
