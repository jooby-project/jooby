package org.jooby.internal.quartz;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

import javax.inject.Provider;
import javax.sql.DataSource;

import org.jooby.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.quartz.CalendarIntervalTrigger;
import org.quartz.CronTrigger;
import org.quartz.DailyTimeIntervalTrigger;
import org.quartz.DateBuilder.IntervalUnit;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.jdbcjobstore.JobStoreTX;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;
import org.quartz.utils.DBConnectionManager;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

@RunWith(PowerMockRunner.class)
@PrepareForTest({QuartzProvider.class, StdSchedulerFactory.class, DBConnectionManager.class,
    QuartzConnectionProvider.class })
public class QuartzProviderTest {

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void defaultSetup() throws Exception {
    Config conf = ConfigFactory.empty()
        .withValue("org.quartz.jobStore.class", ConfigValueFactory.fromAnyRef("X"));
    Properties props = new Properties();
    props.setProperty("org.quartz.jobStore.class", "X");
    new MockUnit(Injector.class, Scheduler.class, Map.class)
        .expect(unit -> {
          Scheduler scheduler = unit.get(Scheduler.class);

          scheduler.setJobFactory(isA(JobFactory.class));
        })
        .expect(unit -> {
          Map triggers = unit.get(Map.class);

          expect(triggers.entrySet()).andReturn(Collections.emptySet());
        })
        .expect(unit -> {
          Scheduler scheduler = unit.get(Scheduler.class);

          StdSchedulerFactory factory = unit.mockConstructor(StdSchedulerFactory.class,
              new Class[]{Properties.class }, props);
          expect(factory.getScheduler()).andReturn(scheduler);
        })
        .run(unit -> {
          new QuartzProvider(unit.get(Injector.class), conf, unit.get(Map.class));
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void guiceJobFactory() throws Exception {
    Config conf = ConfigFactory.empty()
        .withValue("org.quartz.jobStore.class", ConfigValueFactory.fromAnyRef("X"));
    Properties props = new Properties();
    props.setProperty("org.quartz.jobStore.class", "X");

    Job job = ctx -> {
    };

    new MockUnit(Injector.class, Scheduler.class, Map.class, TriggerFiredBundle.class)
        .expect(unit -> {
          Scheduler scheduler = unit.get(Scheduler.class);

          scheduler.setJobFactory(unit.capture(JobFactory.class));
        })
        .expect(unit -> {
          Map triggers = unit.get(Map.class);

          expect(triggers.entrySet()).andReturn(Collections.emptySet());
        })
        .expect(unit -> {
          Class jobClass = Job.class;

          JobDetail jobDetail = unit.mock(JobDetail.class);
          expect(jobDetail.getJobClass()).andReturn(jobClass);

          TriggerFiredBundle bundle = unit.get(TriggerFiredBundle.class);
          expect(bundle.getJobDetail()).andReturn(jobDetail);

          Injector injector = unit.get(Injector.class);
          expect(injector.getInstance(jobClass)).andReturn(job);
        })
        .expect(unit -> {
          Scheduler scheduler = unit.get(Scheduler.class);

          StdSchedulerFactory factory = unit.mockConstructor(StdSchedulerFactory.class,
              new Class[]{Properties.class }, props);
          expect(factory.getScheduler()).andReturn(scheduler);
        })
        .run(
            unit -> {
              new QuartzProvider(unit.get(Injector.class), conf, unit.get(Map.class));
            },
            unit -> {
              JobFactory factory = unit.captured(JobFactory.class).iterator().next();
              Job newJob = factory.newJob(unit.get(TriggerFiredBundle.class),
                  unit.get(Scheduler.class));
              assertEquals(job, newJob);
            });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void jdbcSetup() throws Exception {
    Config conf = ConfigFactory.empty()
        .withValue("org.quartz.jobStore.dataSource", ConfigValueFactory.fromAnyRef("db"));
    Properties props = new Properties();
    props.setProperty("org.quartz.jobStore.dataSource", "db");
    new MockUnit(Injector.class, Scheduler.class, Map.class)
        .expect(unit -> {
          Scheduler scheduler = unit.get(Scheduler.class);

          scheduler.setJobFactory(isA(JobFactory.class));
        })
        .expect(unit -> {
          Map triggers = unit.get(Map.class);

          expect(triggers.entrySet()).andReturn(Collections.emptySet());
        })
        .expect(unit -> {
          Scheduler scheduler = unit.get(Scheduler.class);

          StdSchedulerFactory factory = unit.mockConstructor(StdSchedulerFactory.class,
              new Class[]{Properties.class }, props);
          expect(factory.getScheduler()).andReturn(scheduler);
        })
        .expect(
            unit -> {
              Key<Provider<DataSource>> dskey = Key.get(QuartzProvider.DS_TYPE, Names.named("db"));

              Provider<DataSource> ds = unit.mock(Provider.class);

              Injector injector = unit.get(Injector.class);
              expect(injector.getInstance(dskey)).andReturn(ds);

              QuartzConnectionProvider cnn = unit.mockConstructor(QuartzConnectionProvider.class,
                  new Class[]{Provider.class }, ds);

              DBConnectionManager dbm = unit.mock(DBConnectionManager.class);
              dbm.addConnectionProvider("db", cnn);

              unit.mockStatic(DBConnectionManager.class);
              expect(DBConnectionManager.getInstance()).andReturn(dbm);
            })
        .run(unit -> {
          new QuartzProvider(unit.get(Injector.class), conf, unit.get(Map.class));
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test(expected = IllegalArgumentException.class)
  public void jdbcSetupNoDB() throws Exception {
    Config conf = ConfigFactory.empty()
        .withValue("org.quartz.jobStore.class",
            ConfigValueFactory.fromAnyRef(JobStoreTX.class.getName()));
    Properties props = new Properties();
    props.setProperty("org.quartz.jobStore.class", JobStoreTX.class.getName());
    new MockUnit(Injector.class, Scheduler.class, Map.class)
        .expect(unit -> {
          Scheduler scheduler = unit.get(Scheduler.class);

          scheduler.setJobFactory(isA(JobFactory.class));
        })
        .expect(unit -> {
          Map triggers = unit.get(Map.class);

          expect(triggers.entrySet()).andReturn(Collections.emptySet());
        })
        .expect(unit -> {
          Scheduler scheduler = unit.get(Scheduler.class);

          StdSchedulerFactory factory = unit.mockConstructor(StdSchedulerFactory.class,
              new Class[]{Properties.class }, props);
          expect(factory.getScheduler()).andReturn(scheduler);
        })
        .expect(
            unit -> {
              Key<Provider<DataSource>> dskey = Key.get(QuartzProvider.DS_TYPE, Names.named("db"));

              Provider<DataSource> ds = unit.mock(Provider.class);

              Injector injector = unit.get(Injector.class);
              expect(injector.getInstance(dskey)).andReturn(ds);

              QuartzConnectionProvider cnn = unit.mockConstructor(QuartzConnectionProvider.class,
                  new Class[]{Provider.class }, ds);

              DBConnectionManager dbm = unit.mock(DBConnectionManager.class);
              dbm.addConnectionProvider("db", cnn);

              unit.mockStatic(DBConnectionManager.class);
              expect(DBConnectionManager.getInstance()).andReturn(dbm);
            })
        .run(unit -> {
          new QuartzProvider(unit.get(Injector.class), conf, unit.get(Map.class));
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void start() throws Exception {
    Config conf = ConfigFactory.empty()
        .withValue("org.quartz.jobStore.class", ConfigValueFactory.fromAnyRef("X"));
    Properties props = new Properties();
    props.setProperty("org.quartz.jobStore.class", "X");
    new MockUnit(Injector.class, Scheduler.class, Map.class, JobDetail.class, Trigger.class)
        .expect(unit -> {
          Scheduler scheduler = unit.get(Scheduler.class);

          scheduler.setJobFactory(isA(JobFactory.class));
        })
        .expect(
            unit -> {
              Map triggers = unit.get(Map.class);

              JobDetail job = unit.get(JobDetail.class);
              expect(job.getKey()).andReturn(JobKey.jobKey("j"));

              Trigger trigger = unit.get(Trigger.class);
              expect(trigger.getDescription()).andReturn(null);

              expect(triggers.entrySet()).andReturn(
                  Sets.newHashSet(Maps.immutableEntry(job, trigger)));

              Scheduler scheduler = unit.get(Scheduler.class);
              expect(scheduler.scheduleJob(job, trigger)).andReturn(new Date());

              scheduler.start();
            })
        .expect(unit -> {
          Scheduler scheduler = unit.get(Scheduler.class);

          StdSchedulerFactory factory = unit.mockConstructor(StdSchedulerFactory.class,
              new Class[]{Properties.class }, props);
          expect(factory.getScheduler()).andReturn(scheduler);
        })
        .run(unit -> {
          new QuartzProvider(unit.get(Injector.class), conf, unit.get(Map.class)).start();
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void stop() throws Exception {
    Config conf = ConfigFactory.empty()
        .withValue("org.quartz.jobStore.class", ConfigValueFactory.fromAnyRef("X"));
    Properties props = new Properties();
    props.setProperty("org.quartz.jobStore.class", "X");
    new MockUnit(Injector.class, Scheduler.class, Map.class)
        .expect(unit -> {
          Scheduler scheduler = unit.get(Scheduler.class);

          scheduler.setJobFactory(isA(JobFactory.class));
        })
        .expect(unit -> {
          Map triggers = unit.get(Map.class);
          expect(triggers.entrySet()).andReturn(Collections.emptySet());

          Scheduler scheduler = unit.get(Scheduler.class);

          scheduler.shutdown();
        })
        .expect(unit -> {
          Scheduler scheduler = unit.get(Scheduler.class);

          StdSchedulerFactory factory = unit.mockConstructor(StdSchedulerFactory.class,
              new Class[]{Properties.class }, props);
          expect(factory.getScheduler()).andReturn(scheduler);
        })
        .run(unit -> {
          new QuartzProvider(unit.get(Injector.class), conf, unit.get(Map.class)).stop();
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void get() throws Exception {
    Config conf = ConfigFactory.empty()
        .withValue("org.quartz.jobStore.class", ConfigValueFactory.fromAnyRef("X"));
    Properties props = new Properties();
    props.setProperty("org.quartz.jobStore.class", "X");
    new MockUnit(Injector.class, Scheduler.class, Map.class)
        .expect(unit -> {
          Scheduler scheduler = unit.get(Scheduler.class);

          scheduler.setJobFactory(isA(JobFactory.class));
        })
        .expect(unit -> {
          Map triggers = unit.get(Map.class);
          expect(triggers.entrySet()).andReturn(Collections.emptySet());
        })
        .expect(unit -> {
          Scheduler scheduler = unit.get(Scheduler.class);

          StdSchedulerFactory factory = unit.mockConstructor(StdSchedulerFactory.class,
              new Class[]{Properties.class }, props);
          expect(factory.getScheduler()).andReturn(scheduler);
        })
        .run(unit -> {
          assertEquals(unit.get(Scheduler.class), new QuartzProvider(unit.get(Injector.class),
              conf, unit.get(Map.class)).get());
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void startTriggerWithDescription() throws Exception {
    Config conf = ConfigFactory.empty()
        .withValue("org.quartz.jobStore.class", ConfigValueFactory.fromAnyRef("X"));
    Properties props = new Properties();
    props.setProperty("org.quartz.jobStore.class", "X");
    new MockUnit(Injector.class, Scheduler.class, Map.class, JobDetail.class, Trigger.class)
        .expect(unit -> {
          Scheduler scheduler = unit.get(Scheduler.class);

          scheduler.setJobFactory(isA(JobFactory.class));
        })
        .expect(
            unit -> {
              Map triggers = unit.get(Map.class);

              JobDetail job = unit.get(JobDetail.class);
              expect(job.getKey()).andReturn(JobKey.jobKey("j"));

              Trigger trigger = unit.get(Trigger.class);
              expect(trigger.getDescription()).andReturn("desc").times(2);

              expect(triggers.entrySet()).andReturn(
                  Sets.newHashSet(Maps.immutableEntry(job, trigger)));

              Scheduler scheduler = unit.get(Scheduler.class);
              expect(scheduler.scheduleJob(job, trigger)).andReturn(new Date());

              scheduler.start();
            })
        .expect(unit -> {
          Scheduler scheduler = unit.get(Scheduler.class);

          StdSchedulerFactory factory = unit.mockConstructor(StdSchedulerFactory.class,
              new Class[]{Properties.class }, props);
          expect(factory.getScheduler()).andReturn(scheduler);
        })
        .run(unit -> {
          new QuartzProvider(unit.get(Injector.class), conf, unit.get(Map.class)).start();
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void startSimpleTrigger() throws Exception {
    Config conf = ConfigFactory.empty()
        .withValue("org.quartz.jobStore.class", ConfigValueFactory.fromAnyRef("X"));
    Properties props = new Properties();
    props.setProperty("org.quartz.jobStore.class", "X");
    new MockUnit(Injector.class, Scheduler.class, Map.class, JobDetail.class, SimpleTrigger.class)
        .expect(unit -> {
          Scheduler scheduler = unit.get(Scheduler.class);

          scheduler.setJobFactory(isA(JobFactory.class));
        })
        .expect(
            unit -> {
              Map triggers = unit.get(Map.class);

              JobDetail job = unit.get(JobDetail.class);
              expect(job.getKey()).andReturn(JobKey.jobKey("j"));

              SimpleTrigger trigger = unit.get(SimpleTrigger.class);
              expect(trigger.getDescription()).andReturn(null);
              expect(trigger.getRepeatInterval()).andReturn(1000L);

              expect(triggers.entrySet()).andReturn(
                  Sets.newHashSet(Maps.immutableEntry(job, trigger)));

              Scheduler scheduler = unit.get(Scheduler.class);
              expect(scheduler.scheduleJob(job, trigger)).andReturn(new Date());

              scheduler.start();
            })
        .expect(unit -> {
          Scheduler scheduler = unit.get(Scheduler.class);

          StdSchedulerFactory factory = unit.mockConstructor(StdSchedulerFactory.class,
              new Class[]{Properties.class }, props);
          expect(factory.getScheduler()).andReturn(scheduler);
        })
        .run(unit -> {
          new QuartzProvider(unit.get(Injector.class), conf, unit.get(Map.class)).start();
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void startCalendarTrigger() throws Exception {
    Config conf = ConfigFactory.empty()
        .withValue("org.quartz.jobStore.class", ConfigValueFactory.fromAnyRef("X"));
    Properties props = new Properties();
    props.setProperty("org.quartz.jobStore.class", "X");
    new MockUnit(Injector.class, Scheduler.class, Map.class, JobDetail.class,
        CalendarIntervalTrigger.class)
        .expect(unit -> {
          Scheduler scheduler = unit.get(Scheduler.class);

          scheduler.setJobFactory(isA(JobFactory.class));
        })
        .expect(
            unit -> {
              Map triggers = unit.get(Map.class);

              JobDetail job = unit.get(JobDetail.class);
              expect(job.getKey()).andReturn(JobKey.jobKey("j"));

              CalendarIntervalTrigger trigger = unit.get(CalendarIntervalTrigger.class);
              expect(trigger.getDescription()).andReturn(null);
              expect(trigger.getRepeatInterval()).andReturn(1000);
              expect(trigger.getRepeatIntervalUnit()).andReturn(IntervalUnit.DAY);

              expect(triggers.entrySet()).andReturn(
                  Sets.newHashSet(Maps.immutableEntry(job, trigger)));

              Scheduler scheduler = unit.get(Scheduler.class);
              expect(scheduler.scheduleJob(job, trigger)).andReturn(new Date());

              scheduler.start();
            })
        .expect(unit -> {
          Scheduler scheduler = unit.get(Scheduler.class);

          StdSchedulerFactory factory = unit.mockConstructor(StdSchedulerFactory.class,
              new Class[]{Properties.class }, props);
          expect(factory.getScheduler()).andReturn(scheduler);
        })
        .run(unit -> {
          new QuartzProvider(unit.get(Injector.class), conf, unit.get(Map.class)).start();
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void startDailyTrigger() throws Exception {
    Config conf = ConfigFactory.empty()
        .withValue("org.quartz.jobStore.class", ConfigValueFactory.fromAnyRef("X"));
    Properties props = new Properties();
    props.setProperty("org.quartz.jobStore.class", "X");
    new MockUnit(Injector.class, Scheduler.class, Map.class, JobDetail.class,
        DailyTimeIntervalTrigger.class)
        .expect(unit -> {
          Scheduler scheduler = unit.get(Scheduler.class);

          scheduler.setJobFactory(isA(JobFactory.class));
        })
        .expect(
            unit -> {
              Map triggers = unit.get(Map.class);

              JobDetail job = unit.get(JobDetail.class);
              expect(job.getKey()).andReturn(JobKey.jobKey("j"));

              DailyTimeIntervalTrigger trigger = unit.get(DailyTimeIntervalTrigger.class);
              expect(trigger.getDescription()).andReturn(null);
              expect(trigger.getRepeatInterval()).andReturn(1000);
              expect(trigger.getRepeatIntervalUnit()).andReturn(IntervalUnit.DAY);

              expect(triggers.entrySet()).andReturn(
                  Sets.newHashSet(Maps.immutableEntry(job, trigger)));

              Scheduler scheduler = unit.get(Scheduler.class);
              expect(scheduler.scheduleJob(job, trigger)).andReturn(new Date());

              scheduler.start();
            })
        .expect(unit -> {
          Scheduler scheduler = unit.get(Scheduler.class);

          StdSchedulerFactory factory = unit.mockConstructor(StdSchedulerFactory.class,
              new Class[]{Properties.class }, props);
          expect(factory.getScheduler()).andReturn(scheduler);
        })
        .run(unit -> {
          new QuartzProvider(unit.get(Injector.class), conf, unit.get(Map.class)).start();
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void startCronTrigger() throws Exception {
    Config conf = ConfigFactory.empty()
        .withValue("org.quartz.jobStore.class", ConfigValueFactory.fromAnyRef("X"));
    Properties props = new Properties();
    props.setProperty("org.quartz.jobStore.class", "X");
    new MockUnit(Injector.class, Scheduler.class, Map.class, JobDetail.class, CronTrigger.class)
        .expect(unit -> {
          Scheduler scheduler = unit.get(Scheduler.class);

          scheduler.setJobFactory(isA(JobFactory.class));
        })
        .expect(
            unit -> {
              Map triggers = unit.get(Map.class);

              JobDetail job = unit.get(JobDetail.class);
              expect(job.getKey()).andReturn(JobKey.jobKey("j"));

              CronTrigger trigger = unit.get(CronTrigger.class);
              expect(trigger.getDescription()).andReturn(null);
              expect(trigger.getCronExpression()).andReturn("0/3 0 * * *");

              expect(triggers.entrySet()).andReturn(
                  Sets.newHashSet(Maps.immutableEntry(job, trigger)));

              Scheduler scheduler = unit.get(Scheduler.class);
              expect(scheduler.scheduleJob(job, trigger)).andReturn(new Date());

              scheduler.start();
            })
        .expect(unit -> {
          Scheduler scheduler = unit.get(Scheduler.class);

          StdSchedulerFactory factory = unit.mockConstructor(StdSchedulerFactory.class,
              new Class[]{Properties.class }, props);
          expect(factory.getScheduler()).andReturn(scheduler);
        })
        .run(unit -> {
          new QuartzProvider(unit.get(Injector.class), conf, unit.get(Map.class)).start();
        });
  }

}
