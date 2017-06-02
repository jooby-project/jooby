package org.jooby.jdbi;

import java.io.Closeable;

import org.jooby.test.ServerFeature;
import org.junit.Test;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class JdbiSqlObjectFeature extends ServerFeature {

  public interface MyRepository extends Closeable {
    @SqlUpdate("create table something (id int primary key, name varchar(100))")
    void createSomethingTable();

    @SqlUpdate("insert into something (id, name) values (:id, :name)")
    void insert(@Bind("id") int id, @Bind("name") String name);

    @SqlQuery("select name from something where id = :id")
    String findNameById(@Bind("id") int id);
  }

  {

    use(ConfigFactory.empty().withValue("db", ConfigValueFactory.fromAnyRef("mem")));

    use(new Jdbi(MyRepository.class));

    get("/jdbi-handle", req -> {
      try (MyRepository h = req.require(MyRepository.class)) {
        h.createSomethingTable();

        h.insert(1, "Jooby");

        String name = h.findNameById(1);

        return name;
      }
    });
  }

  @Test
  public void doWithSqlObject() throws Exception {
    request()
        .get("/jdbi-handle")
        .expect("Jooby");
  }
}
