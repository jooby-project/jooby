/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.quartz;

import java.util.Date;

import org.quartz.Calendar;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Registry;
import io.jooby.ServiceKey;
import io.jooby.exception.RegistryException;
import io.jooby.quartz.ExtendedJobExecutionContext;

public class ExtendedJobExecutionContextImpl implements ExtendedJobExecutionContext {

  private JobExecutionContext jobExecutionContext;

  private Registry registry;

  public ExtendedJobExecutionContextImpl(
      JobExecutionContext jobExecutionContext, Registry registry) {
    this.jobExecutionContext = jobExecutionContext;
    this.registry = registry;
  }

  @NonNull @Override
  public <T> T require(@NonNull Class<T> type) throws RegistryException {
    return registry.require(type);
  }

  @NonNull @Override
  public <T> T require(@NonNull Class<T> type, @NonNull String name) throws RegistryException {
    return registry.require(type, name);
  }

  @NonNull @Override
  public <T> T require(@NonNull ServiceKey<T> key) throws RegistryException {
    return registry.require(key);
  }

  @Override
  public Scheduler getScheduler() {
    return jobExecutionContext.getScheduler();
  }

  @Override
  public Trigger getTrigger() {
    return jobExecutionContext.getTrigger();
  }

  @Override
  public Calendar getCalendar() {
    return jobExecutionContext.getCalendar();
  }

  @Override
  public boolean isRecovering() {
    return jobExecutionContext.isRecovering();
  }

  @Override
  public TriggerKey getRecoveringTriggerKey() throws IllegalStateException {
    return jobExecutionContext.getRecoveringTriggerKey();
  }

  @Override
  public int getRefireCount() {
    return jobExecutionContext.getRefireCount();
  }

  @Override
  public JobDataMap getMergedJobDataMap() {
    return jobExecutionContext.getMergedJobDataMap();
  }

  @Override
  public JobDetail getJobDetail() {
    return jobExecutionContext.getJobDetail();
  }

  @Override
  public Job getJobInstance() {
    return jobExecutionContext.getJobInstance();
  }

  @Override
  public Date getFireTime() {
    return jobExecutionContext.getFireTime();
  }

  @Override
  public Date getScheduledFireTime() {
    return jobExecutionContext.getScheduledFireTime();
  }

  @Override
  public Date getPreviousFireTime() {
    return jobExecutionContext.getPreviousFireTime();
  }

  @Override
  public Date getNextFireTime() {
    return jobExecutionContext.getNextFireTime();
  }

  @Override
  public String getFireInstanceId() {
    return jobExecutionContext.getFireInstanceId();
  }

  @Override
  public Object getResult() {
    return jobExecutionContext.getResult();
  }

  @Override
  public void setResult(Object result) {
    jobExecutionContext.setResult(result);
  }

  @Override
  public long getJobRunTime() {
    return jobExecutionContext.getJobRunTime();
  }

  @Override
  public void put(Object key, Object value) {
    jobExecutionContext.put(key, value);
  }

  @Override
  public Object get(Object key) {
    return jobExecutionContext.get(key);
  }

  @Override
  public String toString() {
    return jobExecutionContext.toString();
  }
}
