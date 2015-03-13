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
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;

public class JobExpanderTest {

  public static class NoScheduled implements Job {

    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
    }

  }

  public static class IntervalJob implements Job {

    @Override
    @Scheduled("5s")
    public void execute(final JobExecutionContext context) throws JobExecutionException {
    }

  }

  public static class CronJob implements Job {

    @Override
    @Scheduled("0/3 * * * * ?")
    public void execute(final JobExecutionContext context) throws JobExecutionException {
    }

  }

  public static class InvervalPropertyRefJob implements Job {

    @Override
    @Scheduled("job.scheduled")
    public void execute(final JobExecutionContext context) throws JobExecutionException {
    }

  }

  public static class InvervalCronRefJob implements Job {

    @Override
    @Scheduled("job.scheduled")
    public void execute(final JobExecutionContext context) throws JobExecutionException {
    }

  }

  @Test
  public void sillyJacocoThinkWeNeedToInstantiateJobExpender() throws Exception {
    new JobExpander();
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailWhenAnnotationIsMissing() throws Exception {
    new MockUnit(Config.class)
        .expect(unit -> {
        })
        .run(unit -> {
          JobExpander.jobs(unit.get(Config.class), Arrays.asList(NoScheduled.class));
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
          assertEquals(IntervalJob.class, job.getJobClass());
          assertEquals(IntervalJob.class.getPackage().getName(), job.getKey().getGroup());
          assertEquals(IntervalJob.class.getSimpleName(), job.getKey().getName());

          SimpleTrigger trigger = (SimpleTrigger) entry.getValue();
          assertEquals(5000L, trigger.getRepeatInterval());
          assertEquals(IntervalJob.class.getPackage().getName(), trigger.getKey().getGroup());
          assertEquals(IntervalJob.class.getSimpleName(), trigger.getKey().getName());
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
          assertEquals(CronJob.class, job.getJobClass());
          assertEquals(CronJob.class.getPackage().getName(), job.getKey().getGroup());
          assertEquals(CronJob.class.getSimpleName(), job.getKey().getName());

          CronTrigger trigger = (CronTrigger) entry.getValue();
          assertEquals("0/3 * * * * ?", trigger.getCronExpression());
          assertEquals(CronJob.class.getPackage().getName(), trigger.getKey().getGroup());
          assertEquals(CronJob.class.getSimpleName(), trigger.getKey().getName());
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
              assertEquals(InvervalPropertyRefJob.class, job.getJobClass());
              assertEquals(InvervalPropertyRefJob.class.getPackage().getName(), job.getKey()
                  .getGroup());
              assertEquals(InvervalPropertyRefJob.class.getSimpleName(), job.getKey().getName());

              SimpleTrigger trigger = (SimpleTrigger) entry.getValue();
              assertEquals(3000L, trigger.getRepeatInterval());
              assertEquals(InvervalPropertyRefJob.class.getPackage().getName(), trigger.getKey()
                  .getGroup());
              assertEquals(InvervalPropertyRefJob.class.getSimpleName(), trigger.getKey().getName());
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
        .run(unit -> {
          Entry<JobDetail, Trigger> entry = JobExpander
              .jobs(unit.get(Config.class), Arrays.asList(InvervalCronRefJob.class))
              .entrySet()
              .iterator().next();

          JobDetail job = entry.getKey();
          assertEquals(InvervalCronRefJob.class, job.getJobClass());
          assertEquals(InvervalCronRefJob.class.getPackage().getName(), job.getKey()
              .getGroup());
          assertEquals(InvervalCronRefJob.class.getSimpleName(), job.getKey().getName());

          CronTrigger trigger = (CronTrigger) entry.getValue();
          assertEquals("0/3 * * * * ?", trigger.getCronExpression());
          assertEquals(InvervalCronRefJob.class.getPackage().getName(), trigger.getKey()
              .getGroup());
          assertEquals(InvervalCronRefJob.class.getSimpleName(), trigger.getKey().getName());
        });
  }

}
