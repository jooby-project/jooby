package starter;

import graphql.schema.idl.RuntimeWiring;
import io.jooby.Jooby;
import io.jooby.graphql.GraphQLModule;
import io.jooby.graphql.GraphQLPlaygroundModule;
import io.jooby.graphql.GraphiQLModule;
import io.jooby.json.JacksonModule;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

public class App extends Jooby {
  {
    install(new JacksonModule());

    GraphQLDataFetchers graphQLDataFetchers = new GraphQLDataFetchers();
    install(new GraphQLModule(
        RuntimeWiring.newRuntimeWiring()
            .type(newTypeWiring("Query")
                .dataFetcher("bookById", graphQLDataFetchers.getBookByIdDataFetcher()))
            .type(newTypeWiring("Book")
                .dataFetcher("author", graphQLDataFetchers.getAuthorDataFetcher()))
            .build())
    );

    /** Choose between GraphiQL or GraphQLPlayGround: */
    // install(new GraphiQLModule());
    install(new GraphQLPlaygroundModule());
  }

  public static void main(String[] args) {
    runApp(args, App::new);
  }
}
