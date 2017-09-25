package org.jooby.issues;

import static org.junit.Assert.assertEquals;

import org.jooby.jdbc.Jdbc;
import org.jooby.rx.Rx;
import org.jooby.rx.RxJdbc;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.github.davidmoten.rx.jdbc.Database;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class Issue351 extends ServerFeature {

  {
    use(ConfigFactory.empty()
        .withValue("db", ConfigValueFactory.fromAnyRef("mem")));

    use(new Jdbc());
    use(new RxJdbc());

    onStart(r -> {
      Database db = r.require(Database.class);
      db.update("create table something (id int primary key, name varchar(100))")
          .execute();

      db.update("insert into something (id, name) values (?, ?)")
          .parameters(1, "jooby")
          .execute();
    });

    get("/351/reactive", req ->
      req.require(Database.class)
        .select("select name from something where id = :id")
        .parameter("id", 1)
        .getAs(String.class)
    ).map(Rx.rx());

    get("/351/blocking", req -> {
      return req.require(Database.class)
          .select("select name from something where id = :id")
          .parameter("id", 1)
          .getAs(String.class)
          .toBlocking()
          .single();
    });

    get("/db", req -> {
      assertEquals(req.require(Database.class), req.require(Database.class));
      return "OK";
    });

  }

  @Test
  public void rxjdbc() throws Exception {
    request().get("/351/reactive")
        .expect("jooby");
  }

  @Test
  public void rxjdbcBlocking() throws Exception {
    request().get("/351/blocking")
        .expect("jooby");
  }

  @Test
  public void singletondb() throws Exception {
    request().get("/db")
        .expect("OK");
  }

}
