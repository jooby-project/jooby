package tests.validation;

import io.jooby.annotation.FormParam;
import io.jooby.annotation.POST;
import io.jooby.annotation.QueryParam;
import jakarta.validation.Valid;

public class BeanValidationsController {

    @POST("/validate/query-bean")
    public Bean validateQueryBean(@Valid @QueryParam Bean bean) {
        return bean;
    }

    @POST("/validate/form-bean")
    public Bean validateFormBean(@Valid @FormParam Bean bean) {
        return bean;
    }

    @POST("/validate/body-bean")
    public Bean validateBodyBean(@Valid Bean bean) {
        return bean;
    }

}
