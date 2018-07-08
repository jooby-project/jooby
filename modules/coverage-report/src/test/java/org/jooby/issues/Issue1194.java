package org.jooby.issues;

import org.jooby.Results;
import org.jooby.test.JoobySuite;
import org.jooby.test.ServerFeature;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertTrue;

@RunWith(JoobySuite.class)
public class Issue1194 extends ServerFeature {

    {
        get("/async", promise(deferred -> deferred.resolve(Results.ok())));
    }

    @Test
    public void deferredResolveWithEmptyBodyShouldCloseRequest() throws Exception {
        request().get("/async").expect(200).header("Content-Length", "0");
    }
}
