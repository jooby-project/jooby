/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.quartz;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.quartz.CronTrigger;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;

/**
 * <p>
 * Define a {@link Trigger Quartz Trigger}. There you can put expressions
 * like: <code>5s</code>, <code>15minutes</code>, <code>2hours</code>, etc... or a CRON expression:
 * <code>0/3 * * * * ?</code>.
 * </p>
 * <p>
 * It is also possible to put the name of property:
 * </p>
 *
 * <pre>
 *  public class MyJob {
 *    &#64;Scheduled("job.expr")
 *    public void doWork() {
 *      ...
 *    }
 *  }
 * </pre>
 * <p>
 * And again the property: <code>job.expr</code> must be one of the previously described
 * expressions.
 * </p>
 *
 * Keep in mind the annotation represent a {@link SimpleTrigger} that repeat for ever or
 * {@link CronTrigger}.
 *
 * @author edgar
 * @since 0.5.0
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Scheduled {

  /**
   * Expression can be one of these three options:
   * <ol>
   * <li>Interval: 10s, 10secs, 10minutes, 1h, etc...</li>
   * <li>Cron expression: 0/3 * * * * ?</li>
   * <li>Reference to a property, where the property value is one of the two previous options</li>
   * </ol>
   *
   * @return an expression to create an scheduler.
   * @see com.typesafe.config.Config#getDuration(String, java.util.concurrent.TimeUnit)
   */
  String value();

}
