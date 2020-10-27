package com.cauldronjs.sourceMap;

public class MappingEntry {
    private final SourcePosition generatedSourcePosition;
    private final SourcePosition originalSourcePosition;
    private final String originalName;
    private final String originalFilename;

    public MappingEntry(SourcePosition generatedSourcePosition) {
        this.generatedSourcePosition = generatedSourcePosition;
        this.originalSourcePosition = null;
        this.originalName = null;
        this.originalFilename = null;
    }

    public MappingEntry(SourcePosition generatedSourcePosition, SourcePosition originalSourcePosition, String originalName, String originalFilename) {
        this.generatedSourcePosition = generatedSourcePosition;
        this.originalSourcePosition = originalSourcePosition;
        this.originalName = originalName;
        this.originalFilename = originalFilename;
    }

    public SourcePosition getGeneratedSourcePosition() {
        return generatedSourcePosition;
    }

    public SourcePosition getOriginalSourcePosition() {
        return originalSourcePosition;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public boolean isEqual(MappingEntry other) {
        return this.generatedSourcePosition.compareTo(other.generatedSourcePosition) == 0 &&
                this.originalSourcePosition.compareTo(other.originalSourcePosition) == 0 &&
                this.originalName.equals(other.originalName) &&
                this.originalFilename.equals(other.originalFilename);
    }
}
