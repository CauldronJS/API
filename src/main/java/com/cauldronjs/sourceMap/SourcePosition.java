package com.cauldronjs.sourceMap;

public class SourcePosition {
    private final Integer lineNumber;
    private final Integer columnNumber;

    public SourcePosition(Integer lineNumber, Integer columnNumber) {
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    public Integer getColumnNumber() {
        return columnNumber;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public boolean isEqualish(SourcePosition other) {
        if (this.lineNumber.equals(other.lineNumber) && Math.abs(this.columnNumber - other.columnNumber) <= 1) {
            return true;
        }
        if (Math.abs(this.lineNumber - other.lineNumber) == 1) {
            SourcePosition largerLineNumber = this.lineNumber > other.lineNumber ? this : other;
            if (largerLineNumber.columnNumber == 0) {
                return true;
            }
        }
        return false;
    }

    public int compareTo(SourcePosition other) {
        return this.lineNumber == other.lineNumber ?
                Integer.compare(this.columnNumber, other.columnNumber) :
                Integer.compare(this.lineNumber, other.lineNumber);
    }
}
