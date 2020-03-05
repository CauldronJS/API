package com.cauldronjs.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Paths;
import java.util.logging.Level;

import com.cauldronjs.CauldronAPI;

import org.graalvm.polyglot.HostAccess.Export;

public class FileReader {
  private CauldronAPI cauldron;

  public FileReader(CauldronAPI cauldron) {
    this.cauldron = cauldron;
  }

  @Export
  public File getFile(String location) {
    String localizedName = Paths.get(cauldron.getMainIsolate().cwd().getPath(), location).toString();
    File localFile = new File(localizedName);
    return localFile;
  }

  @Export
  public String read(String location) throws FileNotFoundException, IOException {
    // first read from the disk "lib" dir, then read from resources
    // if neither exist, read from disk
    // god I hate all these variables and I'm THIS close to making a helper file for
    // this shit
    String localizedName = Paths.get(cauldron.getMainIsolate().cwd().getPath(), location).toString();
    BufferedReader reader = null;
    File localFile = new File(localizedName);
    if (localFile.exists()) {
      try {
        reader = new BufferedReader(new InputStreamReader(new FileInputStream(localizedName)));
      } catch (FileNotFoundException ex) {
        // skip because it should be found
      }
    } else if (cauldron.getResource(location) != null) {
      reader = new BufferedReader(new InputStreamReader(cauldron.getResource(location)));
    } else {
      throw new FileNotFoundException(localizedName);
    }

    String result = "";
    String line;
    while ((line = reader.readLine()) != null) {
      result += (line + System.lineSeparator());
    }

    reader.close();

    return result;
  }

  @Export
  public void write(String location, String content, int position, String encoding) {
    File localizedFile = Paths.get(cauldron.getMainIsolate().cwd().getPath(), location).toFile();
    try {
      localizedFile.createNewFile();
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(localizedFile)));
      writer.write(content, position, content.length());
      writer.flush();
      writer.close();
    } catch (Exception ex) {
      cauldron.log(Level.SEVERE, ex.toString());
    }
  }
}