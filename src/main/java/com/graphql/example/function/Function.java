package com.graphql.example.function;

import static graphql.ExecutionInput.newExecutionInput;
import static graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentationOptions.newOptions;
import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Optional;

import com.graphql.example.function.util.JsonKit;
import com.graphql.example.function.util.QueryParameters;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import org.dataloader.DataLoaderRegistry;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentation;
import graphql.execution.instrumentation.tracing.TracingInstrumentation;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {
    static GraphQLSchema graphQLSchema = null;
    /**
     * This function listens at endpoint "/api/graphql". Two ways to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/graphql
     * 2. curl {your host}/api/graphql?name=HTTP%20Query
     */
    @FunctionName("graphql")
    public HttpResponseMessage<String> graphql(
            @HttpTrigger(name = "req", methods = {"get", "post"}, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) throws IOException{
        context.getLogger().info("Java HTTP trigger processed a request....");
        //context.getLogger().info("Body="+request.getBody());
         
        // Parse query parameter
        //String query = request.getQueryParameters().get("query");
        //String name = request.getBody().orElse(query);

           
        QueryParameters parameters = QueryParameters.from(request);

        context.getLogger().info("query: " + parameters.getQuery());
        context.getLogger().info("operation: " + parameters.getOperationName());
        context.getLogger().info("variables: " + parameters.getVariables());
        
        if(parameters.getQuery() == null){
            return request.createResponse(400, "Please pass a query on the query string or in the request body");
        }


        ExecutionInput.Builder executionInput = newExecutionInput()
                .query(parameters.getQuery())
                .operationName(parameters.getOperationName())
                .variables(parameters.getVariables());

        StarWarsWiring.Context swcontext = new StarWarsWiring.Context(context);
                executionInput.context(swcontext);    
                
        // This example uses the DataLoader technique to ensure that the most efficient
        // loading of data (in this case StarWars characters) happens.  We pass that to data
        // fetchers via the graphql context object.
        //
        DataLoaderRegistry dataLoaderRegistry = swcontext.getDataLoaderRegistry();


        DataLoaderDispatcherInstrumentation dlInstrumentation =
                new DataLoaderDispatcherInstrumentation(dataLoaderRegistry, newOptions().includeStatistics(true));

        Instrumentation instrumentation = new ChainedInstrumentation(
                asList(new TracingInstrumentation(), dlInstrumentation)
        );                
        
        GraphQL graphql = GraphQL.newGraphQL(buildSchema())
                                    .instrumentation(instrumentation)
                                    .build();

                                    ExecutionResult executionResult = graphql.execute(executionInput);
        
        //return request.createResponse(200, executionResult.getData().toString());
        return request.createResponse(200, JsonKit.toJson(executionResult.toSpecification()));
    }



    private GraphQLSchema buildSchema(){
        if(graphQLSchema == null){
            //String schema = "type Query{hello: String}";
        
            Reader streamReader = loadSchemaFile("schema.graphqls");
            
            TypeDefinitionRegistry typeDefinitionRegistry = new SchemaParser().parse(streamReader);
            /*
            RuntimeWiring runtimeWiring = newRuntimeWiring()
                    .type("Query", builder -> builder.dataFetcher("hello", new StaticDataFetcher("world")))
                    .build();
            */

            RuntimeWiring wiring = RuntimeWiring.newRuntimeWiring()
            .type(newTypeWiring("Query")
                    .dataFetcher("hero", StarWarsWiring.heroDataFetcher)
                    .dataFetcher("human", StarWarsWiring.humanDataFetcher)
                    .dataFetcher("droid", StarWarsWiring.droidDataFetcher)
            )
            .type(newTypeWiring("Human")
                    .dataFetcher("friends", StarWarsWiring.friendsDataFetcher)
            )
            .type(newTypeWiring("Droid")
                    .dataFetcher("friends", StarWarsWiring.friendsDataFetcher)
            )

            .type(newTypeWiring("Character")
                    .typeResolver(StarWarsWiring.characterTypeResolver)
            )
            .type(newTypeWiring("Episode")
                    .enumValues(StarWarsWiring.episodeResolver)
            )
            .build();

            // finally combine the logical schema with the physical runtime
            graphQLSchema = new SchemaGenerator().makeExecutableSchema(typeDefinitionRegistry, wiring);            
        }
        return graphQLSchema;
    }

    private Reader loadSchemaFile(String name) {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(name);
        return new InputStreamReader(stream);
    }
}
