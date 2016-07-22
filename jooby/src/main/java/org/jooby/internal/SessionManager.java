package org.jooby.internal;

import org.jooby.Cookie;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Session;

public interface SessionManager {

  Session create(Request req, Response rsp);

  Session get(Request req, Response rsp);

  void destroy(Session session);

  void requestDone(Session session);

  Cookie.Definition cookie();

}
