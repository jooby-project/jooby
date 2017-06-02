package app;

import org.crsh.auth.AuthenticationPlugin;
import org.crsh.plugin.CRaSHPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthPlugin extends CRaSHPlugin<AuthPlugin>
    implements AuthenticationPlugin<String> {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  @Override
  public AuthPlugin getImplementation() {
    return this;
  }

  @Override
  public String getName() {
    return "auth";
  }

  @Override
  public Class<String> getCredentialType() {
    return String.class;
  }

  @Override
  public boolean authenticate(final String username, final String credential) throws Exception {
    log.info("{} pass {}", username, credential);
    return true;
  }

}
