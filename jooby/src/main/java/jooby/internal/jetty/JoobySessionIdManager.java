package jooby.internal.jetty;

import static java.util.Objects.requireNonNull;
import jooby.Cookie;
import jooby.Session.Store;

import org.eclipse.jetty.server.session.HashSessionIdManager;

public class JoobySessionIdManager extends HashSessionIdManager {

  private Store generator;

  private String secret;

  public JoobySessionIdManager(final Store generator, final String secret) {
    this.generator = requireNonNull(generator, "A ID generator is required.");
    this.secret = requireNonNull(secret, "An application.secret is required.");
  }

  @Override
  public String newSessionId(final long seedTerm) {
    try {
      String id = generator.generateID(seedTerm);
      return Cookie.Signature.sign(id, secret);
    } catch (RuntimeException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new IllegalStateException("Can't sign session ID", ex);
    }
  }

}
