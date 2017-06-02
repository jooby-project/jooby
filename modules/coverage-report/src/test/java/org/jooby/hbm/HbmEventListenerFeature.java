package org.jooby.hbm;

import java.net.URISyntaxException;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PostLoadEventListener;
import org.jooby.Results;
import org.jooby.hbm.data.Member;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class HbmEventListenerFeature extends ServerFeature {

  public static class Service {
    public String alias() {
      return "post-load";
    }
  }

  @SuppressWarnings("serial")
  public static class MemberPostLoad implements PostLoadEventListener {

    private Service service;

    @Inject
    public MemberPostLoad(final Service service) {
      this.service = service;
    }

    @Override
    public void onPostLoad(final PostLoadEvent event) {
      Member member = (Member) event.getEntity();
      member.setAlias(service.alias());
    }

  }

  {
    use(ConfigFactory.empty()
        .withValue("db", ConfigValueFactory.fromAnyRef("mem")));

    use(new Hbm()
        .onEvent(EventType.POST_LOAD, MemberPostLoad.class)
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
