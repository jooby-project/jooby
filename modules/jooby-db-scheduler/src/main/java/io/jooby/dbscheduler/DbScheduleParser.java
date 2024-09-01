/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.dbscheduler;

import static com.typesafe.config.ConfigFactory.empty;
import static com.typesafe.config.ConfigValueFactory.fromAnyRef;

import com.github.kagkarlsson.scheduler.task.schedule.Schedule;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import com.typesafe.config.ConfigException;

/**
 * Parse {@link Schedule} from string expression. Supported values: - 0 0 1 * * ? (cron expression)
 * - 1m (fixed schedule) - 1h (fixed schedule) - 15s (fixed schedule) - DAILY|12:30,15:30 -
 * FIXED_DELAY|120s (must be expressed in seconds)
 */
public class DbScheduleParser {
  /**
   * Parse {@link Schedule} from string expression. Supported values: - 0 0 1 * * ? (cron
   * expression) - 1m (fixed schedule) - 1h (fixed schedule) - 15s (fixed schedule) -
   * DAILY|12:30,15:30 - FIXED_DELAY|120s (must be expressed in seconds)
   *
   * @param value String expression.
   * @return Parsed schedule.
   * @throws Schedules.UnrecognizableSchedule If something goes wrong.
   */
  public static Schedule parseSchedule(String value) throws Schedules.UnrecognizableSchedule {
    try {
      return Schedules.parseSchedule(value);
    } catch (Schedules.UnrecognizableSchedule cause) {
      try {
        return Schedules.cron(value);
      } catch (IllegalArgumentException notCron) {
        try {
          return Schedules.fixedDelay(
              empty().withValue("schedule", fromAnyRef(value)).getDuration("schedule"));
        } catch (ConfigException.WrongType | ConfigException.BadValue notDuration) {
          throw cause;
        }
      }
    }
  }
}
