///usr/bin/env jbang "$0" "$@" ; exit $?

// Turtle demo using the aplu5 library
// JBang script to run a Java program that uses the Turtle graphics library
// Usage: jbang turtle.java
// Requires: Java 25 or higher

//DEPS ch.aplu.turtle:aplu5:0.1.9
//JAVA 25+

import ch.aplu.turtle.Turtle;
import ch.aplu.turtle.TurtleFrame;

// aplu5.jar is not published to Maven Central
//
// To make aplu5.jar available to JBang, install it in
// the local Maven repository by following these steps:
//
//  (1 - Download file) $ wget https://www.java-online.ch/download/aplu5.jar
//
//  (2 - Install file ) $ mvn install:install-file -Dfile=./aplu5.jar -DgroupId=ch.aplu.turtle -DartifactId=aplu5 -Dversion=0.1.9 -Dpackaging=jar
//

Turtle joe = null;

void drawSquare(int sideLength) {
    for (int i = 0; i < 4; i++) {
        joe.forward(sideLength);
        joe.right(90);
    }
}

void drawing() {
    joe.setPenColor("red");
    // repeat 10 times
    for (int i = 0; i < 10; i++) {
        drawSquare(150);
        joe.left(36); // rotate 36 degrees for the next square
    }
}

void main() {
    TurtleFrame frame = new TurtleFrame("Turtle Demo", 500, 500);
    joe = new Turtle(frame);
    drawing();
}