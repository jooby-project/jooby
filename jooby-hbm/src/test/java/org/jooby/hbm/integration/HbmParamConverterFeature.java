package org.jooby.hbm.integration;

import static org.junit.Assert.assertEquals;

import java.net.URISyntaxException;

import javax.persistence.EntityManager;

import org.apache.http.client.fluent.Request;
import org.apache.http.message.BasicNameValuePair;
import org.jooby.hbm.Hbm;
import org.jooby.hbm.integration.data.Member;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class HbmParamConverterFeature extends ServerFeature {

  {
    use(ConfigFactory.empty().withValue("db", ConfigValueFactory.fromAnyRef("mem")));

    use(new Hbm(Member.class));

    param((type, vals, ctx) -> {
      if (type.getRawType() == Member.class) {
        EntityManager em = ctx.require(EntityManager.class);
        Member member = em.find(Member.class, Integer.parseInt(vals[0].toString()));
        return member;
      }
      return ctx.convert(type, vals);
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
    assertEquals("pedro(1)", Request.Post(uri("members").build())
        .bodyForm(new BasicNameValuePair("id", "1"), new BasicNameValuePair("name", "pedro"))
        .execute()
        .returnContent()
        .asString());

    assertEquals("pedro(1)", Request.Get(uri("members", "1").build())
        .execute()
        .returnContent()
        .asString());

    assertEquals("pedro(1)", Request.Get(uri("members").addParameter("member", "1").build())
        .execute()
        .returnContent()
        .asString());
  }
}
