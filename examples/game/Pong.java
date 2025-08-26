//usr/bin/env jbang "$0" "$@" ; exit $?

// Game developed using Google Gemini AI (2.5 Pro)

//DEPS org.lwjgl:lwjgl:3.3.6
//DEPS org.lwjgl:lwjgl-glfw:3.3.6
//DEPS org.lwjgl:lwjgl-opengl:3.3.6

//DEPS org.lwjgl:lwjgl:3.3.6:natives-windows
//DEPS org.lwjgl:lwjgl-glfw:3.3.6:natives-windows
//DEPS org.lwjgl:lwjgl-opengl:3.3.6:natives-windows

//DEPS org.lwjgl:lwjgl:3.3.6:natives-windows-arm64
//DEPS org.lwjgl:lwjgl-glfw:3.3.6:natives-windows-arm64
//DEPS org.lwjgl:lwjgl-opengl:3.3.6:natives-windows-arm64

//DEPS org.lwjgl:lwjgl:3.3.6:natives-linux
//DEPS org.lwjgl:lwjgl-glfw:3.3.6:natives-linux
//DEPS org.lwjgl:lwjgl-opengl:3.3.6:natives-linux

//DEPS org.lwjgl:lwjgl:3.3.6:natives-linux-arm64
//DEPS org.lwjgl:lwjgl-glfw:3.3.6:natives-linux-arm64
//DEPS org.lwjgl:lwjgl-opengl:3.3.6:natives-linux-arm64

//DEPS org.lwjgl:lwjgl:3.3.6:natives-macos
//DEPS org.lwjgl:lwjgl-glfw:3.3.6:natives-macos
//DEPS org.lwjgl:lwjgl-opengl:3.3.6:natives-macos

