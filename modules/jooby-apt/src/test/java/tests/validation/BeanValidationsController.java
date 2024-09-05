package tests.validation;

import io.jooby.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

public class BeanValidationsController {

    @POST("/validate/query-bean")
    public Bean validateQueryBean(@Valid @QueryParam Bean bean) {
        return bean;
    }

    @POST("/validate/form-bean")
    public Bean validateFormBean(@Valid @FormParam Bean bean) {
        return bean;
    }

    //todo: revive when flash `toNullable` will be fixed
//    @POST("/validate/flash-bean")
//    public Bean validateFlashBean(@Valid @FlashParam Bean bean) {
//        return bean;
//    }

    @POST("/validate/bind-param-bean")
    public Bean validateBindParamBean(@Valid @BindParam Bean bean) {
        return bean;
    }

    @POST("/validate/body-bean")
    public Bean validateBodyBean(@Valid Bean bean) {
        return bean;
    }

    @POST("/validate/list-of-body-beans")
    public List<Bean> validateListOfBodyBeans(@Valid List<Bean> bean) {
        return bean;
    }
}
