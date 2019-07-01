/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import examples.jpa.Person;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

public interface PersonRepo {
  @SqlUpdate("insert into person (id) values (:id)")
  void insert(@BindBean Person p);

  @SqlQuery("select * from person")
  @RegisterBeanMapper(Person.class)
  List<Person> list();
}
