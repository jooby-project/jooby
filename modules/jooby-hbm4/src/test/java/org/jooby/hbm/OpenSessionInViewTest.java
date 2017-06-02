package org.jooby.hbm;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.util.List;

import javax.persistence.EntityManager;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.context.internal.ManagedSessionContext;
import org.hibernate.jpa.HibernateEntityManagerFactory;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.internal.hbm.TrxResponse;
import org.jooby.internal.hbm.TrxResponseTest;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.Lists;
import com.google.inject.Key;

@RunWith(PowerMockRunner.class)
@PrepareForTest({OpenSessionInView.class, ManagedSessionContext.class, TrxResponseTest.class })
public class OpenSessionInViewTest {

  @SuppressWarnings("unchecked")
  @Test
  public void defaults() throws Exception {
    List<Key<EntityManager>> keys = Lists.newArrayList(Key.get(EntityManager.class));
    new MockUnit(HibernateEntityManagerFactory.class, Request.class, Response.class,
        Route.Chain.class,
        EntityManager.class)
            .expect(unit -> {
              Request req = unit.get(Request.class);
              expect(req.set(keys.get(0), unit.get(EntityManager.class))).andReturn(req);

              TrxResponse rsp = unit.mockConstructor(TrxResponse.class, new Class[]{Response.class,
                  EntityManager.class }, unit.get(Response.class), unit.get(EntityManager.class));
              expect(rsp.begin()).andReturn(rsp);
              rsp.done();

              Route.Chain chain = unit.get(Route.Chain.class);
              chain.next(req, rsp);
            })
            .expect(unit -> {
              SessionFactory sf = unit.mock(SessionFactory.class);

              Session session = unit.mock(Session.class);
              session.setFlushMode(FlushMode.AUTO);

              unit.mockStatic(ManagedSessionContext.class);
              expect(ManagedSessionContext.bind(session)).andReturn(session);
              expect(ManagedSessionContext.unbind(sf)).andReturn(session);

              EntityManager em = unit.get(EntityManager.class);
              expect(em.getDelegate()).andReturn(session);

              HibernateEntityManagerFactory hemf = unit.get(HibernateEntityManagerFactory.class);
              expect(hemf.getSessionFactory()).andReturn(sf);
              expect(hemf.createEntityManager()).andReturn(em);

            })
            .run(unit -> {
              new OpenSessionInView(unit.get(HibernateEntityManagerFactory.class), keys)
                  .handle(
                      unit.get(Request.class),
                      unit.get(Response.class),
                      unit.get(Route.Chain.class));
            });
  }

  @SuppressWarnings("unchecked")
  @Test(expected = HibernateException.class)
  public void shouldAlwaysUnbindSession2() throws Exception {
    List<Key<EntityManager>> keys = Lists.newArrayList(Key.get(EntityManager.class));
    new MockUnit(HibernateEntityManagerFactory.class, Request.class, Response.class,
        Route.Chain.class,
        EntityManager.class)
            .expect(unit -> {
              Request req = unit.get(Request.class);
              expect(req.set(keys.get(0), unit.get(EntityManager.class))).andReturn(req);

              TrxResponse rsp = unit.mockConstructor(TrxResponse.class, new Class[]{Response.class,
                  EntityManager.class }, unit.get(Response.class), unit.get(EntityManager.class));

              expect(rsp.begin()).andReturn(rsp);
              rsp.done();

              expectLastCall().andThrow(new HibernateException("intentional err"));

              Route.Chain chain = unit.get(Route.Chain.class);
              chain.next(req, rsp);
            })
            .expect(unit -> {
              SessionFactory sf = unit.mock(SessionFactory.class);

              Session session = unit.mock(Session.class);
              session.setFlushMode(FlushMode.AUTO);

              unit.mockStatic(ManagedSessionContext.class);
              expect(ManagedSessionContext.bind(session)).andReturn(session);
              expect(ManagedSessionContext.unbind(sf)).andReturn(session);

              EntityManager em = unit.get(EntityManager.class);
              expect(em.getDelegate()).andReturn(session);

              HibernateEntityManagerFactory hemf = unit.get(HibernateEntityManagerFactory.class);
              expect(hemf.getSessionFactory()).andReturn(sf);
              expect(hemf.createEntityManager()).andReturn(em);

            })
            .run(unit -> {
              new OpenSessionInView(unit.get(HibernateEntityManagerFactory.class), keys)
                  .handle(
                      unit.get(Request.class),
                      unit.get(Response.class),
                      unit.get(Route.Chain.class));
            });
  }

  @SuppressWarnings("unchecked")
  @Test(expected = HibernateException.class)
  public void shouldAlwaysUnbindSession3() throws Exception {
    List<Key<EntityManager>> keys = Lists.newArrayList(Key.get(EntityManager.class));
    new MockUnit(HibernateEntityManagerFactory.class, Request.class, Response.class,
        Route.Chain.class,
        EntityManager.class)
            .expect(unit -> {
              Request req = unit.get(Request.class);
              expect(req.set(keys.get(0), unit.get(EntityManager.class))).andReturn(req);

              TrxResponse rsp = unit.mockConstructor(TrxResponse.class, new Class[]{Response.class,
                  EntityManager.class }, unit.get(Response.class), unit.get(EntityManager.class));

              expect(rsp.begin()).andReturn(rsp);
              rsp.done();

              Route.Chain chain = unit.get(Route.Chain.class);
              chain.next(req, rsp);
            })
            .expect(unit -> {
              SessionFactory sf = unit.mock(SessionFactory.class);

              Session session = unit.mock(Session.class);
              session.setFlushMode(FlushMode.AUTO);

              unit.mockStatic(ManagedSessionContext.class);
              expect(ManagedSessionContext.bind(session)).andReturn(session);
              expect(ManagedSessionContext.unbind(sf))
                  .andThrow(new HibernateException("intentional err"));

              EntityManager em = unit.get(EntityManager.class);
              expect(em.getDelegate()).andReturn(session);

              HibernateEntityManagerFactory hemf = unit.get(HibernateEntityManagerFactory.class);
              expect(hemf.getSessionFactory()).andReturn(sf);
              expect(hemf.createEntityManager()).andReturn(em);
            })
            .run(unit -> {
              new OpenSessionInView(unit.get(HibernateEntityManagerFactory.class), keys)
                  .handle(
                      unit.get(Request.class),
                      unit.get(Response.class),
                      unit.get(Route.Chain.class));
            });
  }

}
