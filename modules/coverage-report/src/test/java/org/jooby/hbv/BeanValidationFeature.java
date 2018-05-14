package org.jooby.hbv;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;

import org.jooby.Parser;
import org.jooby.hbm.data.Car;
import org.jooby.json.Jackson;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class BeanValidationFeature extends ServerFeature {

  {
    parser(Parser.bean(true));

    use(new Jackson());

    use(new Hbv(Car.class));

    err((req, rsp, err) -> {
      Throwable cause = err.getCause();
      if (cause instanceof ConstraintViolationException) {
        Set<ConstraintViolation<?>> constraints = ((ConstraintViolationException) cause)
            .getConstraintViolations();
        Map<Path, String> errors = constraints.stream()
            .collect(
                Collectors.toMap(
                    ConstraintViolation::getPropertyPath,
                    ConstraintViolation::getMessage
                    )
            );
        TreeMap<Path, String> sortedMap = new TreeMap<>((o1, o2) ->  {
          return o1.toString().compareTo(o2.toString());
        });
        sortedMap.putAll(errors);
        rsp.send(sortedMap);
      }
    });

    get("/validate", req -> {
      Car car = req.params().to(Car.class);
      return car;
    });

  }

  @Test
  public void validate() throws Exception {
    request()
        .get("/validate?manufacturer=&licencePlate=DD-AB-123&seatCount=1")
        .expect(400)
        .expect("{\"manufacturer\":\"may not be empty\",\"seatCount\":\"must be greater than or equal to 2\"}");
  }
}
