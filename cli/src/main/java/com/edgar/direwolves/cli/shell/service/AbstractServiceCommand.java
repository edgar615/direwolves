package com.edgar.direwolves.cli.shell.service;

import io.vertx.core.cli.annotations.Argument;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.shell.command.AnnotatedCommand;
import io.vertx.ext.shell.command.CommandProcess;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Created by Edgar on 2017/6/20.
 *
 * @author Edgar  Date 2017/6/20
 */
public abstract class AbstractServiceCommand extends AnnotatedCommand{

  private String id;

  @Argument(index = 0, argName = "id")
  @Description("the service id")
  public void setId(String id) {
    this.id = id;
  }

  @Override
  public void process(CommandProcess process) {
    VertxInternal vertx = (VertxInternal) process.vertx();
    vertx.eventBus().<JsonObject>send(address(), new JsonObject().put("id", id), ar -> {
      if (ar.failed()) {
        StringWriter buffer = new StringWriter();
        PrintWriter writer = new PrintWriter(buffer);
        ar.cause().printStackTrace(writer);
        process.write(buffer.toString()).end();
        return;
      }
      JsonObject result = ar.result().body();
      process.write(result.encode())
              .write("\n");
      process.end();
    });
  }

  public abstract String address();
}