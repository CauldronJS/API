package com.cauldronjs.sourceMap;

import com.cauldronjs.sourceMap.base64vlq.Constants;

import java.util.ArrayList;

public class Base64VqlDecoder {
    public static Integer[] decode(String input) {
        ArrayList<Integer> result = new ArrayList<>();
        Base64CharProvider provider = new Base64CharProvider(input);
        while (!provider.isEmpty()) {
            result.add(decodeNextInt(provider));
        }
        return result.toArray(new Integer[0]);
    }

    private static int decodeNextInt(Base64CharProvider provider) {
        int result = 0;
        boolean continuation;
        int shift = 0;
        do {
            char c = provider.getNextChar();
            int digit = Base64Converter.fromBase64(c);
            continuation = (digit & Constants.vlqContinuationBit) != 0;
            digit &= Constants.vlqBaseMask;
            result += digit << shift;
            shift += Constants.vlqBaseShift;
        } while (continuation);
        return fromVqlSigned(result);
    }

    private static int fromVqlSigned(int value) {
        return (int)Integer.toUnsignedLong(value);
    }

    private static class Base64CharProvider {
        private final String backingString;
        private int currentIndex = 0;

        public Base64CharProvider(String s) {
            this.backingString = s;
        }

        public char getNextChar() {
            return this.backingString.charAt(this.currentIndex++);
        }

        public boolean isEmpty() {
            return this.currentIndex >= this.backingString.length();
        }
    }
}
