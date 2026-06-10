package com.lumskyy.lumoaidetector.ml;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

/**
 * ObjectInputStream that only resolves classes from a small allow-list.
 * This constrains Java deserialization so a malicious .bin dropped into the
 * models folder cannot execute arbitrary gadget chains during readObject().
 * Only Smile model classes (relocated under the shaded package), core JDK
 * containers and primitive array types are permitted.
 */
final class SecureObjectInputStream extends ObjectInputStream {
    private static final String[] ALLOWED_PREFIXES = new String[]{
            "smile.",
            "com.lumskyy.lumoaidetector.libs.smile.",
            "java.lang.",
            "java.util.",
            "java.time.",
            "[" // array descriptors, validated further below
    };

    SecureObjectInputStream(InputStream in) throws IOException {
        super(in);
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        String name = desc.getName();
        if (isAllowed(name)) {
            return super.resolveClass(desc);
        }
        throw new InvalidClassException(name, "Class is not allowed for model deserialization");
    }

    private static boolean isAllowed(String name) {
        String base = name;
        while (base.startsWith("[")) {
            base = base.substring(1);
        }
        if (base.length() > 0 && base.charAt(0) == 'L' && base.endsWith(";")) {
            base = base.substring(1, base.length() - 1);
        }
        if (base.length() == 1) {
            // primitive array element type (I, D, Z, ...)
            return true;
        }
        for (String prefix : ALLOWED_PREFIXES) {
            if (prefix.equals("[")) {
                continue;
            }
            if (base.startsWith(prefix) || base.equals(trim(prefix))) {
                return true;
            }
        }
        return false;
    }

    private static String trim(String prefix) {
        return prefix.endsWith(".") ? prefix.substring(0, prefix.length() - 1) : prefix;
    }
}
