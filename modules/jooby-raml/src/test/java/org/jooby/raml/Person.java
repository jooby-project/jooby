package org.jooby.raml;

import java.util.List;
import java.util.Optional;


@SuppressWarnings("unused")
public class Person {

  private String name;

  private Person parent;

  private List<Person> children;

  private Optional<Integer> age;

}