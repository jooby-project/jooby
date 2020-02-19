package examples;

import java.util.List;

public interface PetRepo {

  List<Pet> pets();

  Pet findById(long id);

  Pet save(Pet pet);

  Pet update(Pet pet);

  void deleteById(long id);
}
