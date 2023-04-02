/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.quartz;

import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;

public class JobMethodBuilder extends JobBuilder {

  private JobMethodDetail detail;

  public JobMethodBuilder(JobMethodDetail source) {
    this.detail = (JobMethodDetail) source.clone();
  }

  @Override
  public JobBuilder setJobData(JobDataMap newJobDataMap) {
    detail.setJobDataMap(newJobDataMap);
    return this;
  }

  @Override
  public JobBuilder ofType(Class<? extends Job> jobClazz) {
    detail.setJobClass(jobClazz);
    return this;
  }

  @Override
  public JobBuilder withIdentity(JobKey jobKey) {
    detail.setKey(jobKey);
    return this;
  }

  @Override
  public JobBuilder withIdentity(String name) {
    detail.setName(name);
    return this;
  }

  @Override
  public JobBuilder withDescription(String jobDescription) {
    detail.setDescription(jobDescription);
    return this;
  }

  @Override
  public JobBuilder storeDurably(boolean jobDurability) {
    detail.setDurability(jobDurability);
    return this;
  }

  @Override
  public JobBuilder requestRecovery(boolean jobShouldRecover) {
    detail.setRequestsRecovery(jobShouldRecover);
    return this;
  }

  @Override
  public JobDetail build() {
    return detail;
  }
}
