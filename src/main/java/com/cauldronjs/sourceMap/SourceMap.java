package com.cauldronjs.sourceMap;

import java.util.Arrays;
import java.util.stream.Stream;

public class SourceMap {
    private final int version;
    private final String[] sources;
    private final String[] names;
    private final String mappings;
    private final String[] sourcesContent;
    private final String file;
    private final Stream<MappingEntry> parsedMappings;

    public SourceMap(int version, String[] sources, String[] names, String mappings, String[] sourcesContent, String file, MappingEntry[] entries) {
        this.version = version;
        this.sources = sources;
        this.names = names;
        this.mappings = mappings;
        this.sourcesContent = sourcesContent;
        this.file = file;
        this.parsedMappings = Arrays.stream(entries).parallel();
    }

    public int getVersion() {
        return version;
    }

    public String[] getSources() {
        return sources;
    }

    public String[] getNames() {
        return names;
    }

    public String getMappings() {
        return mappings;
    }

    public String[] getSourcesContent() {
        return sourcesContent;
    }

    public String getFile() {
        return file;
    }

    public MappingEntry getEntryFromGeneratedPosition(SourcePosition generatedPosition) {
        Stream<MappingEntry> matchedPositions = this.parsedMappings.filter(parsedMapping ->
                parsedMapping.getGeneratedSourcePosition().isEqualish(generatedPosition)
        );
        return matchedPositions.findFirst().orElse(null);
    }
}
