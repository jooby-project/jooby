/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import examples.jpa.Person;
import io.jooby.Jooby;
import io.jooby.hikari.HikariModule;
import io.jooby.jdbi.JdbiModule;
import io.jooby.jdbi.TransactionalRequest;
import io.jooby.json.JacksonModule;
import org.jdbi.v3.core.Jdbi;

import java.util.List;

public class JdbiApp extends Jooby {
  {
    install(new HikariModule("mem"));
    install(new JdbiModule().sqlObjects(PersonRepo.class));
    install(new JacksonModule());

    require(Jdbi.class).useHandle(h -> {
      h.useTransaction(hh -> {
        hh.createScript("create table person (id int);").execute();
      });
    });

    decorator(new TransactionalRequest());

    get("/create", ctx -> {
      PersonRepo h = require(PersonRepo.class);
      Person p = new Person();
      p.setId(1L);
      h.insert(p);
      return p;
    });

    get("/people", ctx -> {
      PersonRepo h = require(PersonRepo.class);
      List<Person> people = h.list();
      return people;
    });
  }

  public static void main(String[] args) {
    runApp(args, JdbiApp::new);
  }
}
