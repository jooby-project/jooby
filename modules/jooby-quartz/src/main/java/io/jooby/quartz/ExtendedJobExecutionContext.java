/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.quartz;

import org.quartz.JobExecutionContext;

import io.jooby.Registry;

/** Like {@link JobExecutionContext} plus {@link Registry}. */
public interface ExtendedJobExecutionContext extends JobExecutionContext, Registry {}
