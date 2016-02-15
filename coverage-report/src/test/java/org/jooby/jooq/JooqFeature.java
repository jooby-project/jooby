package org.jooby.jooq;

import static org.junit.Assert.assertEquals;

import org.jooby.test.ServerFeature;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class JooqFeature extends ServerFeature {

  private static final Table<Record> SOMETHING = DSL.table(DSL.name("something"));

  private static final Field<Object> ID = DSL.field(DSL.name("id"));

  private static final Field<Object> NAME = DSL.field(DSL.name("name"));

  {
    use(ConfigFactory.empty()
        .withValue("db", ConfigValueFactory.fromAnyRef("mem")));

    use(new jOOQ());

    get("/jooq", req -> {
      assertEquals(SQLDialect.H2, req.require(Configuration.class).dialect());
      try (DSLContext ctx = req.require(DSLContext.class)) {
        return ctx.transactionResult(conf -> {
          DSLContext trx = DSL.using(conf);
          trx.createTable("something")
              .column("id", SQLDataType.INTEGER)
              .column("name", SQLDataType.VARCHAR.length(100))
              .execute();
          trx.createIndex("something_pk").on("something", "id")
              .execute();
          ctx.insertInto(SOMETHING, ID, NAME)
              .values(1, "Jooby")
              .execute();

          Object value = ctx.selectFrom(SOMETHING)
              .where(ID.eq(1))
              .fetchOne(NAME);

          return value;
        });
      }
    });

    get("/select", req -> {
      try (DSLContext ctx = req.require(DSLContext.class)) {
        return ctx.transactionResult(conf -> {
          DSLContext trx = DSL.using(conf);
          Object value = trx.selectFrom(SOMETHING)
              .where(ID.eq(1))
              .fetchOne(NAME);

          return value;
        });
      }
    });
  }

  @Test
  public void jooq() throws Exception {
    request()
        .get("/jooq")
        .expect("Jooby");

    request()
        .get("/select")
        .expect("Jooby");
  }

}
