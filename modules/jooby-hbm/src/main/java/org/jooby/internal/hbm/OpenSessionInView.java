/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.internal.hbm;

import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Route.Chain;
import org.jooby.Route.Filter;
import org.jooby.hbm.UnitOfWork;

public class OpenSessionInView implements Filter {

  @Override
  public void handle(final Request req, final Response rsp, final Chain chain) throws Throwable {
    RootUnitOfWork uow = (RootUnitOfWork) req.require(UnitOfWork.class);
    // start transaction
    uow.begin();

    rsp.after(after(uow));

    rsp.complete(complete(uow));

    // move next
    chain.next(req, rsp);
  }

  private static Route.Complete complete(final RootUnitOfWork uow) {
    return (req, rsp, x) -> {
      x.ifPresent(e -> uow.setRollbackOnly());
      uow.end();
    };
  }

  private static Route.After after(final RootUnitOfWork uow) {
    return (req1, rsp1, result) -> {
      // commit transaction and start readonly transaction
      uow.commit();

      uow.setReadOnly();
      uow.begin();
      return result;
    };
  }

}
