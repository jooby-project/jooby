/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import io.jooby.annotations.Dispatch;
import io.jooby.annotations.GET;
import io.jooby.annotations.POST;
import io.jooby.annotations.Path;

@Path("/mvc")
public class MvcController {

  @GET
  public String getIt(Context ctx) {
    return ctx.getRequestPath();
  }

  @Dispatch("single")
  @GET("/single")
  public String single() {
    return Thread.currentThread().getName();
  }

  @POST
  public PojoBody post(PojoBody body) {
    return body;
  }
}
