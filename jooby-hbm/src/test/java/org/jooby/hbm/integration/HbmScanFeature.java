package org.jooby.hbm.integration;

import static org.junit.Assert.assertEquals;

import java.net.URISyntaxException;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.http.client.fluent.Request;
import org.apache.http.message.BasicNameValuePair;
import org.jooby.Body;
import org.jooby.hbm.Hbm;
import org.jooby.hbm.integration.data.Member;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class HbmScanFeature extends ServerFeature {

  {
    use(ConfigFactory.empty().withValue("db", ConfigValueFactory.fromAnyRef("mem")));

    use(new Hbm().scan());

    get("/members", req -> {
      EntityManager em = req.require(EntityManager.class);
      Query query = em.createQuery("from Member");
      return query.getResultList();
    });

    post("/members", (req, rsp, chain) -> {
      Member member = req.params(Member.class);
      EntityManager em = req.require(EntityManager.class);
      em.persist(member);
      if (req.param("err").toOptional(Boolean.class).orElse(false)) {
        throw new IllegalArgumentException("Rollback on err");
      }
      // we do this way just to make sure the correct delegate got executed
        rsp.send(Body.body(member));
        chain.next(req, rsp);
      });
  }

  @Test
  public void hbm() throws URISyntaxException, Exception {
    assertEquals("[]", Request.Get(uri("members").build()).execute().returnContent().asString());
    // create member
    assertEquals(
        "pedro(1)",
        Request.Post(uri("members").build())
            .bodyForm(new BasicNameValuePair("id", "1"), new BasicNameValuePair("name", "pedro"))
            .execute().returnContent().asString());

    assertEquals("[pedro(1)]", Request.Get(uri("members").build()).execute().returnContent()
        .asString());

    // create member
    assertEquals(
        "pablo(2)",
        Request.Post(uri("members").build())
            .bodyForm(new BasicNameValuePair("id", "2"), new BasicNameValuePair("name", "pablo"))
            .execute().returnContent().asString());

    assertEquals("[pedro(1), pablo(2)]", Request.Get(uri("members").build()).execute()
        .returnContent()
        .asString());

    // at err with rollback
    assertEquals(400, Request.Post(uri("members").addParameter("err", "true").build())
        .bodyForm(new BasicNameValuePair("id", "3"), new BasicNameValuePair("name", "vilma"))
        .execute().returnResponse().getStatusLine().getStatusCode());

    assertEquals("[pedro(1), pablo(2)]", Request.Get(uri("members").build()).execute()
        .returnContent()
        .asString());

    // err with duplicated id
    assertEquals(500, Request.Post(uri("members").build())
        .bodyForm(new BasicNameValuePair("id", "2"), new BasicNameValuePair("name", "vilma"))
        .execute().returnResponse().getStatusLine().getStatusCode());

    // test 2nd readonly trx. 2nd trx is generated while sending response. so object must be
    // persisted
    assertEquals(500, Request.Post(uri("members").build())
        .bodyForm(new BasicNameValuePair("id", "3"),
            new BasicNameValuePair("name", "vilma"),
            new BasicNameValuePair("viewErr", "true"))
        .execute().returnResponse().getStatusLine().getStatusCode());

    assertEquals("[pedro(1), pablo(2), vilma(3)]", Request.Get(uri("members").build()).execute()
        .returnContent()
        .asString());
  }
}
