/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.annotation.htmx;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Triggers a client-side event upon successful response. Maps to the {@code HX-Trigger}, {@code
 * HX-Trigger-After-Settle}, or {@code HX-Trigger-After-Swap} headers.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Repeatable(HxTriggers.class)
public @interface HxTrigger {

  /**
   * The name of the client-side event to trigger.
   *
   * @return The event name.
   */
  String value();

  /**
   * The lifecycle phase at which the event should be triggered.
   *
   * @return The trigger phase. Defaults to {@link Phase#TRIGGER}.
   */
  Phase phase() default Phase.TRIGGER;

  /** Represents the HTMX trigger lifecycle headers. */
  enum Phase {
    /** Appends to the {@code HX-Trigger} header. */
    TRIGGER,

    /** Appends to the {@code HX-Trigger-After-Settle} header. */
    AFTER_SETTLE,

    /** Appends to the {@code HX-Trigger-After-Swap} header. */
    AFTER_SWAP
  }
}
