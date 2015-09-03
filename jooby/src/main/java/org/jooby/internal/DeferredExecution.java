package org.jooby.internal;

import org.jooby.Deferred;

/**
 * Use to detach the request from current thread (async mode). Internal use only.
 *
 * @author edgar
 * @since 0.10.0
 */
@SuppressWarnings("serial")
public class DeferredExecution extends RuntimeException {

  public final Deferred deferred;

  public DeferredExecution(final Deferred deferred) {
    this.deferred = deferred;
  }

}
