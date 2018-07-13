package org.jooby;

import com.google.inject.Binder;
import com.google.inject.ConfigurationException;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class RegistryRequireTest extends ServerFeature {

    class Plumbus {

        private final String message;

        public Plumbus(String message) {
            this.message = message;
        }
        @Override
        public String toString() {
            return message;
        }
    }

    class BagOfHolding implements Jooby.Module {

        private final Plumbus thing;
        private final String name;

        BagOfHolding(String name, Plumbus thing) {
            this.name = name;
            this.thing = thing;
        }

        @Override
        public void configure(Env env, Config conf, Binder binder) {
            binder.bind(Key.get(TypeLiteral.get(Plumbus.class), Names.named(name))).toInstance(thing);
        }
    }

    {
        use(new BagOfHolding("RicksPlumbus", new Plumbus("Property of Rick Sanchez!")));

        get("/gimme", (req, rsp) -> {
            try {
                Plumbus myPlumbus = require(req.param("thing").value(), TypeLiteral.get(Plumbus.class));
                rsp.send(myPlumbus.toString());
            } catch (ConfigurationException e) {
                rsp.status(Status.NOT_FOUND);
                rsp.send("No Rick, no plumbus!");
            }


        });
    }

    @Test
    public void plumbussesShouldBeDependencyInjected() throws Exception {
        request().get("/gimme?thing=RicksPlumbus").expect("Property of Rick Sanchez!");
    }

    @Test
    public void mortysShouldNotReceiveRicksPlumbus() throws Exception {
        request().get("/gimme?thing=MortysPlumbus").expect(Status.NOT_FOUND.value()).expect("No Rick, no plumbus!");
    }
}
