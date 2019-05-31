package com.walmart.guardians.basics;

import graphql.GraphQL;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.TypeRuntimeWiring;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

/**
 * This is a really simple example of constructing a graphql object in java using a SDL file (guardians.graphqls) and a runtime wiring.
 * The example demonstrates that the default "property data fetcher" will be used for the Hero type, in this case the name property, when a property
 * exists and no resolver is registered within the wiring. It further has two examples of simple data fetchers accessing both the source
 * (enclosing type) along with a argument that was passed in the query.
 */
public class GuardiansOfTheGalaxy {

    public static Hero[] HEROES = new Hero[]{new Hero("Thor", Arrays.asList(new Planet("Asgard"))),
        new Hero("Star-lord", Arrays.asList(new Planet("Earth")))};

    public static DataFetcher HERO_FETCHER = (env) ->  {
            String name = env.getArgument("name");
            for(Hero hero : HEROES) {
                if (name.equals(hero.getName())) {
                    return hero;
                }
            }
            return null;
        };

    public static DataFetcher LEADER_FETCHER = (env) ->  {
        Hero selected = env.getSource();
        if (selected.getName().equals("Star-lord")) {
            return true;
        }
        return false;
    };

    public static void main(String[] args) {
        RuntimeWiring wiring = RuntimeWiring.newRuntimeWiring()
            .type(TypeRuntimeWiring.newTypeWiring("QueryType").dataFetcher("getHero", HERO_FETCHER).build())
            .type(TypeRuntimeWiring.newTypeWiring("Hero").dataFetcher("isTheLeader", LEADER_FETCHER).build())
            .build();
        InputStream schemaFile = GuardiansOfTheGalaxy.class.getClassLoader().getResourceAsStream("guardians.graphqls");
        TypeDefinitionRegistry typeDefinitionRegistry = new SchemaParser().parse(new InputStreamReader(schemaFile));
        SchemaGenerator.Options options = SchemaGenerator.Options.defaultOptions().enforceSchemaDirectives(false);
        GraphQLSchema schema = new SchemaGenerator().makeExecutableSchema(options, typeDefinitionRegistry, wiring);
        GraphQL graphQL = GraphQL.newGraphQL(schema).build();
        System.out.println(graphQL.execute("{getHero(name:\"Thor\") {home { name } isTheLeader}}"));
        System.out.println(graphQL.execute("{getHero(name:\"Thor\") {powers}}"));
    }
}
