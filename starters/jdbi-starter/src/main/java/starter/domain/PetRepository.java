package starter.domain;

import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

/**
 * Database access for pets.
 */
@RegisterRowMapper(Pet.Mapper.class)
public interface PetRepository {
  /**
   * List pets using start/max limits.
   *
   * @param start Start offset.
   * @param max Max number of rows.
   * @return Available pets.
   */
  @SqlQuery("select * from pets limit :max offset :start")
  List<Pet> list(int start, int max);

  /**
   * Find a pet by ID.
   *
   * @param id Pet ID.
   * @return Pet or null.
   */
  @SqlQuery("select * from pets where id=:id")
  Pet findById(long id);

  /**
   * Insert a pet and returns the generated PK.
   *
   * @param pet Pet to insert.
   * @return Primary key.
   */
  @SqlUpdate("insert into pets(name) values(:name)")
  @GetGeneratedKeys
  long insert(@BindBean Pet pet);

  /**
   * Update a pet and returns true if the pets was updated.
   *
   * @param pet Pet to update.
   * @return True if the pet was updated.
   */
  @SqlUpdate("update pets set name=:name where id=:id")
  boolean update(@BindBean Pet pet);

  /**
   * Delete a pet by ID.
   *
   * @param id Pet ID.
   * @return True if the pet was deleted.
   */
  @SqlUpdate("delete pets where id=:id")
  boolean delete(long id);
}
