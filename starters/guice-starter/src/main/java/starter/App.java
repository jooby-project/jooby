package starter;

import io.jooby.Jooby;
import io.jooby.di.GuiceModule;
import io.jooby.flyway.FlywayModule;
import io.jooby.hikari.HikariModule;
import io.jooby.jdbi.JdbiModule;
import io.jooby.jdbi.TransactionalRequest;
import io.jooby.json.JacksonModule;
import starter.service.BillingService;
import starter.domain.Order;

public class App extends Jooby {
  {
    /** JSON module: */
    install(new JacksonModule());

    /** DataSource module: */
    install(new HikariModule());

    /** Database migration module: */
    install(new FlywayModule());

    /** Jdbi module: */
    install(new JdbiModule());

    /** Open handle per request: */
    decorator(new TransactionalRequest());

    install(new GuiceModule(new OrderModule()));

    get("/order", ctx -> {
      BillingService billingService = require(BillingService.class);
      Order order = ctx.query(Order.class);
      return billingService.chargeOrder(order);
    });
  }

  public static void main(String[] args) {
    runApp(args, App::new);
  }
}
