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
package org.jooby.sitemap;

import java.util.List;

import org.jooby.Route;

import com.google.common.collect.ImmutableList;

import cz.jiripinkas.jsitemapgenerator.ChangeFreq;
import cz.jiripinkas.jsitemapgenerator.WebPage;

public interface WebPageProvider {

  WebPageProvider SITEMAP = route -> {
    WebPage page = new WebPage();
    page.setName(route.pattern());
    ChangeFreq freq = route.attr("changefreq");
    if (freq != null) {
        page.setChangeFreq(freq);
    }
    Double priority = route.attr("priority");
    if (priority != null) {
      page.setPriority(priority);
    }
    return ImmutableList.of(page);
  };

  List<WebPage> apply(Route.Definition route);
}
