package org.jooby.quartz;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.sql.DataSource;

import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.utils.DBConnectionManager;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.typesafe.config.Config;

public class QuartzConfigurer {
  private static final TypeLiteral<Provider<DataSource>> DS_TYPE =
      new TypeLiteral<Provider<DataSource>>() {
      };
  public static final String DS = StdSchedulerFactory.PROP_JOB_STORE_PREFIX + ".dataSource";

  @Inject
  public QuartzConfigurer(final Injector injector, final Scheduler scheduler,
      final Config config) throws SchedulerException {

    scheduler.setJobFactory((bundle, sch) -> {
      JobDetail jobDetail = bundle.getJobDetail();
      Class<?> jobClass = jobDetail.getJobClass();

      return (Job) injector.getInstance(jobClass);
    });
    if (config.hasPath(DS)) {
      String name = config.getString(DS);
      // get a provider, bc ds wont be ready yet.
      Provider<DataSource> ds = injector.getInstance(Key.get(DS_TYPE, Names.named(name)));
      DBConnectionManager.getInstance()
          .addConnectionProvider(name, new QuartzConnectionProvider(ds));
    }
  }
}
