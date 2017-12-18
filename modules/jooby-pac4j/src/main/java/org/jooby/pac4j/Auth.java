package org.jooby.pac4j;

import com.google.inject.Binder;
import com.google.inject.TypeLiteral;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.Router;
import org.jooby.internal.pac4j.AuthContext;
import org.jooby.internal.pac4j.JoobySessionStore;
import org.jooby.internal.pac4j.ProfileManagerProvider;
import org.jooby.scope.RequestScoped;
import org.pac4j.core.authorization.authorizer.Authorizer;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.IndirectClient;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class Auth implements Jooby.Module {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private Map<String, Authorizer<?>> authorizers = new HashMap<>();

    private List<Function<Config, Client<?, ?>>> clientProviders = new ArrayList<Function<Config, Client<?, ?>>>();

    public Auth authorizer(final String name, final String pattern,
                           final Authorizer<?> authorizer) {
        authorizer(authorizer, name, pattern);
        return this;
    }

    public Auth authorizer(final String name, final String pattern,
                           final Class<? extends Authorizer> authorizer) {
        authorizer(authorizer, name, pattern);
        return this;
    }

    private void authorizer(final Object authorizer, final String name, final String pattern) {
        requireNonNull(name, "An authorizer's name is required.");
        requireNonNull(pattern, "An authorizer's pattern is required.");
        requireNonNull(authorizer, "An authorizer is required.");

        if (authorizer instanceof Authorizer) {
            authorizers.put(name, (Authorizer) authorizer);
        }

        //TODO if authorizer is a class
    }

    public <C extends Credentials, U extends CommonProfile> Auth client(final Client<?, ?> client) {
        return client(config -> client);
    }

    public <C extends Credentials, U extends CommonProfile> Auth client(final Function<Config, Client<?, ?>> provider) {
        clientProviders.add(provider);
        return this;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void configure(final Env env, final Config conf, final Binder binder) {
        List<Client> clients = clientProviders.stream().map(c -> c.apply(conf)).collect(Collectors.toList());
        if (clientProviders.isEmpty()) {
            // no auth client, go dev friendly and add a form auth
        }

        org.pac4j.core.config.Config config = new org.pac4j.core.config.Config(authCallbackPath(conf), clients);

        authorizers.forEach((name, auth) -> config.addAuthorizer(name, auth));

        binder.bind(org.pac4j.core.config.Config.class).toInstance(config);

        binder.bind(new TypeLiteral<SessionStore<AuthContext>>() {
        }).to(JoobySessionStore.class);

        binder.bind(WebContext.class).to(AuthContext.class).in(RequestScoped.class);

        binder.bind(ProfileManager.class).toProvider(ProfileManagerProvider.class).in(RequestScoped.class);

        Router routes = env.router();

        if (clients.stream().anyMatch(c -> c instanceof IndirectClient)) {
            routes.use("*", authCallbackPath(conf),  new CallbackRoute())
                    .name("auth(Callback)");
        }

        //TODO logout
/*
        routes.use("*", logoutUrl.orElse(conf.getString("auth.logout.url")),
                new AuthLogout(redirecTo.orElse(conf.getString("auth.logout.redirectTo"))))
                .name("auth(Logout)");*/

        // bindings.values().forEach(it -> it.accept(binder, config));
    }

    private String authCallbackPath(final Config conf) {
        String fullcallback = conf.getString("auth.callback");
        String root = conf.getString("application.path");
        String callback = URI.create(fullcallback).getPath().replace(root, "");
        log.info(callback);

        return fullcallback;
    }

    @Override
    public Config config() {
        return ConfigFactory.parseResources(getClass(), "auth.conf");
    }

}
