package org.jooby.hbm;

import java.net.URISyntaxException;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.jooby.Results;
import org.jooby.hbm.data.Member;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class HbmScanFeature extends ServerFeature {

  {
    use(ConfigFactory.empty()
        .withValue("db", ConfigValueFactory.fromAnyRef("mem")));

    use(new Hbm().scan());

    get("/members", req -> {
      EntityManager em = req.require(EntityManager.class);
      Query query = em.createQuery("from Member");
      return query.getResultList();
    });

    post("/members", (req, rsp, chain) -> {
      Member member = req.params().to(Member.class);
      EntityManager em = req.require(EntityManager.class);
      em.persist(member);
      if (req.param("err").toOptional(Boolean.class).orElse(false)) {
        throw new IllegalArgumentException("Rollback on err");
      }
      // we do this way just to make sure the correct delegate got executed
        rsp.send(Results.with(member));
        chain.next(req, rsp);
      });
  }

  @Test
  public void hbm() throws URISyntaxException, Exception {
    request()
        .get("/members")
        .expect("[]");

    request()
        .post("/members")
        .form()
        .add("id", 1)
        .add("name", "pedro")
        .expect("pedro(1)");

    request()
        .get("/members")
        .expect("[pedro(1)]");

    request()
        .post("/members")
        .form()
        .add("id", 2)
        .add("name", "pablo")
        .expect("pablo(2)");

    request()
        .get("/members")
        .expect("[pedro(1), pablo(2)]");

    // at err with rollback
    request()
        .post("/members?err=true")
        .form()
        .add("id", 3)
        .add("name", "vilma")
        .expect(400);

    request()
        .get("/members")
        .expect("[pedro(1), pablo(2)]");

    // err with duplicated id
    request()
        .post("/members")
        .form()
        .add("id", 2)
        .add("name", "vilma")
        .expect(500);

    // test 2nd readonly trx. 2nd trx is generated while sending response. so object must be
    // persisted
    request()
        .post("/members")
        .form()
        .add("id", 3)
        .add("name", "vilma")
        .add("viewErr", true)
        .expect(500);

    request()
        .get("/members")
        .expect("[pedro(1), pablo(2), vilma(3)]");
  }
}
