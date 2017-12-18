package org.jooby.internal.pac4j;

import org.jooby.Err;
import org.jooby.Response;
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.http.HttpActionAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation.
 *
 * @author lodrantl
 * @since 2.0.0
 */
public class JoobyHttpActionAdapter implements HttpActionAdapter<Object, AuthContext> {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public Object adapt(final int code, final AuthContext context) {
        logger.debug("requires HTTP action: {}", code);
        Response response = context.getJoobyResponse();

        if (code == 401) {
            try {
                response.status(code).send(response.header(HttpConstants.AUTHENTICATE_HEADER));
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
            return null;
        }
        if (code >= 400) {
            throw new Err(code);
        }

        try {
            if (code == HttpConstants.TEMP_REDIRECT) {
                response.redirect(context.getLocation());
            } else if (code == HttpConstants.OK) {
                final String content = context.getRequestContent();
                logger.debug("render: {}", content);
                response.status(code).send(content);
            } else {
                final String message = "Unsupported HTTP action: " + code;
                logger.error(message);
                throw new TechnicalException(message);
            }
        } catch (Throwable throwable) {
            throw new TechnicalException(throwable);
        }
        return null;
    }
}