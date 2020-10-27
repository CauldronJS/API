package com.cauldronjs.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import com.cauldronjs.isolate.Isolate;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.HostAccess.Export;

public class NetServer {
  private ServerSocket serverSocket;
  private Isolate isolate;

  public NetServer(Isolate isolate) {
    this.isolate = isolate;
  }

  protected NetServer(int port, int backlog, String address) throws IOException {
    this.serverSocket = new ServerSocket(port, backlog, InetAddress.getByName(address));

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        try {
          NetServer.this.serverSocket.close();
        } catch (IOException ex) {
          System.err.println("Failed to shutdown server: " + ex.getMessage());
        }
      }
    });
  }

  public void setSoTimeout(int ms) throws SocketException {
    this.serverSocket.setSoTimeout(ms);
  }

  public void accept(Value callback) {
    try {
      Socket socket = this.serverSocket.accept();
      callback.execute(null, socket);
    } catch (SocketTimeoutException ex) {
      // ignore
    } catch (IOException ex) {
      callback.execute(ex, null);
    }
  }

  public void listen() {

  }

  public void close() throws IOException {
    this.serverSocket.close();
  }

  public ServerSocket getServer() {
    return this.serverSocket;
  }

  @Export
  public static NetServer createServer(int port, int backlog, String address) throws IOException {
    return new NetServer(port, backlog, address);
  }
}