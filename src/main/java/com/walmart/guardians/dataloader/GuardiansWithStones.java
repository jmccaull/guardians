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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * This example builds off GuardiansOfTheGalaxy but utilizes the Dataloader/BatchLoader library developed by Facebook. The example is much the same
 * with the exception that the DataFetcher "stoneLoader" returns a future for the desired value. The stoneLoader collects keys until it is dispatched,
 * in this case two, and avoids making multiple trips to the source of truth by both batching and not duplicating repeated requests. The Dataloader
 * library includes a pluggable cache to avoid repeated requests within its lifetime (hence why they need to be request scoped). The framework itself
 * knows to dispatch the dataloaders using an instrumentation. The instrumentation will dispatch the dataloaders if it detects any provided and when
 * each set of fields in the current level of the hierarchy has resolved to either a value or a future.
 */
public class GuardiansWithStones extends GuardiansOfTheGalaxy {

    private static List<Stones> stonesOnPlanet(String planetName) {
        if (planetName.equals("Earth")) {
            return Arrays.asList(Stones.TIME);
        } else if (planetName.equals("Asgard")) {
            return Arrays.asList(Stones.SPACE);
        } else {
            return null;
        }
    }

    public static BatchLoader<String, List<Stones>> stoneLoader = (keys) -> {
        System.out.println(String.format("Batch loader called with keys: %s", keys));
        return CompletableFuture.completedFuture(
            keys.stream().map(GuardiansWithStones::stonesOnPlanet).collect(Collectors.toList()));
    };

    public static DataFetcher STONE_FETCHER_BATCH = (env) ->  {
        Planet source = env.getSource();
        System.out.println("Calling load for planet " + source.getName());
        return env.getDataLoader("stoneLoader").load(source.getName());
    };

    public static RuntimeWiring.Builder wiringBuilder = RuntimeWiring.newRuntimeWiring()
        .type(TypeRuntimeWiring.newTypeWiring("QueryType").dataFetcher("getHero", HERO_FETCHER).build())
        .type(TypeRuntimeWiring.newTypeWiring("Hero").dataFetcher("isTheLeader", LEADER_FETCHER).build())
        .type(TypeRuntimeWiring.newTypeWiring("Planet").dataFetcher("stones", STONE_FETCHER_BATCH).build());

    public static void main(String[] args) {
        HEROES[0] = new Hero("Thor", Arrays.asList(new Planet("Asgard"), new Planet("Earth")));
        RuntimeWiring wiring = wiringBuilder.build();
        InputStream schemaFile = GuardiansOfTheGalaxy.class.getClassLoader().getResourceAsStream("guardians.graphqls");
        TypeDefinitionRegistry typeDefinitionRegistry = new SchemaParser().parse(new InputStreamReader(schemaFile));
        SchemaGenerator.Options options = SchemaGenerator.Options.defaultOptions().enforceSchemaDirectives(false);
        GraphQLSchema schema = new SchemaGenerator().makeExecutableSchema(options, typeDefinitionRegistry, wiring);
        GraphQL graphQL = GraphQL.newGraphQL(schema).build();
        DataLoaderRegistry dataLoaderRegistry = new DataLoaderRegistry();
        dataLoaderRegistry.register("stoneLoader", DataLoader.newDataLoader(stoneLoader));
        ExecutionInput input = ExecutionInput.newExecutionInput("{A:getHero(name:\"Thor\") {home { stones } isTheLeader}}")
            .dataLoaderRegistry(dataLoaderRegistry).build();
        System.out.println(graphQL.execute(input));
    }
}
