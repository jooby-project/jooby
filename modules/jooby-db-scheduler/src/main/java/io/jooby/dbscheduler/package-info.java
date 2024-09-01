/**
 * Db-scheduler module: https://github.com/kagkarlsson/db-scheduler
 *
 * <pre>{@code
 * import io.jooby.dbscheduler.BeanTasks;
 *
 * {
 *     install(new HikariModule());
 *     install(new DbSchedulerModule(BeanTasks.recurring(this, SampleJob.class)));
 * }
 * }</pre>
 *
 * SampleJob.java:
 *
 * <pre>{@code
 * import io.jooby.dbscheduler.Scheduled;
 *
 * public class SampleJob {
 *
 *   @Scheduled("1m")
 *   public void everyMinute() {
 *     ...
 *   }
 * }
 *
 * }</pre>
 *
 * @since 3.2.10
 * @author edgar
 */
@edu.umd.cs.findbugs.annotations.ReturnValuesAreNonnullByDefault
package io.jooby.dbscheduler;
