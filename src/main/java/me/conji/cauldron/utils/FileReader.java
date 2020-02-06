package me.conji.cauldron.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;

import me.conji.cauldron.CauldronAPI;
import me.conji.cauldron.Isolate;

public class FileReader {
  public static File getFile(CauldronAPI cauldron, String location) {
    String localizedName = Paths.get(cauldron.cwd().getPath(), location).toString();
    File localFile = new File(localizedName);
    return localFile;
  }

  public static String read(CauldronAPI cauldron, String location) throws FileNotFoundException, IOException {
    // first read from the disk "lib" dir, then read from resources
    // if neither exist, read from disk
    // god I hate all these variables and I'm THIS close to making a helper file for
    // this shit
    String localizedName = Paths.get(cauldron.cwd().getPath(), location).toString();
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

  public static void write(CauldronAPI cauldron, String location, String content, int position, String encoding) {
    File localizedFile = Paths.get(cauldron.cwd().getPath(), location).toFile();
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