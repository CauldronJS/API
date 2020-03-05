package com.cauldronjs.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.*;

import com.cauldronjs.CauldronAPI;
import com.cauldronjs.Isolate;

public class PathHelpers {
  private static Isolate isolate() {
    return Isolate.activeIsolate();
  }

  public static String join(String path1, String... paths) {
    return Paths.get(path1, paths).toString();
  }

  public static boolean exists(String path1, String... paths) {
    return Files.exists(Paths.get(path1, paths));
  }

  public static boolean exists(File file, String... paths) {
    return exists(file.getAbsolutePath(), paths);
  }

  public static boolean exists(Path path, String... paths) {
    return exists(path.toFile(), paths);
  }

  public static Path resolveLocalPath(String path1, String... paths) {
    return Paths.get(isolate().cwd().toPath().resolve(path1).toString(), paths);
  }

  public static File resolveLocalFile(String path1, String... paths) {
    return resolveLocalPath(path1, paths).toFile();
  }

  public static File resolveLocalFile(File file, String... paths) {
    return Paths.get(isolate().cwd().toPath().resolve(file.getPath()).toString(), paths).toFile();
  }

  public static boolean existsLocal(String path1, String... paths) {
    return resolveLocalFile(path1, paths).exists();
  }

  public static boolean existsLocal(File file, String... paths) {
    return resolveLocalFile(file, paths).exists();
  }

  public static boolean existsEmbedded(String path1, String... paths) {
    return isolate().cauldron().getResource(join(path1, paths)) != null;
  }

  public static BufferedReader readLocal(String path, String... paths) throws FileNotFoundException {
    File localFile = resolveLocalFile(path, paths);
    FileInputStream fis = new FileInputStream(localFile);
    return new BufferedReader(new InputStreamReader(fis));
  }

  public static BufferedReader readEmbedded(String name) {
    InputStream is = isolate().cauldron().getResource(name);
    return new BufferedReader(new InputStreamReader(is));
  }

  public static void tryInitializeCwd(CauldronAPI cauldron) throws IOException {
    File cwd = cauldron.getMainIsolate().cwd();
    if (!cwd.exists()) {
      cwd.mkdirs();
      Path dir = cwd.toPath();
      dir.resolve("src").toFile().mkdir();
      Files.copy(cauldron.getResource("package.json"), dir.resolve("package.json"));
      Files.copy(cauldron.getResource("src/index.js"), dir.resolve("src/index.js"));
    }
  }
}