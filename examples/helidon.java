///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 21+
//DEPS io.helidon.webserver:helidon-webserver:4.5.4

import io.helidon.webserver.WebServer;

class helidon {

  public static void main(String args[]) {
    WebServer.builder()
      .host("localhost")
      .port(8080)
      .routing(routing -> routing.get("/", (req, res) -> res.send("Hello World!")))
      .build()
      .start();
  }

}
