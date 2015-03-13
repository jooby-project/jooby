package org.jooby.internal.quartz;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.jooby.MockUnit;
import org.jooby.quartz.Scheduled;
import org.junit.Test;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;

public class JobBeanExpanderTest {

  public static class NoScheduled {

    public void job() {
    }

  }

  public static class StaticCallback {

    @Scheduled("5s")
    public static void runAt() {
    }

  }

  public static class PrivateCallback {

    @Scheduled("5s")
    private void runAt() {
    }

  }

  public static class BadArgCallback {

    @Scheduled("5s")
    public void runAt(final String something) {
    }

  }

  public static class BadArgsCallback {

    @Scheduled("5s")
    public void runAt(final String something, final int x) {
    }

  }

  public static class IntervalJob {

    @Scheduled("5s")
    public void runAt() {
    }

  }

  public static class CronJob {

    @Scheduled("0/3 * * * * ?")
    public void doWork() {
    }

  }

  public static class InvervalPropertyRefJob {

    @Scheduled("job.scheduled")
    public void doWork() {
    }

  }

  public static class CronRefJob {

    @Scheduled("job.scheduled")
    public void doWork(final JobExecutionContext ctx) {
    }

  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailWhenAnnotationIsMissing() throws Exception {
    new MockUnit(Config.class)
        .run(unit -> {
          JobExpander.jobs(unit.get(Config.class), Arrays.asList(NoScheduled.class));
        });
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailOnStaticCallbacks() throws Exception {
    new MockUnit(Config.class)
        .run(unit -> {
          JobExpander.jobs(unit.get(Config.class), Arrays.asList(StaticCallback.class));
        });
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailOnPrivateCallback() throws Exception {
    new MockUnit(Config.class)
        .run(unit -> {
          JobExpander.jobs(unit.get(Config.class), Arrays.asList(PrivateCallback.class));
        });
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailOnBadArgCallback() throws Exception {
    new MockUnit(Config.class)
        .run(unit -> {
          JobExpander.jobs(unit.get(Config.class), Arrays.asList(BadArgCallback.class));
        });
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailOnBadArgsCallback() throws Exception {
    new MockUnit(Config.class)
        .run(unit -> {
          JobExpander.jobs(unit.get(Config.class), Arrays.asList(BadArgsCallback.class));
        });
  }

  @Test
  public void shouldBuildIntervalJob() throws Exception {
    new MockUnit(Config.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.hasPath("5s")).andReturn(false);
        })
        .run(unit -> {
          Entry<JobDetail, Trigger> entry = JobExpander
              .jobs(unit.get(Config.class), Arrays.asList(IntervalJob.class)).entrySet()
              .iterator().next();

          JobDetail job = entry.getKey();
          assertEquals(ReflectiveJob.class, job.getJobClass());
          assertEquals(IntervalJob.class.getPackage().getName(), job.getKey().getGroup());
          assertEquals("JobBeanExpanderTest$IntervalJob.runAt", job.getKey().getName());

          SimpleTrigger trigger = (SimpleTrigger) entry.getValue();
          assertEquals(5000L, trigger.getRepeatInterval());
          assertEquals(IntervalJob.class.getPackage().getName(), trigger.getKey().getGroup());
          assertEquals("JobBeanExpanderTest$IntervalJob.runAt", trigger.getKey().getName());
        });
  }

  @Test
  public void shouldBuildCronJob() throws Exception {
    new MockUnit(Config.class)
        .expect(
            unit -> {
              Config config = unit.get(Config.class);
              expect(config.hasPath("0/3 * * * * ?")).andThrow(
                  new ConfigException.BadPath("0/3 * * * * ?", "0/3 * * * * ?"));
            })
        .run(unit -> {
          Entry<JobDetail, Trigger> entry = JobExpander
              .jobs(unit.get(Config.class), Arrays.asList(CronJob.class)).entrySet()
              .iterator().next();

          JobDetail job = entry.getKey();
          assertEquals(ReflectiveJob.class, job.getJobClass());
          assertEquals(CronJob.class.getPackage().getName(), job.getKey().getGroup());
          assertEquals("JobBeanExpanderTest$CronJob.doWork", job.getKey().getName());

          CronTrigger trigger = (CronTrigger) entry.getValue();
          assertEquals("0/3 * * * * ?", trigger.getCronExpression());
          assertEquals(CronJob.class.getPackage().getName(), trigger.getKey().getGroup());
          assertEquals("JobBeanExpanderTest$CronJob.doWork", trigger.getKey().getName());
        });
  }

  @Test
  public void shouldBuildInvervalJobWithPropertyRef() throws Exception {
    new MockUnit(Config.class)
        .expect(
            unit -> {
              Config config = unit.get(Config.class);
              expect(config.hasPath("job.scheduled")).andReturn(true);
              expect(config.getDuration("job.scheduled", TimeUnit.MILLISECONDS)).andReturn(3000L);
            })
        .run(
            unit -> {
              Entry<JobDetail, Trigger> entry = JobExpander
                  .jobs(unit.get(Config.class), Arrays.asList(InvervalPropertyRefJob.class))
                  .entrySet()
                  .iterator().next();

              JobDetail job = entry.getKey();
              assertEquals(ReflectiveJob.class, job.getJobClass());
              assertEquals(InvervalPropertyRefJob.class.getPackage().getName(), job.getKey()
                  .getGroup());
              assertEquals("JobBeanExpanderTest$InvervalPropertyRefJob.doWork", job.getKey()
                  .getName());

              SimpleTrigger trigger = (SimpleTrigger) entry.getValue();
              assertEquals(3000L, trigger.getRepeatInterval());
              assertEquals(InvervalPropertyRefJob.class.getPackage().getName(), trigger.getKey()
                  .getGroup());
              assertEquals("JobBeanExpanderTest$InvervalPropertyRefJob.doWork", trigger
                  .getKey().getName());
            });
  }

  @Test
  public void shouldBuildCronJobWithPropertyRef() throws Exception {
    new MockUnit(Config.class)
        .expect(
            unit -> {
              Config config = unit.get(Config.class);
              expect(config.hasPath("job.scheduled")).andReturn(true);
              expect(config.getDuration("job.scheduled", TimeUnit.MILLISECONDS)).andThrow(
                  new ConfigException.BadValue("/path", "bad"));
              expect(config.getString("job.scheduled")).andReturn("0/3 * * * * ?");
            })
        .run(
            unit -> {
              Entry<JobDetail, Trigger> entry = JobExpander
                  .jobs(unit.get(Config.class), Arrays.asList(CronRefJob.class))
                  .entrySet()
                  .iterator().next();

              JobDetail job = entry.getKey();
              assertEquals(ReflectiveJob.class, job.getJobClass());
              assertEquals(CronRefJob.class.getPackage().getName(), job.getKey()
                  .getGroup());
              assertEquals("JobBeanExpanderTest$CronRefJob.doWork", job.getKey()
                  .getName());

              CronTrigger trigger = (CronTrigger) entry.getValue();
              assertEquals("0/3 * * * * ?", trigger.getCronExpression());
              assertEquals(CronRefJob.class.getPackage().getName(), trigger.getKey()
                  .getGroup());
              assertEquals("JobBeanExpanderTest$CronRefJob.doWork", trigger.getKey()
                  .getName());
            });
  }

}
