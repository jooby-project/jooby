package org.jooby.quartz;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jooby.Env;
import org.jooby.internal.quartz.JobExpander;
import org.jooby.internal.quartz.QuartzProvider;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;

import com.google.inject.Binder;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.binder.ScopedBindingBuilder;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Quartz.class, JobExpander.class, ConfigFactory.class })
public class QuartzTest {

  @SuppressWarnings("unchecked")
  MockUnit.Block scheduler = unit -> {
    ScopedBindingBuilder scopeBB = unit.mock(ScopedBindingBuilder.class);
    scopeBB.asEagerSingleton();

    AnnotatedBindingBuilder<Scheduler> schedulerBB = unit
        .mock(AnnotatedBindingBuilder.class);
    expect(schedulerBB.toProvider(QuartzProvider.class)).andReturn(scopeBB);

    Binder binder = unit.get(Binder.class);
    expect(binder.bind(Scheduler.class)).andReturn(schedulerBB);
  };

  @SuppressWarnings("unchecked")
  MockUnit.Block jobUnit = unit -> {
    LinkedBindingBuilder<Map<JobDetail, Trigger>> namedJobsBB = unit
        .get(LinkedBindingBuilder.class);

    AnnotatedBindingBuilder<Map<JobDetail, Trigger>> jobsBB = unit
        .mock(AnnotatedBindingBuilder.class);
    expect(jobsBB.annotatedWith(Names.named("org.quartz.jobs"))).andReturn(namedJobsBB);

    Binder binder = unit.get(Binder.class);
    expect(binder.bind(new TypeLiteral<Map<JobDetail, Trigger>>() {
    })).andReturn(jobsBB);
  };

  MockUnit.Block onManaged = unit -> {
    Env env = unit.get(Env.class);
    expect(env.managed(QuartzProvider.class)).andReturn(env);
  };

  @SuppressWarnings({"unchecked" })
  @Test
  public void defaultUse() throws Exception {
    Map<JobDetail, Trigger> jobs = Collections.emptyMap();
    new MockUnit(Env.class, Config.class, Binder.class, LinkedBindingBuilder.class)
        .expect(unit -> {
          unit.mockStatic(JobExpander.class);
          expect(JobExpander.jobs(unit.get(Config.class), Arrays.asList(Job.class)))
              .andReturn(jobs);
        })
        .expect(scheduler)
        .expect(jobUnit)
        .expect(unit -> {
          LinkedBindingBuilder<Map<JobDetail, Trigger>> namedJobsBB = unit
              .get(LinkedBindingBuilder.class);
          namedJobsBB.toInstance(jobs);
        })
        .expect(onManaged)
        .run(unit -> {
          new Quartz(Job.class)
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings({"unchecked" })
  @Test
  public void withJob() throws Exception {
    Map<JobDetail, Trigger> jobs = Collections.emptyMap();
    new MockUnit(Env.class, Config.class, Binder.class, LinkedBindingBuilder.class)
        .expect(unit -> {
          unit.mockStatic(JobExpander.class);
          expect(JobExpander.jobs(unit.get(Config.class), Arrays.asList(Job.class)))
              .andReturn(jobs);
        })
        .expect(scheduler)
        .expect(jobUnit)
        .expect(unit -> {
          LinkedBindingBuilder<Map<JobDetail, Trigger>> namedJobsBB = unit
              .get(LinkedBindingBuilder.class);
          namedJobsBB.toInstance(jobs);
        })
        .expect(onManaged)
        .run(unit -> {
          new Quartz()
              .with(Job.class)
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings({"unchecked" })
  @Test
  public void withJobAndTrigger() throws Exception {
    Map<JobDetail, Trigger> jobs = new HashMap<JobDetail, Trigger>();
    new MockUnit(Env.class, Config.class, Binder.class, LinkedBindingBuilder.class,
        JobDetail.class,
        Trigger.class)
        .expect(unit -> {
          jobs.put(unit.get(JobDetail.class), unit.get(Trigger.class));

          unit.mockStatic(JobExpander.class);
          expect(JobExpander.jobs(unit.get(Config.class), Arrays.asList()))
              .andReturn(jobs);
        })
        .expect(scheduler)
        .expect(jobUnit)
        .expect(unit -> {
          LinkedBindingBuilder<Map<JobDetail, Trigger>> namedJobsBB = unit
              .get(LinkedBindingBuilder.class);
          namedJobsBB.toInstance(jobs);
        })
        .expect(onManaged)
        .run(unit -> {
          new Quartz()
              .with(unit.get(JobDetail.class), unit.get(Trigger.class))
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings({"unchecked" })
  @Test
  public void withJobAndTriggerBuilder() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, LinkedBindingBuilder.class,
        JobDetail.class,
        Trigger.class)
        .expect(unit -> {
          unit.mockStatic(JobExpander.class);
          expect(JobExpander.jobs(unit.get(Config.class), Arrays.asList()))
              .andReturn(Collections.emptyMap());
        })
        .expect(scheduler)
        .expect(jobUnit)
        .expect(unit -> {
          LinkedBindingBuilder<Map<JobDetail, Trigger>> namedJobsBB = unit
              .get(LinkedBindingBuilder.class);
          namedJobsBB.toInstance(unit.capture(Map.class));
        })
        .expect(onManaged)
        .run(unit -> {
          new Quartz()
              .with(Job.class, trigger -> {
                trigger.withSchedule(
                    SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMinutes(5)
                    );
              })
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, unit -> {
          assertEquals(1, unit.captured(Map.class).iterator().next().size());
        });
  }

  @SuppressWarnings({"unchecked" })
  @Test
  public void withJobBuilderAndTriggerBuilder() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, LinkedBindingBuilder.class,
        JobDetail.class,
        Trigger.class)
        .expect(unit -> {
          unit.mockStatic(JobExpander.class);
          expect(JobExpander.jobs(unit.get(Config.class), Arrays.asList()))
              .andReturn(Collections.emptyMap());
        })
        .expect(scheduler)
        .expect(jobUnit)
        .expect(unit -> {
          LinkedBindingBuilder<Map<JobDetail, Trigger>> namedJobsBB = unit
              .get(LinkedBindingBuilder.class);
          namedJobsBB.toInstance(unit.capture(Map.class));
        })
        .expect(onManaged)
        .run(unit -> {
          new Quartz()
              .with(Job.class, (job, trigger) -> {
                trigger.withSchedule(
                    SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMinutes(5)
                    );
              })
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, unit -> {
          assertEquals(1, unit.captured(Map.class).iterator().next().size());
        });
  }

  @Test
  public void config() throws Exception {
    new MockUnit(Config.class)
        .expect(unit -> {
          unit.mockStatic(ConfigFactory.class);

          Config quartzDef = unit.mock(Config.class);

          Config local = unit.mock(Config.class);
          expect(local.withFallback(quartzDef)).andReturn(unit.get(Config.class));

          expect(ConfigFactory.parseResources(Quartz.class, "quartz.conf"))
              .andReturn(local);

          expect(ConfigFactory.parseResources(Job.class, "quartz.properties"))
              .andReturn(quartzDef);
        })
        .run(unit -> {
          assertEquals(unit.get(Config.class), new Quartz(Job.class).config());
        });
  }
}
