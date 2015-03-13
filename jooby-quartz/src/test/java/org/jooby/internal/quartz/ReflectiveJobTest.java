package org.jooby.internal.quartz;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import org.jooby.MockUnit;
import org.junit.Test;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;

import com.google.inject.Injector;

public class ReflectiveJobTest {

  public interface MyJob {
    void run(JobExecutionContext ctx);
  }

  public interface MyJobWithReturnType {
    Object run();
  }

  @Test(expected = NullPointerException.class)
  public void shouldFailOnNullInjector() {
    new ReflectiveJob(null);
  }

  @Test
  public void shouldExecuteJob() throws Exception {
    new MockUnit(Injector.class, JobExecutionContext.class)
        .expect(unit -> {
          Runnable runnable = unit.mock(Runnable.class);
          runnable.run();

          Injector injector = unit.get(Injector.class);

          expect(injector.getInstance(Runnable.class)).andReturn(runnable);
        })
        .expect(unit -> {
          JobKey key = JobKey.jobKey("Runnable.run", "java.lang");

          JobDetail job = unit.mock(JobDetail.class);
          expect(job.getKey()).andReturn(key);

          JobExecutionContext ctx = unit.get(JobExecutionContext.class);
          expect(ctx.getJobDetail()).andReturn(job);
        })
        .run(unit -> {
          new ReflectiveJob(unit.get(Injector.class)).execute(unit.get(JobExecutionContext.class));
        });
  }

  @Test
  public void shouldExecuteWithArgument() throws Exception {
    new MockUnit(Injector.class, JobExecutionContext.class)
        .expect(unit -> {
          MyJob runnable = unit.mock(MyJob.class);
          runnable.run(unit.get(JobExecutionContext.class));

          Injector injector = unit.get(Injector.class);

          expect(injector.getInstance(MyJob.class)).andReturn(runnable);
        })
        .expect(unit -> {
          JobKey key = JobKey.jobKey("ReflectiveJobTest$" + MyJob.class.getSimpleName()
              + ".run", ReflectiveJob.class.getPackage().getName());

          JobDetail job = unit.mock(JobDetail.class);
          expect(job.getKey()).andReturn(key);

          JobExecutionContext ctx = unit.get(JobExecutionContext.class);
          expect(ctx.getJobDetail()).andReturn(job);
        })
        .run(unit -> {
          new ReflectiveJob(unit.get(Injector.class)).execute(unit.get(JobExecutionContext.class));
        });
  }

  @Test
  public void shouldExecuteAndSaveResult() throws Exception {
    new MockUnit(Injector.class, JobExecutionContext.class)
        .expect(unit -> {
          MyJobWithReturnType runnable = unit.mock(MyJobWithReturnType.class);
          expect(runnable.run()).andReturn("xxx");

          Injector injector = unit.get(Injector.class);

          expect(injector.getInstance(MyJobWithReturnType.class)).andReturn(runnable);
        })
        .expect(
            unit -> {
              JobKey key = JobKey.jobKey(
                  "ReflectiveJobTest$" + MyJobWithReturnType.class.getSimpleName()
                      + ".run", ReflectiveJob.class.getPackage().getName());

              JobDetail job = unit.mock(JobDetail.class);
              expect(job.getKey()).andReturn(key);

              JobExecutionContext ctx = unit.get(JobExecutionContext.class);
              expect(ctx.getJobDetail()).andReturn(job);
              ctx.setResult("xxx");
            })
        .run(unit -> {
          new ReflectiveJob(unit.get(Injector.class)).execute(unit.get(JobExecutionContext.class));
        });
  }

  @Test(expected = JobExecutionException.class)
  public void shouldCatchInvocationExceptionAndRethrowCause() throws Exception {
    new MockUnit(Injector.class, JobExecutionContext.class)
        .expect(unit -> {
          Runnable runnable = unit.mock(Runnable.class);
          runnable.run();
          expectLastCall().andThrow(new RuntimeException("intentional err"));

          Injector injector = unit.get(Injector.class);
          expect(injector.getInstance(Runnable.class)).andReturn(runnable);
        })
        .expect(unit -> {
          JobKey key = JobKey.jobKey("Runnable.run", "java.lang");

          JobDetail job = unit.mock(JobDetail.class);
          expect(job.getKey()).andReturn(key);

          JobExecutionContext ctx = unit.get(JobExecutionContext.class);
          expect(ctx.getJobDetail()).andReturn(job);
        })
        .run(unit -> {
          new ReflectiveJob(unit.get(Injector.class)).execute(unit.get(JobExecutionContext.class));
        });
  }

  @Test(expected = JobExecutionException.class)
  public void shouldCatchExceptionAndRethrow() throws Exception {
    new MockUnit(Injector.class, JobExecutionContext.class)
        .expect(unit -> {
          Injector injector = unit.get(Injector.class);
          expect(injector.getInstance(Runnable.class)).andReturn(null);
        })
        .expect(unit -> {
          JobKey key = JobKey.jobKey("Runnable.run", "java.lang");

          JobDetail job = unit.mock(JobDetail.class);
          expect(job.getKey()).andReturn(key);

          JobExecutionContext ctx = unit.get(JobExecutionContext.class);
          expect(ctx.getJobDetail()).andReturn(job);
        })
        .run(unit -> {
          new ReflectiveJob(unit.get(Injector.class)).execute(unit.get(JobExecutionContext.class));
        });
  }

}
