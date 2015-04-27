package org.jooby.internal.quartz;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Date;
import java.util.Map.Entry;

import org.jooby.MockUnit;
import org.jooby.quartz.Scheduled;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;

@RunWith(PowerMockRunner.class)
@PrepareForTest({JobExpander.class, System.class })
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

  public static class IntervalJobUnknownAttribute implements Job {

    @Override
    @Scheduled("5s; x= 1")
    public void execute(final JobExecutionContext context) throws JobExecutionException {
    }

  }

  public static class IntervalJobWithDelay implements Job {

    @Override
    @Scheduled("3s; delay = 1s")
    public void execute(final JobExecutionContext context) throws JobExecutionException {
    }

  }

  public static class IntervalJobWithDelayAndRepeat implements Job {

    @Override
    @Scheduled("3s; delay = 1s; repeat =repeat.prop")
    public void execute(final JobExecutionContext context) throws JobExecutionException {
    }

  }

  public static class IntervalJobWithRepeat implements Job {

    @Override
    @Scheduled("3s; repeat = 10")
    public void execute(final JobExecutionContext context) throws JobExecutionException {
    }

  }

  public static class IntervalJobWithRepeatForEver implements Job {

    @Override
    @Scheduled("3s; repeat=*")
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
    long date = 1429984623207L;
    new MockUnit(Config.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getString("5s")).andThrow(new ConfigException.BadPath("5s", "5s"));
        })
        .expect(unit -> {
          unit.mockStatic(System.class);

          expect(System.currentTimeMillis()).andReturn(date);
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
          assertEquals(new Date(date), trigger.getStartTime());
          assertEquals(-1, trigger.getRepeatCount());
          assertEquals(IntervalJob.class.getPackage().getName(), trigger.getKey().getGroup());
          assertEquals(IntervalJob.class.getSimpleName(), trigger.getKey().getName());
        });
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldBuildIntervalJobUnknownAttribute() throws Exception {
    long date = 1429984623207L;
    new MockUnit(Config.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getString("5s; x= 1")).andThrow(new ConfigException.BadPath("5s", "5s"));
        })
        .expect(unit -> {
          unit.mockStatic(System.class);

          expect(System.currentTimeMillis()).andReturn(date);
        })
        .run(unit -> {
          Entry<JobDetail, Trigger> entry = JobExpander
              .jobs(unit.get(Config.class), Arrays.asList(IntervalJobUnknownAttribute.class))
              .entrySet()
              .iterator().next();

          SimpleTrigger trigger = (SimpleTrigger) entry.getValue();
          assertEquals(5000L, trigger.getRepeatInterval());
          assertEquals(new Date(date), trigger.getStartTime());
          assertEquals(-1, trigger.getRepeatCount());
        });
  }

  @Test
  public void shouldBuildIntervalJobWithDelay() throws Exception {
    long date = 1429984623207L;
    new MockUnit(Config.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getString("3s; delay = 1s"))
              .andThrow(new ConfigException.BadPath("bad", "bad"));
          expect(config.getString("1s"))
              .andThrow(new ConfigException.BadPath("bad", "bad"));
        })
        .expect(unit -> {
          unit.mockStatic(System.class);

          expect(System.currentTimeMillis()).andReturn(date);
        })
        .run(
            unit -> {
              Entry<JobDetail, Trigger> entry = JobExpander
                  .jobs(unit.get(Config.class), Arrays.asList(IntervalJobWithDelay.class))
                  .entrySet()
                  .iterator().next();

              JobDetail job = entry.getKey();
              assertEquals(IntervalJobWithDelay.class, job.getJobClass());
              assertEquals(IntervalJobWithDelay.class.getPackage().getName(), job.getKey()
                  .getGroup());
              assertEquals(IntervalJobWithDelay.class.getSimpleName(), job.getKey().getName());

              SimpleTrigger trigger = (SimpleTrigger) entry.getValue();
              assertEquals(3000L, trigger.getRepeatInterval());
              assertEquals(new Date(date + 1000L), trigger.getStartTime());
              assertEquals(-1, trigger.getRepeatCount());
              assertEquals(IntervalJobWithDelay.class.getPackage().getName(), trigger.getKey()
                  .getGroup());
              assertEquals(IntervalJobWithDelay.class.getSimpleName(), trigger.getKey().getName());
            });
  }

  @Test
  public void shouldBuildIntervalJobWithDelayAndRepeat() throws Exception {
    long date = 1429984623207L;
    new MockUnit(Config.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getString("3s; delay = 1s; repeat =repeat.prop"))
              .andThrow(new ConfigException.BadPath("bad", "bad"));
          expect(config.getString("1s"))
              .andThrow(new ConfigException.BadPath("bad", "bad"));
          expect(config.getString("repeat.prop")).andReturn("1");
        })
        .expect(unit -> {
          unit.mockStatic(System.class);

          expect(System.currentTimeMillis()).andReturn(date);
        })
        .run(
            unit -> {
              Entry<JobDetail, Trigger> entry = JobExpander
                  .jobs(unit.get(Config.class), Arrays.asList(IntervalJobWithDelayAndRepeat.class))
                  .entrySet()
                  .iterator().next();

              SimpleTrigger trigger = (SimpleTrigger) entry.getValue();
              assertEquals(3000L, trigger.getRepeatInterval());
              assertEquals(new Date(date + 1000L), trigger.getStartTime());
              assertEquals(1, trigger.getRepeatCount());
            });
  }

  @Test
  public void shouldBuildIntervalJobWithRepeat() throws Exception {
    long date = 1429984623207L;
    new MockUnit(Config.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getString("3s; repeat = 10"))
              .andThrow(new ConfigException.BadPath("bad", "bad"));
          expect(config.getString("10"))
              .andThrow(new ConfigException.BadPath("bad", "bad"));
        })
        .expect(unit -> {
          unit.mockStatic(System.class);

          expect(System.currentTimeMillis()).andReturn(date);
        })
        .run(
            unit -> {
              Entry<JobDetail, Trigger> entry = JobExpander
                  .jobs(unit.get(Config.class), Arrays.asList(IntervalJobWithRepeat.class))
                  .entrySet()
                  .iterator().next();

              JobDetail job = entry.getKey();
              assertEquals(IntervalJobWithRepeat.class, job.getJobClass());
              assertEquals(IntervalJobWithRepeat.class.getPackage().getName(), job.getKey()
                  .getGroup());
              assertEquals(IntervalJobWithRepeat.class.getSimpleName(), job.getKey().getName());

              SimpleTrigger trigger = (SimpleTrigger) entry.getValue();
              assertEquals(3000L, trigger.getRepeatInterval());
              assertEquals(new Date(date), trigger.getStartTime());
              assertEquals(10, trigger.getRepeatCount());
              assertEquals(IntervalJobWithRepeat.class.getPackage().getName(), trigger.getKey()
                  .getGroup());
              assertEquals(IntervalJobWithRepeat.class.getSimpleName(), trigger.getKey().getName());
            });
  }

  @Test
  public void shouldBuildIntervalJobWithRepeatForEver() throws Exception {
    long date = 1429984623207L;
    new MockUnit(Config.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getString("3s; repeat=*"))
              .andThrow(new ConfigException.BadPath("bad", "bad"));
        })
        .expect(unit -> {
          unit.mockStatic(System.class);

          expect(System.currentTimeMillis()).andReturn(date);
        })
        .run(
            unit -> {
              Entry<JobDetail, Trigger> entry = JobExpander
                  .jobs(unit.get(Config.class), Arrays.asList(IntervalJobWithRepeatForEver.class))
                  .entrySet()
                  .iterator().next();

              JobDetail job = entry.getKey();
              assertEquals(IntervalJobWithRepeatForEver.class, job.getJobClass());
              assertEquals(IntervalJobWithRepeatForEver.class.getPackage().getName(), job.getKey()
                  .getGroup());
              assertEquals(IntervalJobWithRepeatForEver.class.getSimpleName(), job.getKey()
                  .getName());

              SimpleTrigger trigger = (SimpleTrigger) entry.getValue();
              assertEquals(3000L, trigger.getRepeatInterval());
              assertEquals(new Date(date), trigger.getStartTime());
              assertEquals(-1, trigger.getRepeatCount());
              assertEquals(IntervalJobWithRepeatForEver.class.getPackage().getName(), trigger
                  .getKey().getGroup());
              assertEquals(IntervalJobWithRepeatForEver.class.getSimpleName(), trigger.getKey()
                  .getName());
            });
  }

  @Test
  public void shouldBuildCronJob() throws Exception {
    new MockUnit(Config.class)
        .expect(
            unit -> {
              Config config = unit.get(Config.class);
              expect(config.getString("0/3 * * * * ?")).andThrow(
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
              expect(config.getString("job.scheduled")).andReturn("3000");
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
