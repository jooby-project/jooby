package examples;

import javax.annotation.Nonnull;
import java.util.List;

public interface PetRepo {

  @Nonnull List<Pet> pets(PetQuery query);

  @Nonnull Pet findById(long id);

  @Nonnull Pet save(@Nonnull Pet pet);

  @Nonnull Pet update(@Nonnull Pet pet);

  void deleteById(long id);
}
