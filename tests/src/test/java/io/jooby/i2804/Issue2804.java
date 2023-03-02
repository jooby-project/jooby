package io.jooby.i2804;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.jooby.MediaType;
import io.jooby.StatusCode;
import io.jooby.json.JacksonModule;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue2804 {

    public static class Error {
        public String message;
    }

    public static class Errors {
        public List<Error> errors = new ArrayList<>();
    }

    @ServerTest
    public void shouldConsiderRouteProduces(ServerTestRunner runner) {
        runner.define(app -> {

            app.install(new JacksonModule());
            app.get("/pdf", ctx -> {

                if (ctx.header("Authorization").isMissing()){
                    ctx.setResponseCode(StatusCode.UNAUTHORIZED).setResponseType(MediaType.json);
                    final Errors errors = new Errors();
                    final Error error = new Error();
                    error.message = "No Authorization provided";
                    errors.errors.add(error);
                    return errors;
                }

                ctx.setResponseCode(StatusCode.OK).setResponseType(MediaType.byFileExtension("pdf"));
                byte[] dummyData = new byte[1024];
                Arrays.fill(dummyData, (byte) 0x0a);
                return new ByteArrayInputStream(dummyData);

            }).produces(MediaType.byFileExtension("pdf"), MediaType.json);

        }).ready(http -> {
            http.get("/pdf", rsp -> {
                assertEquals(StatusCode.UNAUTHORIZED_CODE, rsp.code());
                assertEquals(MediaType.json.toContentTypeHeader(StandardCharsets.UTF_8).toLowerCase(), rsp.header("Content-Type").toLowerCase());
                assertEquals("{\"errors\":[{\"message\":\"No Authorization provided\"}]}", rsp.body().string());
            });
            http.get("/pdf").prepare(req -> req.addHeader("Authorization", "foo")).execute(rsp -> {
                assertEquals(StatusCode.OK_CODE, rsp.code());
                assertEquals(MediaType.byFileExtension("pdf").getValue(), rsp.header("Content-Type"));
                assertEquals(1024, rsp.body().bytes().length);
            });
        });
    }
}
