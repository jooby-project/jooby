package org.jooby.hbm;

import java.net.URISyntaxException;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.jooby.Results;
import org.jooby.hbm.data.Member;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class HbmJpaListenerFeature extends ServerFeature {

  {
    use(ConfigFactory.empty()
        .withValue("db", ConfigValueFactory.fromAnyRef("mem")));

    use(new Hbm()
        .classes(Member.class));

    use("*", Hbm.openSessionInView());

    get("/members", req -> {
      EntityManager em = req.require(EntityManager.class);
      TypedQuery<Member> query = em.createQuery("from Member", Member.class);
      return query.getResultList().stream().map(m -> m.getAlias()).collect(Collectors.toList());
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
        .post("/members")
        .form()
        .add("id", 1)
        .add("name", "pedro")
        .expect("pedro(1)");

    request()
        .get("/members")
        .expect("[post-load]");
  }
}
