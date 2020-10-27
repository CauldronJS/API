package com.cauldronjs.sourceMap.base64vlq;

public class Constants {
    public static final int vlqBaseShift = 5;
    public static final int vlqBase = 1 << vlqBaseShift;
    public static final int vlqBaseMask = vlqBase - 1;
    public static final int vlqContinuationBit = vlqBase;
}
