package starter.domain;

import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

@RegisterRowMapper(User.Mapper.class)
public interface UserRepository {

  @SqlQuery("select * from users where username=:username")
  User findByUsername(String username);
}
