/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3659;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.jooby.Reified;
import io.jooby.guice.GuiceModule;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import jakarta.inject.Inject;

public class Issue3659 {

  public interface Animal {}
  ;

  public static class Cat implements Animal {
    @Override
    public String toString() {
      return getClass().getSimpleName();
    }
  }
  ;

  public static class Dog implements Animal {
    @Override
    public String toString() {
      return getClass().getSimpleName();
    }
  }

  public static class AnimalList {
    private final List<Animal> animals;

    @Inject
    public AnimalList(List<Animal> animals) {
      this.animals = animals;
    }
  }

  public static class AnimalSet {
    private final Set<Animal> animals;

    @Inject
    public AnimalSet(Set<Animal> animals) {
      this.animals = animals;
    }
  }

  public static class AnimalMap {
    private final Map<String, Animal> animals;

    @Inject
    public AnimalMap(Map<String, Animal> animals) {
      this.animals = animals;
    }
  }

  @ServerTest
  public void shouldSupportCollectionBinding(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.install(new GuiceModule());
              app.install(
                  application -> {
                    var services = application.getServices();
                    services.listOf(Animal.class).add(new Cat());
                    services.setOf(Animal.class).add(new Cat());
                    services.mapOf(String.class, Animal.class).put("cat", new Cat());
                  });

              app.install(
                  application -> {
                    var services = application.getServices();
                    services.listOf(Animal.class).add(new Dog());
                    services.setOf(Animal.class).add(new Dog());
                    services.mapOf(String.class, Animal.class).put("dog", new Dog());
                  });

              // Registry
              app.get(
                  "/list",
                  ctx -> {
                    return ctx.require(Reified.list(Animal.class)).stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(","));
                  });
              app.get(
                  "/set",
                  ctx -> {
                    return ctx.require(Reified.set(Animal.class)).stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(","));
                  });
              app.get(
                  "/map",
                  ctx -> {
                    return ctx.require(Reified.map(String.class, Animal.class)).entrySet().stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(","));
                  });
              // Guice
              app.get(
                  "/guice/list",
                  ctx -> {
                    return ctx.require(AnimalList.class).animals.stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(","));
                  });
              app.get(
                  "/guice/set",
                  ctx -> {
                    return ctx.require(AnimalSet.class).animals.stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(","));
                  });
              app.get(
                  "/guice/map",
                  ctx -> {
                    return ctx.require(AnimalMap.class).animals.entrySet().stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(","));
                  });
            })
        .ready(
            http -> {
              for (String prefix : new String[] {"", "/guice"}) {
                http.get(
                    prefix + "/list",
                    rsp -> {
                      assertEquals(List.of("Cat", "Dog"), List.of(rsp.body().string().split(",")));
                    });
                http.get(
                    prefix + "/set",
                    rsp -> {
                      assertEquals(Set.of("Cat", "Dog"), Set.of(rsp.body().string().split(",")));
                    });
                http.get(
                    prefix + "/map",
                    rsp -> {
                      assertEquals(
                          Set.of("cat=Cat", "dog=Dog"), Set.of(rsp.body().string().split(",")));
                    });
              }
            });
  }
}
