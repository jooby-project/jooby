package org.jooby.hbm;

import java.net.URISyntaxException;

import javax.persistence.EntityManager;

import org.jooby.hbm.data.Member;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class HbmParamConverterFeature extends ServerFeature {

  {
    use(ConfigFactory.empty()
        .withValue("db", ConfigValueFactory.fromAnyRef("mem"))
        .withValue("hibernate.hbm2ddl.auto", ConfigValueFactory.fromAnyRef("update")));

    use(new Hbm(Member.class));

    param((type, values, ctx) -> {
      if (type.getRawType() == Member.class && values.length > 0) {
        EntityManager em = ctx.require(EntityManager.class);
        Member member = em.find(Member.class, Integer.parseInt(values[0].toString()));
        return member;
      }
      return ctx.convert(type, values);
    });

    get("/members/:member", req -> req.param("member").to(Member.class));

    get("/members", req -> req.param("member").to(Member.class));

    post("/members", req -> {
      Member member = req.params(Member.class);
      EntityManager em = req.require(EntityManager.class);
      em.persist(member);
      return member;
    });
  }

  @Test
  public void hbm() throws URISyntaxException, Exception {
    // create member
    request()
        .post("/members")
        .form()
        .add("id", 1)
        .add("name", "pedro")
        .expect("pedro(1)");

    request()
        .get("/members/1")
        .expect("pedro(1)");

    request()
        .get("/members?member=1")
        .expect("pedro(1)");

  }
}
