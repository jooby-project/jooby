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
package org.jooby.spec;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * <h1>spec</h1>
 *
 * <p>
 * The spec module allows you to export your API/microservices outside an application.
 * </p>
 *
 * <p>
 * The goal of this module is to define a common way to write APIs and provide you API tools like
 * live doc and testing for <strong>FREE</strong>. By <strong>FREE</strong> we mean:
 * </p>
 *
 * <blockquote>
 * You aren't force to learn any other tool or annotated your code special annotations. All you
 * have to do is: **write your application** following a few/minor suggestions.
 * </blockquote>
 *
 * <p>
 * This module process, collect and compile <strong>routes</strong> from your application. It
 * extracts HTTP method/pattern, parameter, responses, types and doc.
 * </p>
 *
 * <p>
 * You will find here the basis and the necessary documentation to build and expose rich APIs for
 * free, but keep in mind this module isn't intended for direct usage. It is the basis for tools
 * like <a href="http://swagger.io/">Swagger</a> or <a href="http://raml.org">RAML</a>.
 * </p>
 *
 * <h1>api def</h1>
 *
 * <p>
 * The goal of this module is to define a common way to write APIs and provide you API tools like
 * live doc and testing for <strong>FREE</strong>. By <strong>FREE</strong> we mean:
 * </p>
 *
 * <blockquote>
 * You aren't force to learn any other tool or annotated your code special annotations. All you
 * have to do is: **write your application** following a few/minor suggestions.
 * </blockquote>
 *
 * Let's review how to build rich APIs using the <code>spec</code> module via <code>script</code> or
 * <code>mvc</code> programming model:
 *
 * <h2>script API</h2>
 * <pre>
 * {@code
 * {
 *   /{@literal *}{@literal *}
 *    {@literal *} Everything about your Pets.
 *    {@literal *}/
 *   use("/api/pets")
 *      /{@literal *}{@literal *}
 *       * List pets ordered by name.
 *       *
 *       * @param start Start offset, useful for paging. Default is <code>0</code>.
 * * @param max Max page size, useful for paging. Default is <code>200</code>.
 * * @return Pets ordered by name.
 * {@literal *}/
 * .get(req {@literal ->} {
 * int start = req.param("start").intValue(0);
 * int max = req.param("max").intValue(200);
 * DB db = req.require(DB.class);
 * List&lt;Pet&gt; pets = db.findAll(Pet.class, start, max);
 * return pets;
 * })
 * /{@literal *}{@literal *}
 * * Find pet by ID
 * *
 * * @param id Pet ID.
 * * @return Returns <code>200</code> with a single pet or <code>404</code>
 * {@literal *}/
 * .get("/:id",req {@literal ->} {
 * int id=req.param("id").intValue();
 * DB db=req.require(DB.class);
 * Pet pet = db.find(Pet.class,id);
 * return pet;
 * })
 * /{@literal *}{@literal *}
 * * Add a new pet to the store.
 * *
 * * @param body Pet object that needs to be added to the store.
 * * @return Returns a saved pet.
 * {@literal *}/
 * .post(req {@literal ->} {
 * Pet pet=req.body().to(Pet.class);
 * DB db=req.require(DB.class);
 * db.save(pet);
 * return pet;
 * })
 * /{@literal *}{@literal *}
 * * Update an existing pet.
 * *
 * * @param body Pet object that needs to be updated.
 * * @return Returns a saved pet.
 * {@literal *}/
 * .put(req {@literal ->} {
 * Pet pet=req.body().to(Pet.class);
 * DB db=req.require(DB.class);db.save(pet);
 * return pet;
 * })
 * /{@literal *}{@literal *}
 * * Deletes a pet by ID.
 * *
 * * @param id Pet ID.
 * * @return A <code>204</code>
 * {@literal *}/
 * .delete("/:id",req {@literal ->} {
 * int id=req.param("id").intValue();
 * DB db=req.require(DB.class);
 * db.delete(Pet.class,id);
 * return Results.noContent();
 * })
 * .produces("json")
 * .consumes("json");}
 * }</pre>
 *
 * <h2>MVC API</h2>
 *
 * <pre>
 * {@code
 * /{@literal *}{@literal *}
 *  * Everything about your Pets.
 *  {@literal *}/
 *  &#64;Path("/api/pets")
 *  &#64;Consumes("json")
 *  &#64;Produces("json")
 *  public class Pets {
 *
 *    private DB db;
 *
 *    &#64;Inject
 *    public Pets(final DB db) {
 *      this.db = db;
 *    }
 *
 *    /{@literal *}{@literal *}
 *     * List pets ordered by name.
 *     *
 *     * @param start Start offset, useful for paging. Default is <code>0</code>.
 * * @param max Max page size, useful for paging. Default is <code>200</code>.
 * * @return Pets ordered by name.
 * {@literal *}/
 * &#64;GET
 * public List&lt;Pet&gt; list(final Optional<Integer> start, final Optional<Integer> max) {
 * List&lt;Pet&gt; pets = db.findAll(Pet.class, start.orElse(0), max.orElse(200));
 * return pets;
 * }
 *
 * /{@literal *}{@literal *}
 * * Find pet by ID.
 * *
 * * @param id Pet ID.
 * * @return Returns a single pet
 * {@literal *}/
 * &#64;Path("/:id")
 * &#64;GET
 * public Pet get(final int id) {
 * Pet pet = db.find(Pet.class, id);
 * return pet;
 * }
 *
 * /{@literal *}{@literal *}
 * * Add a new pet to the store.
 * *
 * * @param pet Pet object that needs to be added to the store.
 * * @return Returns a saved pet.
 * {@literal *}/
 * &#64;POST
 * public Pet post(@Body final Pet pet) {
 * db.save(pet);
 * return pet;
 * }
 *
 * /{@literal *}{@literal *}
 * * Update an existing pet.
 * *
 * * @param body Pet object that needs to be updated.
 * * @return Returns a saved pet.
 * {@literal *}/
 * &#64;PUT
 * public Pet put(@Body final Pet pet) {
 * db.save(pet);
 * return pet;
 * }
 *
 * /{@literal *}{@literal *}
 * * Deletes a pet by ID.
 * *
 * * @param id Pet ID.
 * {@literal *}/
 * &#64;DELETE
 * public void delete(final int id) {
 * db.delete(Pet.class, id);
 * }
 * }
 * }
 * </pre>
 *
 * <p>
 * Previous examples are feature identical, but they were written in very different way. Still
 * they produces an output likes:
 * </p>
 *
 * <pre>
 * {@code
 * GET /api/pets
 *  summary: Everything about your Pets.
 *  doc: List pets ordered by name.
 *  consumes: [application/json]
 *  produces: [application/json]
 *  params:
 *    start:
 *      paramType: QUERY
 *      type: int
 *      value: 0
 *      doc: Start offset, useful for paging. Default is <code>0</code>.
 *    max:
 *      paramType: QUERY
 *      type: int
 *      value: 200
 *      doc: Max page size, useful for paging. Default is <code>200</code>.
 *  response:
 *    type: java.util.List<apps.model.Pet>
 *    doc: Pets ordered by name.
 * GET /api/pets/:id
 *  summary: Everything about your Pets.
 *  doc: Find pet by ID
 *  consumes: [application/json]
 *  produces: [application/json]
 *  params:
 *    id:
 *      paramType: PATH
 *      type: int
 *      doc: Pet ID.
 *  response:
 *    type: apps.model.Pet
 *    doc: Returns <code>200</code> with a single pet or <code>404</code>
 * POST /api/pets
 *  summary: Everything about your Pets.
 *  doc: Add a new pet to the store.
 *  consumes: [application/json]
 *  produces: [application/json]
 *  params:
 *    body:
 *      paramType: BODY
 *      type: apps.model.Pet
 *      doc: Pet object that needs to be added to the store.
 *  response:
 *    type: apps.model.Pet
 *    doc: Returns a saved pet.
 * PUT /api/pets
 *  summary: Everything about your Pets.
 *  doc: Update an existing pet.
 *  consumes: [application/json]
 *  produces: [application/json]
 *  params:
 *    body:
 *      paramType: BODY
 *      type: apps.model.Pet
 *      doc: Pet object that needs to be updated.
 *  response:
 *    type: apps.model.Pet
 *    doc: Returns a saved pet.
 * DELETE /api/pets/:id
 *  summary: Everything about your Pets.
 *  doc: Deletes a pet by ID.
 *  consumes: [application/json]
 *  produces: [application/json]
 *  params:
 *    id:
 *      paramType: PATH
 *      type: int
 *      doc: Pet ID.
 *  response:
 *    type: void
 *    doc: A <code>204</code>
 * }</pre>
 *
 * <p>
 * <strong>NOTE</strong>: We use <code>text</code> for simplicity and easy read, but keep in mind
 * the output
 * is compiled in binary format.
 * </p>
 *
 *
 * <h2>how it works?</h2>
 *
 * <p>
 * The spec module scan and parse the source code: <code>*.java</code> and produces a list of
 * {@link RouteSpec}.
 * </p>
 * <p>
 * There is a <code>jooby:spec</code> maven plugin for that collects and compiles {@link RouteSpec}
 * at build time, useful for production environments where the source code isn't available.
 * </p>
 *
 * <h3>Why do we parse the source code?</h3>
 *
 * <p>
 * It is required for getting information from <code>script routes</code>. We don't need that for
 * <code>mvc routes</code> because all the information is available via <code>Reflection</code> and
 * {@link Method}.
 * </p>
 *
 * <p>
 * But also, you can write clean and useful JavaDoc in your source code that later are added to the
 * API information.
 * </p>
 *
 * <h3>Why don't parse byte-code with ASM?</h3>
 * <p>
 * Good question, the main reason is that we lost generic type information and we aren't able to
 * tell if the route response is for example a list of pets.
 * </p>
 *
 * <h2>script rules</h2>
 * <p>
 * Have a look at the previous examples again? Do you see anything special? No, right?
 * </p>
 * <p>
 * Well there are some minor things you need to keep in mind for getting or collecting route
 * metadata from <code>script</code> routes:
 * </p>
 *
 * <h3>params</h3>
 * <p>
 * Params need to be in one sentence/statement, like:
 *
 * </p>
 *
 * <pre>
 * req {@literal ->} {
 *   int id = req.param("id").intValue();
 * }
 * </pre>
 *
 * not like:
 *
 * <pre>
 * req {@literal ->} {
 *   Mutant p = req.param("id");
 *   int id = p.intValue();
 * }
 * </pre>
 *
 * <h3>response type (a.k.a return type)</h3>
 *
 * <p>
 * There should be <strong>ONLY one</strong> return statement and return type needs to be declared
 * as variable, like:
 * </p>
 *
 * <pre>
 * req {@literal ->} {
 *   ...
 *   Pet pet = db.find(id); // variable pet
 *   ...
 *   return pet;
 * }
 * </pre>
 *
 * not like:
 *
 * <pre>
 * req {@literal ->} {
 *   ...
 *   return db.find(id); // we aren't able to tell what type returns db.find
 * }
 * </pre>
 *
 * or
 *
 * <pre>
 * req {@literal ->} {
 *   ...
 *   if (...) {
 *     return ...;
 *   } else {
 *     return ...;
 *   }
 * }
 * </pre>
 *
 * <p>
 * There is a workaround if these rules doesn't make sense to you and/or the algorithm fails to
 * resolve the correct type. Please checkout next section.
 * </p>
 *
 * <h2>API doc</h2>
 * <p>
 * If you take a few minutes and write good quality doc the prize will be huge!
 * </p>
 * <p>
 * The tool takes the doc and export it as part of your API!!
 * </p>
 * <p>
 * Here is an example on how to document script routes:
 * </p>
 *
 * <pre>
 *   /{@literal *}{@literal *}
 *    * Everything about your Pets.
 *    {@literal *}/
 *   use("/api/pets")
 *      /{@literal *}{@literal *}
 *       * List pets ordered by name.
 *       *
 *       * @param start Start offset, useful for paging. Default is <code>0</code>.
 * * @param max Max page size, useful for paging. Default is <code>200</code>.
 * * @return Pets ordered by name.
 * {@literal *}/
 * .get(req {@literal ->} {
 * int start = req.param("start").intValue(0);
 * int max = req.param("max").intValue(200);
 * DB db = req.require(DB.class);
 * List&lt;Pet&gt; pets = db.findAll(Pet.class, start, max);
 * return pets;
 * });
 * </pre>
 *
 * The spec for <code>/api/pets</code> will have the following doc:
 *
 * <pre>
 *   params:
 *     start:
 *       paramType: QUERY
 *       type: int
 *       value: 0
 *       doc: Start offset, useful for paging. Default is <code>0</code>.
 * max:
 * paramType: QUERY
 * type: int
 * value: 200
 * doc: Max page size, useful for paging. Default is <code>200</code>.
 * response:
 * type: java.util.List&lt;apps.model.Pet&gt;
 * doc: Pets ordered by name.
 * </pre>
 *
 * <h3>response</h3>
 *
 * <p>
 * With JavaDoc, you can control the default type returned by the route and/or the status codes.
 * For example:
 * </p>
 *
 * <pre>
 *
 *   /{@literal *}{@literal *}
 *    * Find pet by ID.
 *    *
 *    * @param id Pet ID.
 *    * @return Returns a {&#64;link Pet} with <code>200</code> status or <code>404</code>
 * {@literal *}/
 * get(req {@literal ->} {
 * DB db = req.require(DB.class);
 * return db.find(Pet.class, id);
 * });
 * </pre>
 *
 * <p>
 * Here you tell the tool that this route produces a <code>Pet</code>, response looks like:
 * </p>
 *
 * <pre>
 * response:
 *   type: apps.model.Pet
 *    statusCodes:
 *      200: Success
 *      404: Not Found
 *    doc: Returns <code>200</code> with a single pet or <code>404</code>
 * </pre>
 *
 * <p>
 * You can override the default message of the status code with:
 * </p>
 *
 * <pre>
 * &#64;return Returns a <code>{&#64;link Pet}</code> with <code>200 = Success</code> status or
 * <code>404 = Missing</code>
 * </pre>
 *
 * <p>
 * Finally, you can specify the response type via JavaDoc type references:
 * <code>{&#64;link Pet}</code>. This is useful when the tool isn't able to detect the type for
 * you and/or you aren't able to follow the return rules described before.
 * </p>
 *
 * @author edgar
 * @see RouteProcessor
 * @since 0.15.0
 */
public interface RouteSpec extends Serializable {

  /**
   * @return Top level doc (a.k.a summary).
   */
  Optional<String> summary();

  /**
   * @return Route name.
   */
  Optional<String> name();

  /**
   * @return Route method.
   */
  String method();

  /**
   * @return Route pattern.
   */
  String pattern();

  /**
   * @return Route doc.
   */
  Optional<String> doc();

  /**
   * @return List all the types this route can consumes, defaults is: {@code * / *}.
   */
  List<String> consumes();

  /**
   * @return List all the types this route can produces, defaults is: {@code * / *}.
   */
  List<String> produces();

  /**
   * @return List of params or empty list.
   */
  List<RouteParam> params();

  /**
   * @return Route response.
   */
  RouteResponse response();

  /**
   * Additional route attributes.
   *
   * @return Route attributes.
   */
  Map<String, Object> attributes();
}
