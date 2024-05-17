package io.jooby.avaje.inject;

import io.jooby.avaje.inject.app.TestApp;
import io.jooby.test.JoobyTest;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AvajeInjectModuleTest {

    static OkHttpClient client = new OkHttpClient();

    @JoobyTest(TestApp.class)
    public void shouldPropagateJoobyServicesToAvajeBeanScope(String serverPath) throws IOException {
        Request request = new Request.Builder().url(serverPath + "/ping").build();

        try (Response response = client.newCall(request).execute()) {
            assertEquals("pong", response.body().string());
        }
    }
}
