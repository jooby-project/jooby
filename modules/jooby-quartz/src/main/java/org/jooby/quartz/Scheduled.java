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

import org.quartz.Trigger;

/**
 * <p>
 * Define a {@link Trigger Quartz Trigger}. Using one of three expressions:
 * </p>
 * <ul>
 * <li>interval: like<code>5s;delay=60s;repeat=*</code>, <code>delay</code> and <code>repeat</code>
 * are optional
 * <li>cron: like <code>0/3 * * * * ?</code></li>
 * <li>property name: a property defined in <code>.conf</code> file which has one of two previous
 * formats</li>
 * </ul>
 *
 * Examples:
 * <p>
 * Run every 5 minutes, start immediately and repeat for ever:
 * </p>
 *
 * <pre>
 * &#64;Scheduled("5m")
 *
 * &#64;Scheduled("5m; delay=0")
 *
 * &#64;Scheduled("5m; delay=0; repeat=*")
 * </pre>
 *
 * Previous, expressions are identical.
 *
 * <p>
 * Run every 1 hour with an initial delay of 15 minutes for 10 times
 * </p>
 *
 * <pre>
 * &#64;Scheduled("1h; delay=15m; repeat=10")
 * </pre>
 *
 * <p>
 * Fire at 12pm (noon) every day
 * </p>
 *
 * <pre>
 * 0 0 12 * * ?
 * </pre>
 *
 * <p>
 * Fire at 10:15am every day
 * </p>
 *
 * <pre>
 * 0 15 10 ? * *
 * </pre>
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
