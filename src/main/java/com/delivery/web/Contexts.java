package com.delivery.web;

import io.javalin.http.Context;

/**
 * Small shared helpers for Javalin handlers (DRY).
 */
public final class Contexts {

    private Contexts() {
    }

    /** Reads a path parameter and parses it as a {@code long} entity id. */
    public static long pathId(Context ctx, String name) {
        return Long.parseLong(ctx.pathParam(name));
    }
}