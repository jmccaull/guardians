package com.walmart.guardians.basics;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
/**
 * A simple object to associate with Heros on a on to many relationship
 */
public class Planet {
    private final String name;
}
