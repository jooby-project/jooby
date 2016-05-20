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
/**
 * This copy of Woodstox XML processor is licensed under the
 * Apache (Software) License, version 2.0 ("the License").
 * See the License for details about distribution rights, and the
 * specific rights regarding derivate works.
 *
 * You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/
 *
 * A copy is also included in the downloadable source code package
 * containing Woodstox, in file "ASL2.0", under the same directory
 * as this file.
 */
package org.jooby.internal;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class LocaleUtils {

  public static List<Locale> parse(final String value) {
    return range(value).stream()
        .map(r -> Locale.forLanguageTag(r.getRange()))
        .collect(Collectors.toList());
  }

  public static Locale parseOne(final String value) {
    return parse(value).get(0);
  }

  public static List<Locale.LanguageRange> range(final String value) {
    // replace ';' by ',' well-formed vs ill-formed
    List<Locale.LanguageRange> range = Locale.LanguageRange.parse(value.replace(';', ','));
    return range.stream()
        .sorted(Comparator.comparing(Locale.LanguageRange::getWeight).reversed())
        .collect(Collectors.toList());
  }

}
