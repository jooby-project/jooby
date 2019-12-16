package starter;

import io.jooby.Jooby;
import io.jooby.di.GuiceModule;
import io.jooby.hikari.HikariModule;
import io.jooby.jdbi.JdbiModule;
import io.jooby.json.JacksonModule;
import org.jdbi.v3.core.Jdbi;
import starter.billing.BillingModule;
import starter.billing.BillingService;
import starter.billing.PizzaOrder;

public class App extends Jooby {
  {
    install(new JacksonModule());

    install(new HikariModule());

    install(new JdbiModule());

    install(new GuiceModule(new BillingModule()));

    onStarted(() -> {
      require(Jdbi.class).useHandle(handle -> {
        handle.createUpdate(
            "create table pizza (id bigint auto_increment, type varchar(255), count int, credit_card varchar(255))")
            .execute();
      });
    });

    get("/order", ctx -> {
      BillingService billingService = require(BillingService.class);
      PizzaOrder order = ctx.query(PizzaOrder.class);
      return billingService.chargeOrder(order);
    });
  }

  public static void main(String[] args) {
    runApp(args, App::new);
  }
}
