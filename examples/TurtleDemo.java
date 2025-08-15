
//SOURCES Turtle.java

import java.awt.Color;

public class TurtleDemo
{
    public static void main(String[] args) 
    {
        Turtle joe = new Turtle();
        joe.penColor(Color.RED);
        for(int i = 0; i < 10; i++)
        {
               for (int j = 0; j < 4; j++) {
                joe.forward(150);
                joe.right(90);
               }
               joe.left(36);
        }
        joe.hide();
    }
}