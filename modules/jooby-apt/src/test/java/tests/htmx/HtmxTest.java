/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.htmx;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.jooby.apt.ProcessorRunner;

public class HtmxTest {

  @Test
  public void shouldDoBasicHtmx() throws Exception {
    new ProcessorRunner(new BasicUserHx())
        .withHtmxCode(
            source -> {
              assertThat(source)
                  .containsIgnoringWhitespaces(
                      """
                      public Object getUser(io.jooby.Context ctx) throws Exception {
                        var c = this.factory.apply(ctx);
                        var result_ = c.getUser(ctx.path("id").value());
                        return io.jooby.ModelAndView.of("users/profile.hbs", result_);
                      }
                      """)
                  .containsIgnoringWhitespaces(
                      """
                      public Object getUserMap(io.jooby.Context ctx) throws Exception {
                        var c = this.factory.apply(ctx);
                        var result_ = c.getUserMap(ctx.path("id").valueOrNull());
                        return io.jooby.ModelAndView.of("users/profile.hbs", result_);
                      }
                      """)
                  .containsIgnoringWhitespaces(
                      """
                      public Object getUserModelAndView(io.jooby.Context ctx) throws Exception {
                        var c = this.factory.apply(ctx);
                        var result_ = c.getUserModelAndView(ctx.path("id").valueOrNull());
                        return result_;
                      }
                      """)
                  .containsIgnoringWhitespaces(
                      """
                      public Object createUser(io.jooby.Context ctx) throws Exception {
                        var c = this.factory.apply(ctx);
                        var result_ = c.createUser(ctx.body(tests.htmx.UserDto3936.class));
                        ctx.setResponseHeader("HX-Retarget", "#user-table");
                        ctx.setResponseHeader("HX-Reswap", "beforeend");
                        ctx.setResponseHeader("HX-Trigger", "userCreated, updateGraph");
                        var mv_ = new io.jooby.htmx.HtmxModelAndView("users/row.hbs", result_);
                        mv_.addOob("components/notification_toast");
                        mv_.addOob("components/stats_counter");
                        return mv_;
                      }
                      """);
            });
  }

  @Test
  public void shouldInjectContext() throws Exception {
    new ProcessorRunner(new ContextInjectionHx())
        .withHtmxCode(
            source -> {
              assertThat(source)
                  .containsIgnoringWhitespaces(
                      """
                      public Object updateUser(io.jooby.Context ctx) throws Exception {
                        var c = this.factory.apply(ctx);
                        var result_ = c.updateUser(ctx.path("id").valueOrNull(), ctx.body(tests.htmx.UserDto3936.class), new io.jooby.htmx.HtmxContext(ctx));
                        var mv_ = new io.jooby.htmx.HtmxModelAndView("users/profile.hbs", result_);
                        mv_.addOob("components/notification_toast");
                        return mv_;
                      }
                      """);
            });
  }

  @Test
  public void shouldDoDynamicResponse() throws Exception {
    new ProcessorRunner(new DynamicResponseHx())
        .withHtmxCode(
            source -> {
              assertThat(source)
                  .containsIgnoringWhitespaces(
                      """
                       public Object deleteUser(io.jooby.Context ctx) throws Exception {
                        var c = this.factory.apply(ctx);
                        var result_ = c.deleteUser(ctx.path("id").valueOrNull(), ctx);
                        return result_.send(ctx);
                      }
                      """);
            });
  }

  @Test
  public void shouldHandleError() throws Exception {
    new ProcessorRunner(new ErrorBoundaryHx())
        .withHtmxCode(
            source -> {
              assertThat(source)
                  .containsIgnoringWhitespaces(
                      """
                      public Object saveRiskProfile(io.jooby.Context ctx) throws Exception {
                         var c = this.factory.apply(ctx);
                         try {
                           var result_ = c.saveRiskProfile(ctx.path("id").valueOrNull(), ctx.body(tests.htmx.RiskDto3936.class));
                           var mv_ = new io.jooby.htmx.HtmxModelAndView("users/risk_badge.hbs", result_);
                           mv_.addOob("users/risk_form", java.util.Map.of());
                           return mv_;
                         } catch (Exception ex) {
                           var statusCode_ = ctx.getRouter().errorCode(ex);
                           var validationResult_ = ctx.require(io.jooby.validation.ValidationExceptionMapper.class).toResult(statusCode_, ex);
                           if (validationResult_ == null) {
                             throw ex;
                           }
                           ctx.setResponseCode(io.jooby.StatusCode.UNPROCESSABLE_ENTITY);
                           ctx.setResponseHeader("HX-Retarget", "#risk-form-container");
                           java.util.Map<String, Object> errorModel_ = new java.util.HashMap<>();
                           errorModel_.put("validationResult", validationResult_);
                           return io.jooby.ModelAndView.of("users/risk_form", errorModel_);
                         }
                       }
                      """)
                  // Bean validation
                  .containsIgnoringWhitespaces(
                      """
                      public Object saveRiskProfileBeanValidation(io.jooby.Context ctx) throws Exception {
                        var c = this.factory.apply(ctx);
                        try {
                          var result_ = c.saveRiskProfileBeanValidation(ctx.path("id").valueOrNull(), io.jooby.validation.BeanValidator.apply(ctx, ctx.body(tests.htmx.RiskDto3936.class)));
                          var mv_ = new io.jooby.htmx.HtmxModelAndView("users/risk_badge.hbs", result_);
                          mv_.addOob("users/risk_form_top", java.util.Map.of());
                          return mv_;
                        } catch (Exception ex) {
                          var statusCode_ = ctx.getRouter().errorCode(ex);
                          var validationResult_ = ctx.require(io.jooby.validation.ValidationExceptionMapper.class).toResult(statusCode_, ex);
                          if (validationResult_ == null) {
                            throw ex;
                          }
                          ctx.setResponseCode(io.jooby.StatusCode.UNPROCESSABLE_ENTITY);
                          ctx.setResponseHeader("HX-Retarget", "#risk-form-top-container");
                          java.util.Map<String, Object> errorModel_ = new java.util.HashMap<>();
                          errorModel_.put("validationResult", validationResult_);
                          return io.jooby.ModelAndView.of("users/risk_form_top", errorModel_);
                        }
                      }
                      """);
            });
  }

  @Test
  public void shouldClaimModelAndView() throws Exception {
    new ProcessorRunner(new ClaimedRouteHx())
        .withHtmxCode(
            source -> {
              assertThat(source)
                  .containsIgnoringWhitespaces(
                      """
                      public void install(io.jooby.Jooby app) throws Exception {
                        /** See {@link ClaimedRouteHx#index()} */
                        app.get("/", this::index);

                        /** See {@link ClaimedRouteHx#tasks()} */
                        app.get("/tasks", this::tasks);
                      }
                      """);
            });
  }
}
