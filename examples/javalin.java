///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS io.javalin:javalin:4.1.1 org.slf4j:slf4j-simple:1.7.31

import io.javalin.Javalin;

public class javalin {
    public static void main(String args[]) {
        Javalin app = Javalin.create().start(8080);
        app.get("/", ctx -> ctx.redirect("/hello"));
        app.get("/hello", ctx -> ctx.result("Hello Javalin!"));
        app.get("/hello/{name}", ctx -> ctx.result(String.format("Hello %s!", ctx.pathParam("name"))));
    }
}
