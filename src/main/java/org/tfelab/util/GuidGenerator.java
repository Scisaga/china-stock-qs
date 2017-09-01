package org.tfelab.util;

import java.util.UUID;

/**
 *
 */
public abstract class GuidGenerator {

    public static String generate() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }
}