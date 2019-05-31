package com.walmart.guardians.dataloader;

import com.walmart.guardians.basics.GuardiansOfTheGalaxy;
import com.walmart.guardians.basics.Hero;
import com.walmart.guardians.basics.Planet;
import com.walmart.guardians.basics.Stones;
import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.TypeRuntimeWiring;
import org.dataloader.BatchLoader;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * A slightly more contrived example that attempts to bring to light some of the problems with the data loader implementation. Essentially the
 * Javacript implementation uses an "event thread" to collect dataloader promises and dispatches them at set intervals. This concept (event thread)
 * does not exist in java implementation for a number of good reasons. The lack of a constant method of dispatching means that there is no way for the
 * framework to know when there are outstanding dispatches, causing the user to manually call dispatch on the dataloader object. This manual dispatch
 * increases the number of trips required and reduces the benefit of using the dataloader library in the first place. Further this also means that
 * when trying to run multiple executions (as in a batch of different queries) with the same context/dataloaders, that none of the requests across the
 * executions will be batched together compounding the problem. In javascript the creation of the promises would get batched together as they are
 * created within the time interval, in java the instrumentation forces dispatching as soon as each executions futures are created. Typically the
 * javascript implementation would result in 1/batch size the connections/trips to the source of data, but in Java the number remains constant. There
 * is currently no good solution in java.
 */
public class ChainedLoadersProblem extends GuardiansWithStones {

    public static DataFetcher POWER_FETCHER = (env) ->  {
        Hero source = env.getSource();
        List<Planet> homes = source.getHome();
        DataLoader<String, List<Stones>> stoneLoader = env.getDataLoader("stoneLoader");
        DataLoader<List<Stones>, List<String>> powerLoader = env.getDataLoader("powerLoader");
        return stoneLoader.load(homes.get(0).getName()).thenCompose(powerLoader::load);
//        commenting out the above line and un-commenting the below will make main complete
//        return stoneLoader.load(homes.get(0).getName()).thenCompose(stonesList -> {
//            CompletableFuture<List<String>> powerPromise = powerLoader.load(stonesList);
//            powerLoader.dispatch(); //this is the required manual dispatch
//            return powerPromise;
//        });
        };

    public static BatchLoader<List<Stones>, List<String>> powerLoader = (keys) -> {
        System.out.println(String.format("Batch loader called with keys: %s", keys));
        return CompletableFuture.completedFuture(
            keys.stream().map(ChainedLoadersProblem::getStonePowers).collect(Collectors.toList()));
    };

    private static List<String> getStonePowers(List<Stones> stones) {
        return stones.stream().map(Stones::getPower).collect(Collectors.toList());
    }

    public static void main(String[] args) {
        RuntimeWiring wiring = wiringBuilder
            .type(TypeRuntimeWiring.newTypeWiring("Hero").dataFetcher("isTheLeader", LEADER_FETCHER)
                .dataFetcher("powers", POWER_FETCHER).build()).build();
        InputStream schemaFile = GuardiansOfTheGalaxy.class.getClassLoader().getResourceAsStream("guardians.graphqls");
        TypeDefinitionRegistry typeDefinitionRegistry = new SchemaParser().parse(new InputStreamReader(schemaFile));
        SchemaGenerator.Options options = SchemaGenerator.Options.defaultOptions().enforceSchemaDirectives(false);
        GraphQLSchema schema = new SchemaGenerator().makeExecutableSchema(options, typeDefinitionRegistry, wiring);
        GraphQL graphQL = GraphQL.newGraphQL(schema).build();
        DataLoaderRegistry dataLoaderRegistry = new DataLoaderRegistry();
        dataLoaderRegistry.register("stoneLoader", DataLoader.newDataLoader(stoneLoader));
        dataLoaderRegistry.register("powerLoader", DataLoader.newDataLoader(powerLoader));
        ExecutionInput input = ExecutionInput.newExecutionInput("{A:getHero(name:\"Thor\") {powers}}")
            .dataLoaderRegistry(dataLoaderRegistry).build();
        System.out.println(graphQL.execute(input));
    }
}
