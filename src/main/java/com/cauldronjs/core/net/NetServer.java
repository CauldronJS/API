package com.cauldronjs.core.net;

import org.apache.http.impl.nio.bootstrap.HttpServer;
import org.apache.http.impl.nio.bootstrap.ServerBootstrap;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.cauldronjs.CauldronAPI;
import com.cauldronjs.Isolate;
import com.cauldronjs.api.Thenable;
import com.cauldronjs.exceptions.JsException;

import org.graalvm.polyglot.Value;

public class NetServer {
  private static Isolate isolate;
  private HttpServer server;
  private Value listenHandler;

  public NetServer(Isolate isolate) {
    super();
    if (NetServer.isolate == null) {
      NetServer.isolate = isolate;
    }
  }

  public NetServer(int port, int backlog, String addr) {
    this.server = ServerBootstrap.bootstrap().setListenerPort(port).setServerInfo("CauldronJS/1.1")
        .registerHandler("*", new NetServerHandler(this)).create();
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        NetServer.this.server.shutdown(5, TimeUnit.SECONDS);
      }
    });
  }

  public void listen() throws IOException {
    this.server.start();
  }

  public void close() throws IOException {
    this.server.shutdown(5, TimeUnit.SECONDS);
  }

  public HttpServer getServer() {
    return this.server;
  }

  public void setListenHandler(Value handler) {
    this.listenHandler = handler;
  }

  public Value getListenHandler() {
    return this.listenHandler;
  }

  public NetServer createServer(int port, int backlog, String addr) {
    return new NetServer(port, backlog, addr);
  }
}