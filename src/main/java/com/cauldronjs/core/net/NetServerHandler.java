package com.cauldronjs.core.net;

import java.io.IOException;
import java.util.Locale;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.nio.protocol.BasicAsyncRequestConsumer;
import org.apache.http.nio.protocol.BasicAsyncResponseProducer;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
import org.apache.http.protocol.HttpContext;
import org.graalvm.polyglot.Value;

public class NetServerHandler implements HttpAsyncRequestHandler<HttpRequest> {
  private NetServer server;

  public NetServerHandler(NetServer server) {
    this.server = server;
  }

  @Override
  public HttpAsyncRequestConsumer<HttpRequest> processRequest(HttpRequest request, HttpContext context)
      throws HttpException, IOException {
    return new BasicAsyncRequestConsumer();
  }

  @Override
  public void handle(HttpRequest data, HttpAsyncExchange httpExchange, HttpContext context)
      throws HttpException, IOException {
    // TODO: call the listen event handler
    final HttpResponse response = httpExchange.getResponse();
    final String method = data.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH);
    final String target = data.getRequestLine().getUri();
    // we need to inform the server we're looking for handlers. From there, the
    // handlers will be passed the request and response objects natively. It is
    // expected that the JS lib will polyfill any methods to work with the objects
    // passed
    Value handler = this.server.getListenHandler();
    if (handler.canExecute()) {
      handler.execute(data, response, context, method, target);
    }
    httpExchange.submitResponse(new BasicAsyncResponseProducer(response));
  }

}