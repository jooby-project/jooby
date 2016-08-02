package org.jooby.issues;

import java.net.URISyntaxException;

import org.jooby.hbm.Hbm;
import org.jooby.hbm.UnitOfWork;
import org.jooby.hbm.data.Member;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class Issue439 extends ServerFeature {

  {
    use(ConfigFactory.empty()
        .withValue("db", ConfigValueFactory.fromAnyRef("mem")));

    use(new Hbm().classes(Member.class));

    use("*", Hbm.openSessionInView());

    get("/members", req -> {
      return req.require(UnitOfWork.class).apply(em -> {
        return em.createQuery("from Member").getResultList();
      });

    });

    post("/members", (req, rsp, chain) -> {
      req.require(UnitOfWork.class).accept(em -> {
        Member member = req.params().to(Member.class);
        em.persist(member);
        rsp.send(member);
        chain.next(req, rsp);
      });
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
  }
}
