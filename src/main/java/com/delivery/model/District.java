package com.delivery.model;

/**
 * Represents city districts.
 */
public enum District {
    CENTRAL,
    SUBURB,
    OUTSKIRTS;

    /**
     * Approximate distances in km between districts (symmetric matrix),
     * used by distance-based delivery cost strategies.
     */
    private static final double[][] DISTANCE_KM = {
            //          CENTRAL  SUBURB  OUTSKIRTS
            {0.0,      5.0,     12.0},   // from CENTRAL
            {5.0,      0.0,     8.0},    // from SUBURB
            {12.0,     8.0,     0.0}     // from OUTSKIRTS
    };

    public boolean isRemote() {
        return this == OUTSKIRTS;
    }

    /**
     * Returns the approximate distance in km from this district to another one.
     */
    public double distanceTo(District other) {
        return DISTANCE_KM[this.ordinal()][other.ordinal()];
    }
}
