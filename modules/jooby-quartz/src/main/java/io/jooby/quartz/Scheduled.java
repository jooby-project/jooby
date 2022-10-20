/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.quartz;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.quartz.Trigger;

/**
 * Define a {@link Trigger Quartz Trigger}. Using one of three expressions:
 *
 * <ul>
 *   <li>interval: like<code>5s;delay=60s;repeat=*</code>, <code>delay</code> and <code>repeat
 *       </code> are optional
 *   <li>cron: like <code>0/3 * * * * ?</code>
 *   <li>property name: a property defined in <code>.conf</code> file which has one of two previous
 *       formats
 * </ul>
 *
 * Examples:
 *
 * <p>Run every 5 minutes, start immediately and repeat for ever:
 *
 * <pre>
 * &#64;Scheduled("5m")
 *
 * &#64;Scheduled("5m; delay=0")
 *
 * &#64;Scheduled("5m; delay=0; repeat=*")
 * </pre>
 *
 * Previous, expressions are identical.
 *
 * <p>Run every 1 hour with an initial delay of 15 minutes for 10 times
 *
 * <pre>
 * &#64;Scheduled("1h; delay=15m; repeat=10")
 * </pre>
 *
 * <p>Fire at 12pm (noon) every day
 *
 * <pre>
 * 0 0 12 * * ?
 * </pre>
 *
 * <p>Fire at 10:15am every day
 *
 * <pre>
 * 0 15 10 ? * *
 * </pre>
 *
 * @author edgar
 * @since 0.5.0
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Scheduled {

  /**
   * Expression can be one of these three options:
   *
   * <ol>
   *   <li>Interval: 10s, 10secs, 10minutes, 1h, etc...
   *   <li>Cron expression: 0/3 * * * * ?
   *   <li>Reference to a property, where the property value is one of the two previous options
   * </ol>
   *
   * @return an expression to create an scheduler.
   * @see com.typesafe.config.Config#getDuration(String, java.util.concurrent.TimeUnit)
   */
  String value();

  /**
   * Sometimes, when you have many Triggers (or few worker threads in your Quartz thread pool),
   * Quartz may not have enough resources to immediately fire all of the Triggers that are scheduled
   * to fire at the same time. In this case, you may want to control which of your Triggers get
   * first crack at the available Quartz worker threads. For this purpose, you can set the priority
   * property on a Trigger.
   *
   * @return Quartz calendar.
   */
  String calendar() default "";

  /**
   * Quartz Calendar objects (not java.util.Calendar objects) can be associated with triggers at the
   * time the trigger is defined and stored in the scheduler. Calendars are useful for excluding
   * blocks of time from the the trigger’s firing schedule.
   *
   * @return Priority default 5.
   */
  int priority() default Trigger.DEFAULT_PRIORITY;

  /**
   * A misfire occurs if a persistent trigger "misses" its firing time because of the scheduler
   * being shutdown, or because there are no available threads in Quartz’s thread pool for executing
   * the job.
   *
   * @return Misfire instruction.
   */
  int misfire() default Trigger.MISFIRE_INSTRUCTION_SMART_POLICY;
}
