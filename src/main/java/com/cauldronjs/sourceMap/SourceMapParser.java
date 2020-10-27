package com.cauldronjs.sourceMap;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.io.*;

public class SourceMapParser {
    private final Context context;

    public SourceMapParser(Context context) {
        this.context = context;
    }

    public SourceMap parseSourceMap(InputStream stream) throws IOException {
        InputStreamReader streamReader = new InputStreamReader(stream);
        BufferedReader bufferedReader = new BufferedReader(streamReader);
        String line;
        StringBuilder result = new StringBuilder();
        while ((line = bufferedReader.readLine()) != null) {
            result.append(line);
        }
        Value json = this.context.eval("js", "JSON.parse").execute(result.toString());
        return parseSourceMap(json);
    }

    public SourceMap parseSourceMap(Value value) {
        int version = value.getMember("version").asInt();
        String[] sources = value.getMember("sources").as(String[].class);
        String[] names = value.getMember("names").as(String[].class);
        String mappings = value.getMember("mappings").asString();
        String[] sourcesContent = value.getMember("sourcesContent").as(String[].class);
        String file = value.getMember("file").asString();
        MappingEntry[] entries = MappingListParser.parseMappings(mappings, names, sources);
        return new SourceMap(version, sources, names, mappings, sourcesContent, file, entries);
    }
}
