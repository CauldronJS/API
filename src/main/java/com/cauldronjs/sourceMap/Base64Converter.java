package com.cauldronjs.sourceMap;

public class Base64Converter {
    private static final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    public static int fromBase64(char value) {
        return alphabet.indexOf(value);
    }

    public static char toBase64(int value) {
        return alphabet.charAt(value);
    }
}
