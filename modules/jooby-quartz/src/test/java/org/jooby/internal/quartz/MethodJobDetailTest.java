package org.jooby.internal.quartz;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.PersistJobDataAfterExecution;

public class MethodJobDetailTest {

  public static class ConcurrentJob {

    public void doWork() {
    }
  }

  @DisallowConcurrentExecution
  @PersistJobDataAfterExecution
  public static class NotConcurrentJob {

    public void doWork() {
    }
  }

  @Test
  public void shouldTreatJobAsConcurrent() throws Exception {
    assertEquals(false,
        new MethodJobDetail(ConcurrentJob.class.getDeclaredMethod("doWork"))
            .isConcurrentExectionDisallowed());
  }

  @Test
  public void shouldTreatJobAsNotPersistent() throws Exception {
    assertEquals(false,
        new MethodJobDetail(ConcurrentJob.class.getDeclaredMethod("doWork"))
            .isPersistJobDataAfterExecution());
  }

  @Test
  public void shouldNOTTreatJobAsConcurrent() throws Exception {
    assertEquals(true,
        new MethodJobDetail(NotConcurrentJob.class.getDeclaredMethod("doWork"))
            .isConcurrentExectionDisallowed());
  }

  @Test
  public void shouldTreatJobAsPersistent() throws Exception {
    assertEquals(true,
        new MethodJobDetail(NotConcurrentJob.class.getDeclaredMethod("doWork"))
            .isPersistJobDataAfterExecution());
  }
}
