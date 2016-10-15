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
package org.jooby.crash;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.crsh.cli.impl.Delimiter;
import org.crsh.cli.impl.completion.CompletionMatch;
import org.crsh.cli.spi.Completion;
import org.crsh.plugin.PluginContext;
import org.crsh.shell.Shell;
import org.crsh.shell.ShellFactory;
import org.crsh.shell.ShellProcess;
import org.crsh.util.Utils;
import org.jooby.Request;
import org.jooby.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

import javaslang.control.Try;

class WebShellHandler implements WebSocket.FullHandler {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  @SuppressWarnings("rawtypes")
  @Override
  public void connect(final Request req, final WebSocket ws) throws Exception {
    PluginContext ctx = req.require(PluginContext.class);
    ShellFactory factory = ctx.getPlugin(ShellFactory.class);
    Shell shell = factory.create(null);

    AtomicReference<ShellProcess> process = new AtomicReference<ShellProcess>();

    ws.onMessage(msg -> {
      Map event = msg.to(Map.class);
      String type = (String) event.get("type");
      if (type.equals("welcome")) {
        log.debug("sending welcome + prompt");
        ws.send(event("print", shell.getWelcome()));
        ws.send(event("prompt", shell.getPrompt()));
      } else if (type.equals("execute")) {
        String command = (String) event.get("command");
        Integer width = (Integer) event.get("width");
        Integer height = (Integer) event.get("height");
        process.set(shell.createProcess(command));
        SimpleProcessContext context = new SimpleProcessContext(r -> {
          Try.run(() -> {
            // reset process
            process.set(null);

            ws.send(event("print", r.get()));
            ws.send(event("prompt", shell.getPrompt()));
            ws.send(event("end"));
          }).onFailure(x -> log.error("error found while sending output", x));

          if ("bye".equals(command)) {
            ws.close(WebSocket.NORMAL);
          }
        }, width, height);
        log.debug("executing {}", command);
        process.get().execute(context);
      } else if (type.equals("cancel")) {
        ShellProcess p = process.get();
        if (p != null) {
          log.info("cancelling {}", p);
          p.cancel();
        }
      } else if (type.equals("complete")) {
        String prefix = (String) event.get("prefix");
        CompletionMatch completion = shell.complete(prefix);
        Completion completions = completion.getValue();
        Delimiter delimiter = completion.getDelimiter();
        StringBuilder sb = new StringBuilder();
        List<String> values = new ArrayList<String>();
        if (completions.getSize() == 1) {
          String value = completions.getValues().iterator().next();
          delimiter.escape(value, sb);
          if (completions.get(value)) {
            sb.append(delimiter.getValue());
          }
          values.add(sb.toString());
        } else {
          String commonCompletion = Utils.findLongestCommonPrefix(completions.getValues());
          if (commonCompletion.length() > 0) {
            delimiter.escape(commonCompletion, sb);
            values.add(sb.toString());
          } else {
            for (Map.Entry<String, Boolean> entry : completions) {
              delimiter.escape(entry.getKey(), sb);
              values.add(sb.toString());
              sb.setLength(0);
            }
          }
        }
        log.debug("completing {} with {}", prefix, values);
        ws.send(event("complete", values));
      }
    });

    // clean up on close
    ws.onClose(status -> {
      log.info("closing web-socket");
      ShellProcess sp = process.get();
      if (sp != null) {
        sp.cancel();
      }
      shell.close();
    });
  }

  private Object event(final String type, final Object data) {
    return ImmutableMap.of("type", type, "data", data);
  }

  private Object event(final String type) {
    return ImmutableMap.of("type", type);
  }

}
