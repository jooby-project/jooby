package jooby.internal.jetty;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

import jooby.Session;
import jooby.Session.Store;
import jooby.Session.Store.SaveReason;

import org.eclipse.jetty.server.session.AbstractSession;
import org.eclipse.jetty.server.session.AbstractSessionManager;

import com.google.common.collect.ImmutableList;

public class JoobySessionManager extends AbstractSessionManager {

  private final Map<String, JoobySession> sessions = new ConcurrentHashMap<>();

  private Store store;

  private boolean preserveOnStop;

  private int saveInterval;

  private String secret;

  public JoobySessionManager(final Session.Store store, final String secret) {
    this.store = requireNonNull(store, "A session store is required.");
    this.secret = requireNonNull(secret, "An application secret is required.");
    setSessionIdManager(new JoobySessionIdManager(store, secret));
  }

  @Override
  protected void addSession(final AbstractSession session) {
    if (isRunning()) {
      sessions.put(session.getClusterId(), (JoobySession) session);
      // TODO: session.willPassivate();
      // TODO: store.save((JoobySession) session);
      // TODO: session.didActivate();
    }
  }

  @Override
  public AbstractSession getSession(final String idInCluster) {
    JoobySession session = sessions.get(idInCluster);

    if (session == null) {
      try {
        session = (JoobySession) store.get(idInCluster);
      } catch (Exception ex) {
        Session.log.error("Can't get session from storage: " + idInCluster, ex);
      }
      if (session != null) {
        JoobySession race = sessions.putIfAbsent(idInCluster, session);
        if (race != null) {
          // TODO: session.willPassivate();
          session.clearAttributes();
          // TODO: session.didActivate();
          session = race;
        }
        // expiry?
        if (!session.access(System.currentTimeMillis())) {
          session = null;
        }
      }
    }
    return session;
  }

  @Override
  protected void shutdownSessions() throws Exception {
    for (JoobySession session : ImmutableList.copyOf(this.sessions.values())) {
      if (preserveOnStop) {
        store.save(session, SaveReason.PRESERVE_ON_STOP);
        this.sessions.remove(session.getClusterId());
      } else {
        session.invalidate();
      }
    }

  }

  @Override
  protected AbstractSession newSession(final HttpServletRequest request) {
    JoobySession session = new JoobySession(this, request);
    session.setMaxInactiveInterval(getMaxInactiveInterval());
    session.setSaveInterval(saveInterval);
    session.setSecret(secret);
    return session;
  }

  @Override
  protected boolean removeSession(final String clusterId) {
    synchronized (this) {
      JoobySession session = sessions.remove(clusterId);
      try {
        store.delete(clusterId);
      } catch (Exception ex) {
        Session.log.error("Can't delete session: " + clusterId, ex);
      }
      return session != null;
    }
  }

  @Override
  public void renewSessionId(final String oldClusterId, final String oldNodeId,
      final String newClusterId, final String newNodeId) {
    synchronized (this) {
      JoobySession session = sessions.remove(oldClusterId);
      try {
        store.delete(oldClusterId);
      } catch (Exception ex) {
        Session.log.error("Can't delete session: " + oldClusterId, ex);
      }

      session.setClusterId(newClusterId);
      session.setNodeId(newNodeId);

      sessions.put(newClusterId, session);

      try {
        store.save(session, SaveReason.RENEW_ID);
      } catch (Exception ex) {
        Session.log.error("Can't save session: " + session.getId(), ex);
      }
    }

    super.renewSessionId(oldClusterId, oldNodeId, newClusterId, newNodeId);
  }

  @Override
  public void doStop() throws Exception {
    // TODO Auto-generated method stub
    super.doStop();
  }

  @Override
  public JoobySessionIdManager getSessionIdManager() {
    return (JoobySessionIdManager) super.getSessionIdManager();
  }

  public Session.Store getSessionStore() {
    return store;
  }

  public boolean isPreserveOnStop() {
    return preserveOnStop;
  }

  public void setPreserveOnStop(final boolean preserveOnStop) {
    this.preserveOnStop = preserveOnStop;
  }

  public void setSaveInterval(final int saveInterval) {
    this.saveInterval = saveInterval;
  }

}
