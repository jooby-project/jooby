package org.jooby.internal;

import java.lang.reflect.Method;

import org.jooby.Managed;

public class PreDestroyManaged implements Managed {

  private Object source;

  private Method preDestroy;

  public PreDestroyManaged(final Object source, final Method preDestroy) {
    this.source = source;
    this.preDestroy = preDestroy;
  }

  @Override
  public void start() throws Exception {
  }

  @Override
  public void stop() throws Exception {
    preDestroy.invoke(source);
  }

}
