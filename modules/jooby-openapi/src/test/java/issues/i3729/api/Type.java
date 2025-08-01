/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3729.api;

/**
 * Books can be broadly categorized into fiction and non-fiction. With many genres and subgenres
 * within each.
 */
public enum Type {
  /**
   * Fiction books are based on imaginary characters and events, while non-fiction books are based o
   * n real people and events.
   */
  Fiction,

  /** Non-fiction genres include biography, autobiography, history, self-help, and true crime. */
  NonFiction;
}
