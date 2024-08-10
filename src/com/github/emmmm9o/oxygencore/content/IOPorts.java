package com.github.emmmm9o.oxygencore.content;

import com.github.emmmm9o.oxygencore.io.IOPortType;
import com.github.emmmm9o.oxygencore.io.ports.*;

public class IOPorts {
  public static IOPortType singleItemPort, forbiddenPort;

  public static void load() {
    singleItemPort = new SingleItemPortType("single-item-port");
    forbiddenPort = new ForbiddenPortType("forbidden-port");
  }
}
