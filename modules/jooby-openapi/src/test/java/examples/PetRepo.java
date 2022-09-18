package examples;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.List;

public interface PetRepo {

  @NonNull List<Pet> pets(PetQuery query);

  @NonNull Pet findById(long id);

  @NonNull Pet save(@NonNull Pet pet);

  @NonNull Pet update(@NonNull Pet pet);

  void deleteById(long id);
}
