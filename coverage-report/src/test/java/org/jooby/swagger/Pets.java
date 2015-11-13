package org.jooby.swagger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Singleton;

import org.jooby.Err;
import org.jooby.mvc.Body;
import org.jooby.mvc.GET;
import org.jooby.mvc.POST;
import org.jooby.mvc.Path;

import com.google.common.collect.Lists;

@Path("/pets")
@Singleton
public class Pets {

  private Map<Integer, Pet> pets = new HashMap<>();

  @GET
  @Path("/{id}")
  public Pet get(final int id) {
    Pet pet = pets.get(id);
    if (pet == null) {
      throw new Err(404, "" + id);
    }
    return pet;
  }

  @GET
  public Iterable<Pet> list(final Optional<Integer> size) {
    return Lists.newArrayList(pets.values()).subList(0, size.orElse(pets.values().size()));
  }

  @POST
  public Pet create(@Body final Pet pet) {
    pet.setId(pets.size() + 1);
    pets.put(pet.getId(), pet);
    return pet;
  }

}
