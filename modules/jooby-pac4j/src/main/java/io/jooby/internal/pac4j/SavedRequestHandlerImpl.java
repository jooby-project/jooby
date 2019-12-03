package io.jooby.internal.pac4j;

import io.jooby.Context;
import io.jooby.pac4j.Pac4jContext;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.engine.savedrequest.DefaultSavedRequestHandler;

import java.util.Set;

public class SavedRequestHandlerImpl extends DefaultSavedRequestHandler {
  private Set<String> excludes;

  public SavedRequestHandlerImpl(Set<String> excludes) {
    this.excludes = excludes;
  }

  @Override public void save(WebContext webContext) {
    Pac4jContext pac4j = (Pac4jContext) webContext;
    Context context = pac4j.getContext();
    if (!excludes.contains(context.pathString())) {
      super.save(webContext);
    }
  }
}
