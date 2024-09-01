/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.dbscheduler;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import com.github.kagkarlsson.scheduler.task.schedule.CronSchedule;
import com.github.kagkarlsson.scheduler.task.schedule.Daily;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import com.github.kagkarlsson.scheduler.task.schedule.Schedule;

public class SchedulerParserTest {

  @Test
  public void parse() {
    parse(
        "DAILY|12:30",
        schedule -> {
          assertInstanceOf(Daily.class, schedule);
        });
    parse(
        "FIXED_DELAY|120s",
        schedule -> {
          assertInstanceOf(FixedDelay.class, schedule);
        });
    parse(
        "0 0 1 * * ?",
        schedule -> {
          assertInstanceOf(CronSchedule.class, schedule);
        });
    parse(
        "1m",
        schedule -> {
          assertInstanceOf(FixedDelay.class, schedule);
        });
  }

  public static void parse(String value, Consumer<Schedule> consumer) {
    consumer.accept(DbScheduleParser.parseSchedule(value));
  }
}
