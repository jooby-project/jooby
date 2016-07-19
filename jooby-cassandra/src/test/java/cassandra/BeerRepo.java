package cassandra;

import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;

@Accessor
public interface BeerRepo {

  @Query("select * from beer")
  Result<Beer> list();
}
