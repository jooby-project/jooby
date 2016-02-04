package org.jooby.internal.spec;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import org.jooby.Jooby;
import org.jooby.Results;
import org.jooby.spec.RouteProcessor;
import org.jooby.spec.RouteSpec;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.module.SimpleModule;

import apps.DB;
import apps.model.Pet;

public class DocApp extends Jooby {

  {
    /**
     * Everything about your Pets.
     */
    use("/api/pets")
        /**
         * List pets ordered by name.
         *
         * @param start Start offset, useful for paging. Default is <code>0</code>.
         * @param max Max page size, useful for paging. Default is <code>200</code>.
         * @return Pets ordered by name.
         */
        .get(req -> {
          int start = req.param("start").intValue(0);
          int max = req.param("max").intValue(200);
          DB db = req.require(DB.class);
          List<Pet> pets = db.findAll(Pet.class, start, max);
          return pets;
        })
        /**
         * Find pet by ID.
         *
         * @param id Pet ID.
         * @return Returns a single pet
         */
        .get("/:id", req -> {
          int id = req.param("id").intValue();
          DB db = req.require(DB.class);
          Pet pet = db.find(Pet.class, id);
          return pet;
        })
        /**
         * Add a new pet to the store.
         *
         * @param body Pet object that needs to be added to the store.
         * @return Returns a saved pet.
         */
        .post(req -> {
          Pet pet = req.body().to(Pet.class);
          DB db = req.require(DB.class);
          pet = db.save(pet);
          return pet;
        })
        /**
         * Update an existing pet.
         *
         * @param body Pet object that needs to be updated.
         * @return Returns a saved pet.
         */
        .put(req -> {
          Pet pet = req.body().to(Pet.class);
          DB db = req.require(DB.class);
          pet = db.save(pet);
          return pet;
        })
        /**
         * Deletes a pet by ID.
         *
         * @param id Pet ID.
         */
        .delete("/:id", req -> {
          int id = req.param("id").intValue();
          DB db = req.require(DB.class);
          db.delete(Pet.class, id);
          return Results.noContent();
        });
  }

  public static void main(final String[] args) throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    SerializationConfig serializationConfig =
        mapper.getSerializationConfig();
    VisibilityChecker<?> visibilityChecker =
        serializationConfig.getDefaultVisibilityChecker();
    mapper.setVisibility(visibilityChecker
        .withFieldVisibility(Visibility.ANY)
        .withGetterVisibility(Visibility.PUBLIC_ONLY)
        .withSetterVisibility(Visibility.NONE)
        .withCreatorVisibility(Visibility.PROTECTED_AND_PUBLIC)
        );
    List<RouteSpec> specs = new RouteProcessor().process(new DocApp());
    SimpleModule mod = new SimpleModule();
    mod.addSerializer(SerObject.class, new JsonSerializer<SerObject>() {

      @Override
      public void serialize(final SerObject value, final JsonGenerator gen, final SerializerProvider serializers)
          throws IOException, JsonProcessingException {
        gen.writeObject(value.attr);
      }});

    mod.addSerializer(Type.class, new JsonSerializer<Type>() {

      @Override
      public void serialize(final Type value, final JsonGenerator gen, final SerializerProvider serializers)
          throws IOException, JsonProcessingException {
        gen.writeObject(value.getTypeName());
      }});

    mapper.registerModule(mod);
    System.out.println(mapper.writer().withDefaultPrettyPrinter().writeValueAsString(specs));
  }
}
