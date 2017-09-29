package org.jooby.jdbi;

import org.jdbi.v3.core.Jdbi;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class JdbiInClauseTest {

  @Test
  public void inClause() {
    Jdbi jdbi = Jdbi.create("jdbc:h2:mem:test");
    List<String> names = jdbi.withHandle(h -> {
      h.createUpdate("CREATE TABLE USER (id INTEGER PRIMARY KEY, name VARCHAR)")
          .execute();
      h.createUpdate("INSERT INTO USER(id, name) VALUES (:id, :name)")
          .bind("id", 1)
          .bind("name", "Pedro Picapiedra")
          .execute();
      h.createUpdate("INSERT INTO USER(id, name) VALUES (:id, :name)")
          .bind("id", 2)
          .bind("name", "Pablo Marmol")
          .execute();

      return h.createQuery("select name from USER where id in (<id>)")
          .bindList("id", 1, 2)
          .mapTo(String.class)
          .list();
    });

    assertEquals(2, names.size());
  }
}
