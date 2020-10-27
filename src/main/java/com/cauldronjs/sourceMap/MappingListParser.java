package com.cauldronjs.sourceMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class MappingListParser {
    private static class NumericMappingEntry {
        private final Integer generatedLineNumber;
        private final Integer generatedColumnNumber;
        private Integer originalSourceFileIndex;
        private Integer originalLineNumber;
        private Integer originalColumnNumber;
        private Integer originalNameIndex;

        public NumericMappingEntry(int generatedColumnNumber, int generatedLineNumber) {
            this.generatedColumnNumber = generatedColumnNumber;
            this.generatedLineNumber = generatedLineNumber;
        }

        public MappingEntry toMappingEntry(String[] names, String[] sources) {
            SourcePosition generatedSourcePosition = new SourcePosition(this.generatedLineNumber, this.generatedColumnNumber);
            SourcePosition originalSourcePosition = new SourcePosition(this.originalLineNumber, this.originalColumnNumber);
            String name = this.originalNameIndex == null ? null : names[this.originalNameIndex];
            String source = this.originalSourceFileIndex == null ? null : sources[this.originalSourceFileIndex];
            return new MappingEntry(generatedSourcePosition, originalSourcePosition, name, source);
        }
    }

    private static class MappingParserState {
        private Integer currentGeneratedLineNumber;
        private Integer currentGeneratedColumnBase;
        private Integer sourcesListIndexBase;
        private Integer originalSourceStartingLineBase;
        private Integer originalSourceStartingColumnBase;
        private Integer namesListIndexBase;

        public void nextLine(int lineNumber) {
            this.currentGeneratedColumnBase = 0;
            this.currentGeneratedLineNumber = lineNumber;
        }

        public void setState(
                Integer newGeneratedColumnBase,
                Integer newSourcesListIndexBase,
                Integer newOriginalSourceStartingLineBase,
                Integer newOriginalSourceStartingColumnBase,
                Integer newNamesListIndexBase
        ) {
            this.currentGeneratedColumnBase = newGeneratedColumnBase;
            this.sourcesListIndexBase = newSourcesListIndexBase;
            this.originalSourceStartingLineBase = newOriginalSourceStartingLineBase;
            this.originalSourceStartingColumnBase = newOriginalSourceStartingColumnBase;
            this.namesListIndexBase = newNamesListIndexBase;
        }
    }

    private static Integer ifNullThen(Integer value) {
        return value == null || value < 0 ? 0 : value;
    }

    private static NumericMappingEntry parseSingleMappingSegment(Integer[] segmentFields, MappingParserState state) {
        NumericMappingEntry entry = new NumericMappingEntry(state.currentGeneratedColumnBase + segmentFields[0], state.currentGeneratedLineNumber);
        if (segmentFields.length > 1) {
            entry.originalSourceFileIndex = ifNullThen(state.sourcesListIndexBase) + segmentFields[1];
            entry.originalLineNumber = ifNullThen(state.originalSourceStartingLineBase) + segmentFields[2];
            entry.originalColumnNumber = ifNullThen(state.originalSourceStartingColumnBase) + segmentFields[3];
        }
        if (segmentFields.length >= 5) {
            entry.originalNameIndex = ifNullThen(state.namesListIndexBase) + segmentFields[4];
        }
        return entry;
    }

    public static MappingEntry[] parseMappings(String map, String[] names, String[] sources) {
        ArrayList<MappingEntry> entries = new ArrayList<>();
        MappingParserState parserState = new MappingParserState();
        String[] lines = map.split(";");
        for (int i = 0; i < lines.length; i++) {
            parserState.nextLine(i);
            if (lines[i].length() == 0) {
                continue;
            }
            Stream<String> segmentsForLine = Arrays.stream(lines[i].split(",")).filter(segment -> segment.length() > 0);
            segmentsForLine.forEach(segment -> {
                Integer[] decoded = Base64VqlDecoder.decode(segment);
                Logger.getGlobal().log(Level.INFO, segment + ": {0},{1},{2},{3},{4}", decoded);
                NumericMappingEntry mappingEntry = parseSingleMappingSegment(decoded, parserState);
                entries.add(mappingEntry.toMappingEntry(names, sources));
                parserState.setState(
                        mappingEntry.generatedColumnNumber,
                        mappingEntry.originalSourceFileIndex,
                        mappingEntry.originalLineNumber,
                        mappingEntry.originalColumnNumber,
                        mappingEntry.originalNameIndex
                );
            });
        }
        return entries.toArray(new MappingEntry[0]);
    }
}
