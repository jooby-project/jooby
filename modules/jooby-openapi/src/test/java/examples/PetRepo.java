/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import java.util.List;

public interface PetRepo {

  List<Pet> pets(PetQuery query);

  Pet findById(long id);

  Pet save(Pet pet);

  Pet update(Pet pet);

  void deleteById(long id);
}
