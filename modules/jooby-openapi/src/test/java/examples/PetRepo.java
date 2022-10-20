/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import java.util.List;

import edu.umd.cs.findbugs.annotations.NonNull;

public interface PetRepo {

  @NonNull List<Pet> pets(PetQuery query);

  @NonNull Pet findById(long id);

  @NonNull Pet save(@NonNull Pet pet);

  @NonNull Pet update(@NonNull Pet pet);

  void deleteById(long id);
}
