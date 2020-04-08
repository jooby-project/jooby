package io.jooby.internal.pac4j;

import io.jooby.Registry;
import org.pac4j.core.authorization.authorizer.Authorizer;
import org.pac4j.core.context.WebContext;

import java.util.List;

public class ForwardingAuthorizer implements Authorizer {
  private Registry registry;

  private Class authorizerType;

  public ForwardingAuthorizer(Class authorizerType) {
    this.authorizerType = authorizerType;
  }

  @Override public boolean isAuthorized(WebContext context, List profiles) {
    return authorizer(authorizerType).isAuthorized(context, profiles);
  }

  private Authorizer authorizer(Class authorizerType) {
    return (Authorizer) registry.require(authorizerType);
  }

  public void setRegistry(Registry registry) {
    this.registry = registry;
  }
}
