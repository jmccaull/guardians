package com.walmart.guardians.basics;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
@RequiredArgsConstructor
/**
 * A simple hero class to be the return object of our hero query.
 */
public class Hero {
    private final String name;
    private final List<Planet> home;
}
