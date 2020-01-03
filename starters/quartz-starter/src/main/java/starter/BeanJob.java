package starter;

import io.jooby.quartz.Scheduled;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;

import java.util.HashMap;

public class BeanJob {

  @Scheduled("beanJob.cron")
  public void syncState(JobExecutionContext ctx) throws InterruptedException {
    JobDataMap data = ctx.getMergedJobDataMap();
    System.out.println("Data: " + new HashMap<>(data));
  }
}
