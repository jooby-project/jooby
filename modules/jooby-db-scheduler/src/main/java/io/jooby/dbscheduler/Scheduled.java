/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.dbscheduler;

import java.lang.annotation.*;

/**
 * Define a {@link com.github.kagkarlsson.scheduler.task.schedule.Schedule schedule}. Supported
 * values: - 0 0 1 * * ? (cron expression) - 1m (fixed schedule) - 1h (fixed schedule) - 15s (fixed
 * schedule) - DAILY|12:30,15:30 - FIXED_DELAY|120s (must be expressed in seconds)
 *
 * <p>All task created are recurring task.
 *
 * @author edgar
 * @since 3.2.10
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Scheduled {

  /**
   * Expression can be one of these three options:
   *
   * <p>- 0 0 1 * * ? (cron expression) - 1m (fixed schedule) - 1h (fixed schedule) - 15s (fixed
   * schedule) - DAILY|12:30,15:30 - FIXED_DELAY|120s (must be expressed in seconds)
   *
   * @return an expression to create a schedule.
   * @see DbScheduleParser#parseSchedule(String)
   */
  String value();
}
