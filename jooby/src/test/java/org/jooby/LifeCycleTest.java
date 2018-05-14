package org.jooby;

import org.junit.Test;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;

public class LifeCycleTest {

  static class ShouldNotAllowStaticMethod {
    @PostConstruct
    public static void start() {
    }
  }

  static class ShouldNotAllowPrivateMethod {
    @PreDestroy
    private void destroy() {
    }
  }

  static class ShouldNotAllowMethodWithArguments {
    @PostConstruct
    public void start(final int arg) {
    }
  }

  static class ShouldNotAllowMethodWithReturnType {
    @PostConstruct
    public String start() {
      return null;
    }
  }

  static class ShouldNotWrapRuntimeException {
    @PostConstruct
    public void start() {
      throw new RuntimeException("intetional err");
    }
  }

  static class ShouldWrapNoRuntimeException {
    @PostConstruct
    public void start() throws IOException {
      throw new IOException("intetional err");
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void noStaticMethod() {
    LifeCycle.lifeCycleAnnotation(ShouldNotAllowStaticMethod.class, PostConstruct.class);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldNotAllowPrivateMethod() {
    LifeCycle.lifeCycleAnnotation(ShouldNotAllowPrivateMethod.class, PreDestroy.class);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldNotAllowMethodWithArguments() {
    LifeCycle.lifeCycleAnnotation(ShouldNotAllowMethodWithArguments.class, PostConstruct.class);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldNotAllowMethodWithReturnType() {
    LifeCycle.lifeCycleAnnotation(ShouldNotAllowMethodWithReturnType.class, PostConstruct.class);
  }

  @Test(expected = RuntimeException.class)
  public void shouldNotWrapRuntimeExceptin() throws Throwable {
    LifeCycle.lifeCycleAnnotation(ShouldNotWrapRuntimeException.class, PostConstruct.class)
      .get().accept(new ShouldNotWrapRuntimeException());
    ;
  }

  @Test(expected = IOException.class)
  public void shouldWrapNotWrapException() throws Throwable {
    LifeCycle.lifeCycleAnnotation(ShouldWrapNoRuntimeException.class, PostConstruct.class)
      .get().accept(new ShouldWrapNoRuntimeException());
    ;
  }

}
