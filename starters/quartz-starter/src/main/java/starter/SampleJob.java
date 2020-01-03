package starter;

import io.jooby.quartz.Scheduled;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

@DisallowConcurrentExecution
public class SampleJob implements Job {
  private Logger log = LoggerFactory.getLogger(getClass());

  @Scheduled("2h; repeat = 3; delay = 2m")
  @Override public void execute(JobExecutionContext context) throws JobExecutionException {
    JobDataMap dataMap = context.getMergedJobDataMap();
    log.info("running {}", new HashMap<>(dataMap));
  }
}
