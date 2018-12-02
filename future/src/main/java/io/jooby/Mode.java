package io.jooby;

public enum Mode {
  /**
   * Always execute handler in the event loop thread (non-blocking).
   */
  EVENT_LOOP,

  /**
   * Always execute handler in a worker thread (blocking). This is the default execution mode.
   */
  WORKER
}
