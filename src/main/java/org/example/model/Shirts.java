package org.example.model;

import java.util.Objects;

public class Shirts {

    public enum ShirtSize {
        xsmall,
        small,
        medium,
        large,
        xlarge
    }

    public String getName() {
        return name;
    }

    public ShirtSize getSize() {
        return size;
    }

    private String name;
    private ShirtSize size;

}