//DEPS org.lwjgl:lwjgl:3.3.6:natives-macos-arm64
//DEPS org.lwjgl:lwjgl-glfw:3.3.6:natives-macos-arm64
//DEPS org.lwjgl:lwjgl-opengl:3.3.6:natives-macos-arm64

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * A simple Pong game using LWJGL 3 and runnable with JBang.
 *
 * How to Run:
 * 1. Install JBang (https://www.jbang.dev/download/)
 * 2. Save this file as Pong.java
 * 3. Open a terminal and run: jbang Pong.java
 *
 * Controls:
 * - Player 1 (Left Paddle): W (Up), S (Down)
 * - Player 2 (Right Paddle): Up Arrow (Up), Down Arrow (Down)
 * - Close the window or press ESC to exit.
 */
public class Pong {

    // The window handle
    private long window;

    // Window dimensions
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final String TITLE = "JBang LWJGL Pong";

    // Game object properties
    private static final float PADDLE_WIDTH = 10;
    private static final float PADDLE_HEIGHT = 100;
    private static final float BALL_SIZE = 10;
    private static final float PADDLE_SPEED = 5f;
    private static final float INITIAL_BALL_SPEED = 4f;

    // Game state variables
    private float player1Y, player2Y;
    private float ballX, ballY;
    private float ballVelX, ballVelY;
    private int score1, score2;

    public void run() {
        init();
        loop();

        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    private void init() {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Configure GLFW
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

        // Create the window
        window = glfwCreateWindow(WIDTH, HEIGHT, TITLE, NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
            }
        });

        // Get the thread stack and push a new frame
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight);

            // Get the resolution of the primary monitor
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // Center the window
            glfwSetWindowPos(
                window,
                (vidmode.width() - pWidth.get(0)) / 2,
                (vidmode.height() - pHeight.get(0)) / 2
            );
        } // The stack frame is popped automatically

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        // Enable v-sync
        glfwSwapInterval(1);

        // Make the window visible
        glfwShowWindow(window);

        // Initialize game state
        resetGame();
    }

    private void resetGame() {
        player1Y = (HEIGHT - PADDLE_HEIGHT) / 2;
        player2Y = (HEIGHT - PADDLE_HEIGHT) / 2;
        resetBall();
    }
    
    private void resetBall() {
        ballX = (WIDTH - BALL_SIZE) / 2;
        ballY = (HEIGHT - BALL_SIZE) / 2;
        
        // Give the ball a random initial direction
        ballVelX = (Math.random() > 0.5) ? INITIAL_BALL_SPEED : -INITIAL_BALL_SPEED;
        ballVelY = (Math.random() > 0.5) ? INITIAL_BALL_SPEED : -INITIAL_BALL_SPEED;
    }


    private void loop() {
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities();

        // Set up a 2D projection
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, WIDTH, HEIGHT, 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);

        // Set the clear color
        glClearColor(0.1f, 0.1f, 0.1f, 0.0f);

        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while (!glfwWindowShouldClose(window)) {
            // Clear the framebuffer
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            handleInput();
            update();
            render();

            // swap the color buffers
            glfwSwapBuffers(window);

            // Poll for window events. The key callback above will only be
            // invoked during this call.
            glfwPollEvents();

            // Sleep to limit the frame rate
            // This is a simple way to control the frame rate without busy-waiting
            // In a real game, you might want to use a more sophisticated timing mechanism
            try {Thread.sleep(16);} catch (InterruptedException e) {}

        }
    }

    private void handleInput() {
        // Player 1 controls (W, S)
        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) {
            player1Y -= PADDLE_SPEED;
        }
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) {
            player1Y += PADDLE_SPEED;
        }

        // Player 2 controls (Up, Down arrows)
        if (glfwGetKey(window, GLFW_KEY_UP) == GLFW_PRESS) {
            player2Y -= PADDLE_SPEED;
        }
        if (glfwGetKey(window, GLFW_KEY_DOWN) == GLFW_PRESS) {
            player2Y += PADDLE_SPEED;
        }

        // Clamp paddles to screen bounds
        if (player1Y < 0) player1Y = 0;
        if (player1Y > HEIGHT - PADDLE_HEIGHT) player1Y = HEIGHT - PADDLE_HEIGHT;
        if (player2Y < 0) player2Y = 0;
        if (player2Y > HEIGHT - PADDLE_HEIGHT) player2Y = HEIGHT - PADDLE_HEIGHT;
    }

    private void update() {
        // Move ball
        ballX += ballVelX;
        ballY += ballVelY;

        // Ball collision with top/bottom walls
        if (ballY <= 0 || ballY >= HEIGHT - BALL_SIZE) {
            ballVelY = -ballVelY;
        }

        // Ball collision with paddles
        // Player 1 (left)
        if (ballX <= PADDLE_WIDTH && ballY + BALL_SIZE >= player1Y && ballY <= player1Y + PADDLE_HEIGHT) {
            ballVelX = Math.abs(ballVelX); // Ensure it moves right
        }
        // Player 2 (right)
        if (ballX >= WIDTH - PADDLE_WIDTH - BALL_SIZE && ballY + BALL_SIZE >= player2Y && ballY <= player2Y + PADDLE_HEIGHT) {
            ballVelX = -Math.abs(ballVelX); // Ensure it moves left
        }
        
        // Scoring
        if (ballX < 0) {
            score2++;
            System.out.println("Score: " + score1 + " - " + score2);
            resetBall();
        }
        if (ballX > WIDTH) {
            score1++;
            System.out.println("Score: " + score1 + " - " + score2);
            resetBall();
        }
    }

    private void render() {
        // Set color to white
        glColor3f(1.0f, 1.0f, 1.0f);

        // Draw Player 1 paddle
        drawRect(0, player1Y, PADDLE_WIDTH, PADDLE_HEIGHT);

        // Draw Player 2 paddle
        drawRect(WIDTH - PADDLE_WIDTH, player2Y, PADDLE_WIDTH, PADDLE_HEIGHT);

        // Draw the ball
        drawRect(ballX, ballY, BALL_SIZE, BALL_SIZE);

        // Draw a dotted center line (optional)
        for (int i = 0; i < HEIGHT; i += 20) {
            drawRect(WIDTH / 2 - 1, i, 2, 10);
        }
    }

    private void drawRect(float x, float y, float width, float height) {
        glBegin(GL_QUADS);
        glVertex2f(x, y);
        glVertex2f(x + width, y);
        glVertex2f(x + width, y + height);
        glVertex2f(x, y + height);
        glEnd();
    }


    public static void main(String[] args) {
        new Pong().run();
    }
}