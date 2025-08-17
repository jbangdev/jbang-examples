/**AUTHOR: NICHOLAS SEWARD
 * EMAIL: nicholas.seward@gmail.com
 * LICENSE: MIT (USE THIS HOWEVER YOU SEE FIT.)
 * DATE: 6/21/2012
 * VERSION: 2 (THIS SHOULD BE VERSION 0.1 BUT THAT IS A SILLY NUMBERING SYSTEM.)
 *
 *
 * THIS SOFTWARE HAS NO WARRANTY.  IF IT WORKS, SUPER.  IF IT DOESN'T, LET ME
 * KNOW AND I MIGHT OR MIGHT NOT DO SOMETHING ABOUT IT.
 *
 *    .-./*)   .-./*)   .-./*)   .-./*)   .-./*)   .-./*)   .-./*)   .-./*)
 *  _/___\/  _/___\/  _/___\/  _/___\/  _/___\/  _/___\/  _/___\/  _/___\/
 *    U U      U U      U U      U U      U U      U U      U U      U U
 */

import java.awt.event.*;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.lang.reflect.Method;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Turtle is a selfcontained class that will allow students to make
 * beautiful turtle graphics with ease.
 *
 * @author Nicholas Seward
 */
@SuppressWarnings("unchecked")
public class Turtle implements Runnable, ActionListener, MouseListener, MouseMotionListener, KeyListener, ComponentListener, MouseWheelListener
{

    private static ArrayList<Turtle> turtles;
    private static TreeMap<Long,ArrayList> turtleStates;
    private static TreeMap<Long,ArrayList> redoStates;
    private static JFrame window;
    private static JApplet applet;
    private static JLabel draw;
    private static int width, height;
    private static BufferedImage offscreenImage, midscreenImage, onscreenImage;
    private static Graphics2D offscreen, midscreen, onscreen;
    private static BufferedImage backgroundImage;
    private static Color backgroundColor;
    private static ImageIcon icon;
    private static Turtle turtle;
    private static HashMap<String,Polygon> shapes;  //You can only add.  Never remove.
    private static HashMap<String, Color> colors;
    private static HashMap<String,ArrayList<ArrayList>> keyBindings;
    private static HashMap<Turtle,ArrayList<ArrayList>> mouseBindings;
    private static double centerX, centerY;
    private static double scale;
    private static TreeSet<String> keysDown;
    private static TreeSet<String> processedKeys;
    private static TreeSet<String> unprocessedKeys;
    private static long lastUpdate;
    private static long fps;
    private static final Object turtleLock=new Object();
    private static int dragx=0,dragy=0,x=0,y=0,modifiers=0;
    private static final Object keyLock = new Object();
    private static final int REFRESH_MODE_ANIMATED=0;
    private static final int REFRESH_MODE_STATE_CHANGE=1;
    private static final int REFRESH_MODE_ON_DEMAND=2;
    private static int refreshMode;
    private static final int BACKGROUND_MODE_STRETCH=0;
    private static final int BACKGROUND_MODE_CENTER=1;
    private static final int BACKGROUND_MODE_TILE=2;
    private static final int BACKGROUND_MODE_CENTER_RELATIVE=3;
    private static final int BACKGROUND_MODE_TILE_RELATIVE=4;
    private static int backgroundMode;
    private static Turtle selectedTurtle;
    private static boolean running;




    static { init(); }

    /**
     * This is an internal method that should never be called.
     */
    public void run()
    {
        if(Thread.currentThread().getName().equals("render")) renderLoop();
        else if(Thread.currentThread().getName().equals("listen")) eventLoop();
    }

    private void eventLoop()
    {
        //System.out.println("EVENT LOOP STARTED");
        long time=0;
        while(running)
        {
            time=System.nanoTime();
            processKeys();
            waitUntil(time+1000000000/fps);
        }
    }

    private void renderLoop()
    {
        //System.out.println("RENDER LOOP STARTED");
        long time=0;
        while(running)
        {
            time=System.nanoTime();
            //System.out.println(time);
            if(refreshMode==REFRESH_MODE_ANIMATED) draw();
            if (!waitUntil(time+1000000000/fps)) fps--;
            else if (fps<30) fps++;
        }
    }

    private static boolean waitUntil(Long time)
    {
        long now=System.nanoTime();
        if (now<time)
        {
            try {Thread.sleep((time - now) / 1000000);}
            catch(Exception e) {}
            return true;
        }
        else return false;
    }

    private static void init()
    {
        //mouseBindings.put(null, new ArrayList<ArrayList>());
        turtles=new ArrayList<Turtle>();
        turtleStates=new TreeMap<Long,ArrayList>();
        redoStates=new TreeMap<Long,ArrayList>();
        width=500;
        height=500;
        backgroundColor=Color.WHITE;
        keyBindings=new HashMap<String,ArrayList<ArrayList>>();
        mouseBindings=new HashMap<Turtle,ArrayList<ArrayList>>();
        centerX=0;
        centerY=0;
        scale=1;
        keysDown=new TreeSet<String>();
        processedKeys=new TreeSet<String>();
        unprocessedKeys=new TreeSet<String>();
        lastUpdate=0;
        fps=30;
        dragx=0;
        dragy=0;
        x=0;
        y=0;
        modifiers=0;
        refreshMode=REFRESH_MODE_ANIMATED;
        backgroundMode=BACKGROUND_MODE_TILE_RELATIVE;
        selectedTurtle=null;
        running=true;


        window = new JFrame("Turtle");
        icon = new ImageIcon();
        setupBuffering();
        draw = new JLabel(icon);
        window.setContentPane(draw);
        //window.setDefaultCloseOperation (JFrame.DISPOSE_ON_CLOSE);
        try{window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);}
        catch(Exception e){}
        
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("File");
        menuBar.add(menu);
        JMenuItem menuItem1 = new JMenuItem("Save...");
        
        menuItem1.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menu.add(menuItem1);
        window.setJMenuBar(menuBar);
        window.pack();
        window.requestFocusInWindow();
        drawTurtleIcon();
        window.setVisible(true);
        makeShapes();
        turtle=new Turtle(0);
        draw.setFocusable(true);
        menuItem1.addActionListener(turtle);
        window.addComponentListener(turtle);
        draw.addComponentListener(turtle);
        draw.addMouseListener(turtle);
        draw.addMouseMotionListener(turtle);
        draw.addMouseWheelListener(turtle);
        window.addKeyListener(turtle);
        draw.addKeyListener(turtle);
        draw.requestFocus();
        initColors();

//        GraphicsEnvironment ge;
//        ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
//        String[] fontNames = ge.getAvailableFontFamilyNames();
//        System.out.println(Arrays.toString(fontNames));

        (new Thread(turtle,"render")).start();
        (new Thread(turtle,"listen")).start();
    }

    public static void exit()
    {
        running=false;
        window.setVisible(false);
        window.dispose();
    }

    /**
     * This is an experimental method that should allow you to make turtle
     * applets in the future.  For now, it doesn't work because the key and
     * mouse bindings require reflection and applets think that allowing
     * reflection would open a security hole.  Theoretically in the init method
     * of the applet you need to place <code>Turtle.startApplet(this);</code>.
     * <b>This is not currently working.</b>
     *
     * @param applet
     */
    public static void startApplet(JApplet applet)
    {
        //turtleStates.clear();
        //turtles.clear();
        //init();
        Turtle.applet=applet;
        applet.setContentPane(window.getContentPane());
        window.setVisible(false);
        try{(new Thread((Runnable)applet,"turtle")).start();}
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    private static void initColors()
    {
        colors = new HashMap<String, Color>();
        colors.put("aliceblue", new Color(0xf0f8ff));
        colors.put("antiquewhite", new Color(0xfaebd7));
        colors.put("aqua", new Color(0x00ffff));
        colors.put("aquamarine", new Color(0x7fffd4));
        colors.put("azure", new Color(0xf0ffff));
        colors.put("beige", new Color(0xf5f5dc));
        colors.put("bisque", new Color(0xffe4c4));
        colors.put("black", new Color(0x000000));
        colors.put("blanchedalmond", new Color(0xffebcd));
        colors.put("blue", new Color(0x0000ff));
        colors.put("blueviolet", new Color(0x8a2be2));
        colors.put("brown", new Color(0xa52a2a));
        colors.put("burlywood", new Color(0xdeb887));
        colors.put("cadetblue", new Color(0x5f9ea0));
        colors.put("chartreuse", new Color(0x7fff00));
        colors.put("chocolate", new Color(0xd2691e));
        colors.put("coral", new Color(0xff7f50));
        colors.put("cornflowerblue", new Color(0x6495ed));
        colors.put("cornsilk", new Color(0xfff8dc));
        colors.put("crimson", new Color(0xdc143c));
        colors.put("cyan", new Color(0x00ffff));
        colors.put("darkblue", new Color(0x00008b));
        colors.put("darkcyan", new Color(0x008b8b));
        colors.put("darkgoldenrod", new Color(0xb8860b));
        colors.put("darkgray", new Color(0xa9a9a9));
        colors.put("darkgreen", new Color(0x006400));
        colors.put("darkkhaki", new Color(0xbdb76b));
        colors.put("darkmagenta", new Color(0x8b008b));
        colors.put("darkolivegreen", new Color(0x556b2f));
        colors.put("darkorange", new Color(0xff8c00));
        colors.put("darkorchid", new Color(0x9932cc));
        colors.put("darkred", new Color(0x8b0000));
        colors.put("darksalmon", new Color(0xe9967a));
        colors.put("darkseagreen", new Color(0x8fbc8f));
        colors.put("darkslateblue", new Color(0x483d8b));
        colors.put("darkslategray", new Color(0x2f4f4f));
        colors.put("darkturquoise", new Color(0x00ced1));
        colors.put("darkviolet", new Color(0x9400d3));
        colors.put("deeppink", new Color(0xff1493));
        colors.put("deepskyblue", new Color(0x00bfff));
        colors.put("dimgray", new Color(0x696969));
        colors.put("dodgerblue", new Color(0x1e90ff));
        colors.put("firebrick", new Color(0xb22222));
        colors.put("floralwhite", new Color(0xfffaf0));
        colors.put("forestgreen", new Color(0x228b22));
        colors.put("fuchsia", new Color(0xff00ff));
        colors.put("gainsboro", new Color(0xdcdcdc));
        colors.put("ghostwhite", new Color(0xf8f8ff));
        colors.put("gold", new Color(0xffd700));
        colors.put("goldenrod", new Color(0xdaa520));
        colors.put("gray", new Color(0x808080));
        colors.put("green", new Color(0x008000));
        colors.put("greenyellow", new Color(0xadff2f));
        colors.put("honeydew", new Color(0xf0fff0));
        colors.put("hotpink", new Color(0xff69b4));
        colors.put("indianred", new Color(0xcd5c5c));
        colors.put("indigo", new Color(0x4b0082));
        colors.put("ivory", new Color(0xfffff0));
        colors.put("khaki", new Color(0xf0e68c));
        colors.put("lavender", new Color(0xe6e6fa));
        colors.put("lavenderblush", new Color(0xfff0f5));
        colors.put("lawngreen", new Color(0x7cfc00));
        colors.put("lemonchiffon", new Color(0xfffacd));
        colors.put("lightblue", new Color(0xadd8e6));
        colors.put("lightcoral", new Color(0xf08080));
        colors.put("lightcyan", new Color(0xe0ffff));
        colors.put("lightgoldenrodyellow", new Color(0xfafad2));
        colors.put("lightgreen", new Color(0x90ee90));
        colors.put("lightgrey", new Color(0xd3d3d3));
        colors.put("lightpink", new Color(0xffb6c1));
        colors.put("lightsalmon", new Color(0xffa07a));
        colors.put("lightseagreen", new Color(0x20b2aa));
        colors.put("lightskyblue", new Color(0x87cefa));
        colors.put("lightslategray", new Color(0x778899));
        colors.put("lightsteelblue", new Color(0xb0c4de));
        colors.put("lightyellow", new Color(0xffffe0));
        colors.put("lime", new Color(0x00ff00));
        colors.put("limegreen", new Color(0x32cd32));
        colors.put("linen", new Color(0xfaf0e6));
        colors.put("magenta", new Color(0xff00ff));
        colors.put("maroon", new Color(0x800000));
        colors.put("mediumaquamarine", new Color(0x66cdaa));
        colors.put("mediumblue", new Color(0x0000cd));
        colors.put("mediumorchid", new Color(0xba55d3));
        colors.put("mediumpurple", new Color(0x9370db));
        colors.put("mediumseagreen", new Color(0x3cb371));
        colors.put("mediumslateblue", new Color(0x7b68ee));
        colors.put("mediumspringgreen", new Color(0x00fa9a));
        colors.put("mediumturquoise", new Color(0x48d1cc));
        colors.put("mediumvioletred", new Color(0xc71585));
        colors.put("midnightblue", new Color(0x191970));
        colors.put("mintcream", new Color(0xf5fffa));
        colors.put("mistyrose", new Color(0xffe4e1));
        colors.put("moccasin", new Color(0xffe4b5));
        colors.put("navajowhite", new Color(0xffdead));
        colors.put("navy", new Color(0x000080));
        colors.put("oldlace", new Color(0xfdf5e6));
        colors.put("olive", new Color(0x808000));
        colors.put("olivedrab", new Color(0x6b8e23));
        colors.put("orange", new Color(0xffa500));
        colors.put("orangered", new Color(0xff4500));
        colors.put("orchid", new Color(0xda70d6));
        colors.put("palegoldenrod", new Color(0xeee8aa));
        colors.put("palegreen", new Color(0x98fb98));
        colors.put("paleturquoise", new Color(0xafeeee));
        colors.put("palevioletred", new Color(0xdb7093));
        colors.put("papayawhip", new Color(0xffefd5));
        colors.put("peachpuff", new Color(0xffdab9));
        colors.put("peru", new Color(0xcd853f));
        colors.put("pink", new Color(0xffc0cb));
        colors.put("plum", new Color(0xdda0dd));
        colors.put("powderblue", new Color(0xb0e0e6));
        colors.put("purple", new Color(0x800080));
        colors.put("red", new Color(0xff0000));
        colors.put("rosybrown", new Color(0xbc8f8f));
        colors.put("royalblue", new Color(0x4169e1));
        colors.put("saddlebrown", new Color(0x8b4513));
        colors.put("salmon", new Color(0xfa8072));
        colors.put("sandybrown", new Color(0xf4a460));
        colors.put("seagreen", new Color(0x2e8b57));
        colors.put("seashell", new Color(0xfff5ee));
        colors.put("sienna", new Color(0xa0522d));
        colors.put("silver", new Color(0xc0c0c0));
        colors.put("skyblue", new Color(0x87ceeb));
        colors.put("slateblue", new Color(0x6a5acd));
        colors.put("slategray", new Color(0x708090));
        colors.put("snow", new Color(0xfffafa));
        colors.put("springgreen", new Color(0x00ff7f));
        colors.put("steelblue", new Color(0x4682b4));
        colors.put("tan", new Color(0xd2b48c));
        colors.put("teal", new Color(0x008080));
        colors.put("thistle", new Color(0xd8bfd8));
        colors.put("tomato", new Color(0xff6347));
        colors.put("turquoise", new Color(0x40e0d0));
        colors.put("violet", new Color(0xee82ee));
        colors.put("wheat", new Color(0xf5deb3));
        colors.put("white", new Color(0xffffff));
        colors.put("whitesmoke", new Color(0xf5f5f5));
        colors.put("yellow", new Color(0xffff00));
        colors.put("yellowgreen", new Color(0x9acd32));
    }

    private static Color getColor(String color)
    {
        String origColor=color;
        color = color.toLowerCase().replaceAll("[^a-z]", "");
        if (colors.containsKey(color))
        {
            return colors.get(color);
        }
        else
        {
           return null;
        }
    }

    private static void setupBuffering()
    {
        synchronized(turtleLock)
        {
            lastUpdate=0;
            offscreenImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            midscreenImage  = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            onscreenImage  = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            offscreen = offscreenImage.createGraphics();
            midscreen  = midscreenImage.createGraphics();
            onscreen  = onscreenImage.createGraphics();
            //offscreen.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
            //offscreen.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            offscreen.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            midscreen.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            onscreen.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            drawBackground(offscreen);
            drawBackground(onscreen);
            icon.setImage(onscreenImage);
        }
    }

    private static void drawTurtleIcon()
    {
        byte[] imageData= new byte[]{71,73,70,56,57,97,16,0,16,0,-95,2,0,0,-103,
            0,0,-1,0,-1,-1,-1,-1,-1,-1,33,-7,4,1,10,0,2,0,44,0,0,0,0,16,0,16,0,
            0,2,44,-108,-113,-87,-53,-19,-33,-128,4,104,74,35,67,-72,34,-21,11,
            124,27,-90,-107,-109,72,117,-91,-71,110,103,-37,90,-31,-10,-55,-87,
            122,-34,74,72,-15,17,-56,-127,8,33,5,0,59};
        try
        {
            BufferedImage tmpicon = ImageIO.read(new ByteArrayInputStream(imageData));
            window.setIconImage(tmpicon);
        }
        catch (Exception e) {}
    }

    private static void makeShapes()
    {
        shapes=new HashMap<String,Polygon>();
        int[] xs=new int[]{66, 65, 63, 61, 53, 44, 33, 24, 23, 19, 17, 14, 9, 8, 8, 10, 13, 11, 10, 2, 9, 11, 15, 11, 11, 10, 12, 18, 20, 22, 23, 26, 35, 44, 53, 61, 62, 64, 66, 71, 77, 78, 77, 76, 72, 77, 81, 86, 91, 94, 97, 98, 97, 95, 92, 87, 82, 77, 72, 74, 77, 78, 76, 70};
        int[] ys=new int[]{18, 19, 21, 25, 23, 22, 23, 27, 25, 21, 20, 21, 27, 30, 32, 34, 37, 42, 47, 50, 53, 59, 65, 68, 69, 71, 74, 79, 80, 80, 78, 74, 77, 78, 77, 75, 79, 81, 82, 81, 76, 73, 71, 69, 66, 59, 60, 61, 60, 58, 54, 50, 46, 42, 40, 39, 40, 41, 34, 32, 28, 27, 24, 19};
        Polygon p = new Polygon(xs,ys,xs.length);
        shapes.put("turtle", p);
        xs=new int[]{0,100,0,20};
        ys=new int[]{0,50,100,50};
        p = new Polygon(xs,ys,xs.length);
        shapes.put("arrow", p);
        xs=new int[]{0,100,100,0};
        ys=new int[]{0,0,100,100};
        p = new Polygon(xs,ys,xs.length);
        shapes.put("rectangle", p);
        shapes.put("square", p);
        xs=new int[]{0,100,0};
        ys=new int[]{0,50,100};
        p = new Polygon(xs,ys,xs.length);
        shapes.put("triangle", p);
        int divisions=24;
        xs=new int[divisions];
        ys=new int[divisions];
        for(int i=0;i<divisions;i++)
        {
            double angle=Math.toRadians(i*360.0/divisions);
            xs[i]=(int)Math.round(50+50*Math.cos(angle));
            ys[i]=(int)Math.round(50+50*Math.sin(angle));
        }
        p = new Polygon(xs,ys,xs.length);
        shapes.put("circle", p);
    }

    /*    .-./*)   .-./*)   .-./*)   .-./*)   .-./*)   .-./*)   .-./*)   .-./*)
     *  _/___\/  _/___\/  _/___\/  _/___\/  _/___\/  _/___\/  _/___\/  _/___\/
     *    U U      U U      U U      U U      U U      U U      U U      U U
     *    .-./*)   .-./*)   .-./*)                     .-./*)   .-./*)   .-./*)
     *  _/___\/  _/___\/  _/___\/ TURTLE CONSTRUCTION_/___\/  _/___\/  _/___\/
     *    U U      U U      U U                        U U      U U      U U
     *    .-./*)   .-./*)   .-./*)   .-./*)   .-./*)   .-./*)   .-./*)   .-./*)
     *  _/___\/  _/___\/  _/___\/  _/___\/  _/___\/  _/___\/  _/___\/  _/___\/
     *    U U      U U      U U      U U      U U      U U      U U      U U
     */

    /**
     * This is a internal constuctor that makes a singleton that does the
     * listening but is not added to the stack of turtles to be rendered.
     * You don't need to use this outside of the Turtle.java file.
     *
     * @param i Pass this any integer.  It doesn't do anything.
     */
    private Turtle(int i){}

    private Point2D.Double location=new Point2D.Double(0,0);
    private double direction=0; //degrees
    private String shape="turtle"; //Stores a key to the shapes hashmap
    private BufferedImage image=null;
    private double shapeWidth=33;
    private double shapeHeight=33;
    private double tilt=0;
    private double penWidth=2;
    private Color penColor=Color.BLACK;
    private double outlineWidth=2;
    private Color outlineColor=Color.BLACK;
    private Color fillColor=new Color(0,255,0,128);
    private double speed=50; //milliseconds to execute a move
    private boolean isPenDown=true;
    private boolean isFilling=false;
    private boolean isVisible=true;
    private ArrayList<Point2D.Double> polygon=new ArrayList<Point2D.Double>();
    //temporary storage
    private Long _time;
    private Point2D.Double _location;
    private Double _direction;
    private String _shape;
    private BufferedImage _image;
    private Double _shapeWidth;
    private Double _shapeHeight;
    private Double _tilt;
    private Double _penWidth;
    private Color _penColor;
    private Double _outlineWidth;
    private Color _outlineColor;
    private Color _fillColor;
    private Double _speed;
    private Boolean _isPenDown;
    private Boolean _isFilling;
    private Boolean _isVisible;
    private ArrayList<Point2D.Double> _polygon;
    private Boolean _isFill;
    private Boolean _isStamp;
    private Double _dotSize;
    private Color _dotColor;
    private Font _font;
    private String _text;
    private Integer _justification;
    private Point2D.Double _textOffset;

    //secondary temporary storage
    private Long __time;
    private Point2D.Double __location;
    private Double __direction;
    private String __shape;
    private BufferedImage __image;
    private Double __shapeWidth;
    private Double __shapeHeight;
    private Double __tilt;
    private Double __penWidth;
    private Color __penColor;
    private Double __outlineWidth;
    private Color __outlineColor;
    private Color __fillColor;
    private Double __speed;
    private Boolean __isPenDown;
    private Boolean __isFilling;
    private Boolean __isVisible;
    private ArrayList<Point2D.Double> __polygon;
    private Boolean __isFill;
    private Boolean __isStamp;
    private Double __dotSize;
    private Color __dotColor;
    private Font __font;
    private String __text;
    private Integer __justification;
    private Point2D.Double __textOffset;

    /**
     * Makes a default turtle.
     */
    public Turtle()
    {
        if(window==null)init();
        synchronized(turtleLock){turtles.add(this);}
        long time=storeCurrentState();
        updateAll();
    }

    /**
     * Makes a default turtle at the specified position.
     *
     * @param x x coordinate
     * @param y y coordinate
     */
    public Turtle(double x,double y)
    {
        if(window==null)init();
        location=new Point2D.Double(x,y);
        synchronized(turtleLock){turtles.add(this);}
        long time=storeCurrentState();
        updateAll();
    }

    /**
     * This creates a cloned copy of a turtle.
     *
     * @return a cloned copy of a turtle
     */
    public Turtle clone()
    {
        Turtle t=new Turtle(0);
        t.location=(Point2D.Double)this.location.clone();
        t.direction=this.direction;
        t.shape=t.shape;
        t.image=this.image;
        t.shapeWidth=this.shapeWidth;
        t.shapeHeight=this.shapeHeight;
        t.tilt=this.tilt;
        t.penWidth=this.penWidth;
        t.penColor=this.penColor;
        t.outlineWidth=this.outlineWidth;
        t.outlineColor=this.outlineColor;
        t.fillColor=this.fillColor;
        t.speed=this.speed;
        t.isPenDown=this.isPenDown;
        t.isFilling=this.isFilling;
        t.isVisible=this.isVisible;
        if(window==null)init();
        synchronized(turtleLock){turtles.add(t);}
        long time=t.storeCurrentState();
        return t;
    }

/*    .-./*)   .-./*)   .-./*)   .-./*)   .-./*)   .-./*)   .-./*)   .-./*)
 *  _/___\/  _/___\/  _/___\/  _/___\/  _/___\/  _/___\/  _/___\/  _/___\/
 *    U U      U U      U U      U U      U U      U U      U U      U U
 *    .-./*)   .-./*)   .-./*)                     .-./*)   .-./*)   .-./*)
 *  _/___\/  _/___\/  _/___\/  STATE MANAGEMENT  _/___\/  _/___\/  _/___\/
 *    U U      U U      U U                        U U      U U      U U
 *    .-./*)   .-./*)   .-./*)   .-./*)   .-./*)   .-./*)   .-./*)   .-./*)
 *  _/___\/  _/___\/  _/___\/  _/___\/  _/___\/  _/___\/  _/___\/  _/___\/
 *    U U      U U      U U      U U      U U      U U      U U      U U
 */

    private long storeCurrentState()
    {
        return storeCurrentState(false, false, 0,null,null,null,0,null);
    }
    private long storeAnimatedState()
    {
        return storeCurrentState(true, false, 0,null,null,null,0,null);
    }
    private long storeCurrentState(boolean animate, boolean isStamp,double dotSize,Color dotColor,Font font,String text,int justification,Point2D.Double textOffset)
    {
        ArrayList state = new ArrayList();
        long time=System.nanoTime();
        synchronized(turtleLock)
        {
            state.add(0);            //0
            state.add(this);            //1
            state.add(location.clone());//2
            state.add(direction);       //3
            state.add(shape);           //4
            state.add(image);           //5
            state.add(shapeWidth);      //6
            state.add(shapeHeight);     //7
            state.add(tilt);            //8
            state.add(penWidth);        //9
            state.add(penColor);        //10
            state.add(outlineWidth);    //11
            state.add(outlineColor);    //12
            state.add(fillColor);       //13
            state.add(speed);           //14
            state.add(isPenDown);       //15
            state.add(isFilling);       //16
            state.add(isVisible);       //17
            state.add(isStamp);         //18
            state.add(dotSize);         //19
            state.add(dotColor);        //20
            state.add(font);            //21
            state.add(text);            //22
            state.add(justification);   //23
            state.add(textOffset);      //24
            if (!turtleStates.isEmpty() && turtleStates.lastKey()>time) time=turtleStates.lastKey()+1;
            if (animate) time+=(long)(speed*1000000);
            state.set(0, time);
            turtleStates.put(time, state);
            redoStates.clear(); 
        }
        if(refreshMode==REFRESH_MODE_STATE_CHANGE) draw();
        if(refreshMode==REFRESH_MODE_ANIMATED)waitUntil(time);
        return time;
    }

    private static void clearStorage()
    {
        synchronized(turtleLock)
        {
            for(Turtle t:turtles)
            {
                t.__time=null;
                t.__location=null;
                t.__direction=null;
                t.__shape=null;
                t.__image=null;
                t.__shapeWidth=null;
                t.__shapeHeight=null;
                t.__tilt=null;
                t.__penWidth=null;
                t.__penColor=null;
                t.__outlineWidth=null;
                t.__outlineColor=null;
                t.__fillColor=null;
                t.__speed=null;
                t.__isPenDown=null;
                t.__isFilling=null;
                t.__isVisible=null;
                t.__isStamp=null;
                t.__dotSize=null;
                t.__dotColor=null;
                t.__font=null;
                t.__text=null;
                t.__justification=null;
                t.__textOffset=null;
                t._time=null;
                t._location=null;
                t._direction=null;
                t._shape=null;
                t._shapeWidth=null;
                t._shapeHeight=null;
                t._image=null;
                t._tilt=null;
                t._penWidth=null;
                t._penColor=null;
                t._outlineWidth=null;
                t._outlineColor=null;
                t._fillColor=null;
                t._speed=null;
                t._isPenDown=null;
                t._isFilling=null;
                t._isVisible=null;
                t._isStamp=null;
                t._dotSize=null;
                t._dotColor=null;
                t._font=null;
                t._text=null;
                t._justification=null;
                t._textOffset=null;
            }
        }
    }

    private static void retrieveState(long time)
    {
        synchronized(turtleLock)
        {
            Turtle t=getStateTurtle(turtleStates.get(time));
            t.__time=t._time;
            t.__location=t._location;
            t.__direction=t._direction;
            t.__shape=t._shape;
            t.__image=t._image;
            t.__shapeWidth=t._shapeWidth;
            t.__shapeHeight=t._shapeHeight;
            t.__tilt=t._tilt;
            t.__penWidth=t._penWidth;
            t.__penColor=t._penColor;
            t.__outlineWidth=t._outlineWidth;
            t.__outlineColor=t._outlineColor;
            t.__fillColor=t._fillColor;
            t.__speed=t._speed;
            t.__isPenDown=t._isPenDown;
            t.__isFilling=t._isFilling;
            t.__isVisible=t._isVisible;
            t.__isStamp=t._isStamp;
            t.__dotSize=t._dotSize;
            t.__dotColor=t._dotColor;
            t.__font=t._font;
            t.__text=t._text;
            t.__justification=t._justification;
            t.__textOffset=t._textOffset;
            ArrayList state=turtleStates.get(time);
            t._time=getStateTime(state);
            t._location=getStateLocation(state);
            t._direction=getStateDirection(state);
            t._shape=getStateShape(state);
            t._shapeWidth=getStateShapeWidth(state);
            t._shapeHeight=getStateShapeHeight(state);
            t._image=getStateImage(state);
            t._tilt=getStateTilt(state);
            t._penWidth=getStatePenWidth(state);
            t._penColor=getStatePenColor(state);
            t._outlineWidth=getStateOutlineWidth(state);
            t._outlineColor=getStateOutlineColor(state);
            t._fillColor=getStateFillColor(state);
            t._speed=getStateSpeed(state);
            t._isPenDown=getStateIsPenDown(state);
            t._isFilling=getStateIsFilling(state);
            t._isVisible=getStateIsVisible(state);
            t._isStamp=getStateIsStamp(state);
            t._dotSize=getStateDotSize(state);
            t._dotColor=getStateDotColor(state);
            t._font=getStateFont(state);
            t._text=getStateText(state);
            t._justification=getStateJustification(state);
            t._textOffset=getStateTextOffset(state);
        }
    }

    private static long getStateTime(ArrayList state){return (Long)state.get(0);}
    private static Turtle getStateTurtle(ArrayList state){return (Turtle)state.get(1);}
    private static Point2D.Double getStateLocation(ArrayList state){return (Point2D.Double)((Point2D.Double)state.get(2)).clone();}
    private static double getStateDirection(ArrayList state){return (Double)state.get(3);}
    private static String getStateShape(ArrayList state){return (String)state.get(4);}
    private static BufferedImage getStateImage(ArrayList state){return (BufferedImage)state.get(5);}
    private static double getStateShapeWidth(ArrayList state){return (Double)state.get(6);}
    private static double getStateShapeHeight(ArrayList state){return (Double)state.get(7);}
    private static double getStateTilt(ArrayList state){return (Double)state.get(8);}
    private static double getStatePenWidth(ArrayList state){return (Double)state.get(9);}
    private static Color getStatePenColor(ArrayList state){return (Color)state.get(10);}
    private static double getStateOutlineWidth(ArrayList state){return (Double)state.get(11);}
    private static Color getStateOutlineColor(ArrayList state){return (Color)state.get(12);}
    private static Color getStateFillColor(ArrayList state){return (Color)state.get(13);}
    private static double getStateSpeed(ArrayList state){return (Double)state.get(14);}
    private static boolean getStateIsPenDown(ArrayList state){return (Boolean)state.get(15);}
    private static boolean getStateIsFilling(ArrayList state){return (Boolean)state.get(16);}
    private static boolean getStateIsVisible(ArrayList state){return (Boolean)state.get(17);}
    private static boolean getStateIsStamp(ArrayList state){return (Boolean)state.get(18);}
    private static double getStateDotSize(ArrayList state){return (Double)state.get(19);}
    private static Color getStateDotColor(ArrayList state){return (Color)state.get(20);}
    private static Font getStateFont(ArrayList state){return (Font)state.get(21);}
    private static String getStateText(ArrayList state){return (String)state.get(22);}
    private static int getStateJustification(ArrayList state){return (Integer)state.get(23);}
    private static Point2D.Double getStateTextOffset(ArrayList state){return (Point2D.Double)state.get(24);}

    private static void restoreState(long time)
    {
        ArrayList state=turtleStates.get(time);
        Turtle t=getStateTurtle(turtleStates.get(time));
        t.location=getStateLocation(state);
        t.direction=getStateDirection(state);
        t.shape=getStateShape(state);
        t.shapeWidth=getStateShapeWidth(state);
        t.shapeHeight=getStateShapeHeight(state);
        t.image=getStateImage(state);
        t.tilt=getStateTilt(state);
        t.penWidth=getStatePenWidth(state);
        t.penColor=getStatePenColor(state);
        t.outlineWidth=getStateOutlineWidth(state);
        t.outlineColor=getStateOutlineColor(state);
        t.fillColor=getStateFillColor(state);
        t.speed=getStateSpeed(state);
        t.isPenDown=getStateIsPenDown(state);
        t.isFilling=getStateIsFilling(state);
        t.isVisible=getStateIsVisible(state);
        if(refreshMode==REFRESH_MODE_STATE_CHANGE) draw();
    }

    private void select()
    {
        selectedTurtle=this;
    }

    private void unselect()
    {
        if (selectedTurtle==this) selectedTurtle=null;
    }

    /**
     * Determines if a turtle is covering a screen position
     *
     * @param x x screen coordinate
     * @param y y screen coordinate
     * @return true if this turtle is at the indicated screen position.
     */
    public boolean contains(int x, int y)
    {
        Point2D.Double point=new Point2D.Double(x,y);
        if (_location==null)return false;
        AffineTransform m = offscreen.getTransform();
        double x1,y1,dir1;
        x1=_location.x;
        y1=_location.y;
        dir1=_direction;
        m.translate(((x1-centerX)*scale+width/2),((y1-centerY)*(-scale)+height/2));
        m.scale(scale, scale);
        if (image==null)
        {
            m.rotate(-Math.toRadians(dir1));
            m.scale(shapeWidth/100.0, shapeHeight/100.0);
            m.translate(-50,-50);
            Polygon p =shapes.get(shape);
            GeneralPath gp=new GeneralPath();
            gp.append(p.getPathIterator(m),false);
            return gp.contains(x, y);
        }
        else
        {
            int w=image.getWidth();
            int h=image.getHeight();
            m.rotate(-Math.toRadians(dir1));
            m.scale(shapeWidth/1.0/w, shapeHeight/1.0/h);
            m.translate(-w/2,-h/2);
            try {m.inverseTransform(point, point);}
            catch(Exception e){return false;}
            x=(int)point.x;
            y=(int)point.y;
            try 
            {
                //System.out.println((new Color(image.getRGB(x, y),true)).getAlpha());
                return (new Color(image.getRGB(x, y),true)).getAlpha()>50;
            }
            catch(Exception e){return false;}
        }
    }
    
    /*    .-./*)   .-./*)   .-./*)   .-./*)   .-./*)   .-./*)   .-./*)   .-./*)
     *  _/___\/  _/___\/  _/___\/  _/___\/  _/___\/  _/___\/  _/___\/  _/___\/
     *    U U      U U      U U      U U      U U      U U      U U      U U
     *    .-./*)   .-./*)   .-./*)                     .-./*)   .-./*)   .-./*)
     *  _/___\/  _/___\/  _/___\/   TURTLE METHODS   _/___\/  _/___\/  _/___\/
     *    U U      U U      U U                        U U      U U      U U
     *    .-./*)   .-./*)   .-./*)   .-./*)   .-./*)   .-./*)   .-./*)   .-./*)
     *  _/___\/  _/___\/  _/___\/  _/___\/  _/___\/  _/___\/  _/___\/  _/___\/
     *    U U      U U      U U      U U      U U      U U      U U      U U
     */

    /**
     * Gets the speed of the animation.
     * @return milliseconds it takes to do one action
     */
    public double getSpeed()
    {
        return speed;
    }

    /**
     * Sets the speed of the animation.
     * @param delay milliseconds it takes to do one action
     * @return state change timestamp
     */
    public long speed(double delay)
    {
        this.speed=delay;
        long timeStamp=storeCurrentState();
        return timeStamp;
    }

    /**
     * Moves the turtle forward by the distance.
     *
     * @param distance distance to move forward
     * @return state change timestamp
     */
    public long forward(double distance)
    {
        double angle=Math.toRadians(direction);
        Point2D.Double pastLocation=(Point2D.Double)location.clone();
        location.x+=distance*Math.cos(angle);
        location.y+=distance*Math.sin(angle);
        long timeStamp=storeAnimatedState();
        return timeStamp;
    }

    /**
     * Moves the turtle backward by the distance.
     *
     * @param distance distance to move backward
     * @return state change timestamp
     */
    public long backward(double distance)
    {
        double angle=Math.toRadians(direction);
        Point2D.Double pastLocation=(Point2D.Double)location.clone();
        location.x-=distance*Math.cos(angle);
        location.y-=distance*Math.sin(angle);
        long timeStamp=storeAnimatedState();
        return timeStamp;
    }

    /**
     * Turns the turtle left by the number of indicated degrees.
     * 
     * @param angle angle in degrees
     * @return state change timestamp
     */
    public long left(double angle)
    {
        direction+=angle;
        long timeStamp=storeAnimatedState();
        return timeStamp;
    }
    
    /**
     * Turns the turtle right by the number of indicated degrees.
     * 
     * @param angle angle in degrees
     * @return state change timestamp
     */
    public long right(double angle)
    {
        direction-=angle;
        long timeStamp=storeAnimatedState();
        return timeStamp;
    }

    /**
     * Gets the direction the turtle is facing neglecting tilt.
     *
     * @return state change timestamp
     */
    public double getDirection()
    {
        double a=direction;
        while(a>=360)a-=360;
        while(a<0)a+=360;
        return a;
    }

    /**
     * Sets the direction the turtle is facing neglecting tilt.
     *
     * @param direction angle counter-clockwise from east
     * @return state change timestamp
     */
    public long setDirection(double direction)
    {
        double a=direction;
        while(this.direction-a>180)a+=360;
        while(this.direction-a<-180)a-=360;
        this.direction=a;
        //this.direction=direction;
        long timeStamp=storeAnimatedState();
        return timeStamp;
    }

    /**
     * Moves the turtle to (0,0) and facing east.
     *
     * @return state change timestamp
     */
    public long home()
    {
        return setPosition(0,0,0);
    }

    /**
     * Hides the turtle but it can still draw.
     *
     * @return state change timestamp
     */
    public long hide()
    {
        isVisible=false;
        long timeStamp=storeCurrentState();
        return timeStamp;
    }

    /**
     * Makes the turtle visible.
     *
     * @return state change timestamp
     */
    public long show()
    {
        isVisible=true;
        long timeStamp=storeCurrentState();
        return timeStamp;
    }

    /**
     * Sets the direction in such a way that it faces (x,y)
     *
     * @param x x coordinate of target location
     * @param y y coordinate of target location
     * @return state change timestamp
     */
    public long face(double x, double y)
    {
        return setDirection(towards(x,y));
    }

    /**
     * Gets direction towards (x,y)
     *
     * @param x x coordinate of target location
     * @param y y coordinate of target location
     * @return angle in degrees where 0<=angle<360
     */
    public double towards(double x, double y)
    {
        return Math.toDegrees(Math.atan2(y-location.y, x-location.x));
    }

    /**
     * Gets the distance to another position.
     *
     * @param x x coordinate of target location
     * @param y y coordinate of target location
     * @return distance between turtle's current location and another position
     */
    public double distance(double x, double y)
    {
        return Math.sqrt((y-location.y)*(y-location.y)+(x-location.x)*(x-location.x));
    }

    /**
     * Gets the x coordinate of the turtle.
     *
     * @return x coordinate
     */
    public double getX()
    {
        return location.x;
    }

    /**
     * Gets the y coordinate of the turtle.
     *
     * @return y coordinate
     */
    public double getY()
    {
        return location.y;
    }

    /**
     * Sets the position and direction of a turtle.
     *
     * @param x x coordinate
     * @param y y coordinate
     * @param direction angle counter-clockwise from east in degrees
     * @return state change timestamp
     */
    public long setPosition(double x, double y, double direction)
    {
        location.x=x;
        location.y=y;
        double a=direction;
        while(this.direction-a>180)a+=360;
        while(this.direction-a<-180)a-=360;
        this.direction=a;
        this.direction=direction;
        long timeStamp=storeAnimatedState();
        return timeStamp;
    }

    /**
     * Sets the position of a turtle.
     *
     * @param x x coordinate
     * @param y y coordinate
     * @return state change timestamp
     */
    public long setPosition(double x, double y)
    {
        return setPosition(x, y, direction);
    }

    /**
     * Adds an additional angle to rotation of the turtle's shape when rendering.
     * This is useful when you need to face a different direction than the
     * direction you are moving in.
     *
     * @param angle angle in degrees
     * @return state change timestamp
     */
    public long tilt(double angle)
    {
        tilt+=angle;
        long timeStamp=storeAnimatedState();
        return timeStamp;
    }

    /**
     * Sets the angle to rotate the turtle's shape when rendering.
     * This is useful when you need to face a different direction than the
     * direction you are moving in.
     *
     * @param angle angle in degrees
     * @return state change timestamp
     */
    public long setTilt(double angle)
    {
        //double a=angle;
        //while(tilt-a>180)a+=360;
        //while(tilt-a<-180)a-=360;
        //tilt=a;
        tilt=angle;
        long timeStamp=storeAnimatedState();
        return timeStamp;
    }

    /**
     * Gets the rotation of the turtle's shape away from the turtle's direction.
     *
     * @return tilt in degrees (positive in counter-clockwise)
     */
    public double getTilt()
    {
        return tilt;
    }

    /**
     * Sets the width of the turtles path
     *
     * @param penWidth width of the turtles path
     * @return state change timestamp
     */
    public long width(double penWidth)
    {
        this.penWidth=penWidth;
        long timeStamp=storeCurrentState();
        return timeStamp;
    }

    /**
     * Sets the width of the turtle's outline.
     *
     * @param width width of the turtle's outline
     * @return state change timestamp
     */
    public long outlineWidth(double width)
    {
        this.outlineWidth=width;
        long timeStamp=storeCurrentState();
        return timeStamp;
    }

    /**
     * Picks the turtle's tail up so it won't draw on the screen as it moves.
     *
     * @return state change timestamp
     */
    public long up()
    {
        this.isPenDown=false;
        long timeStamp=storeCurrentState();
        return timeStamp;
    }

    /**
     * Puts the turtle's tail down so it will draw on the screen as it moves.
     *
     * @return state change timestamp
     */
    public long down()
    {
        this.isPenDown=true;
        long timeStamp=storeCurrentState();
        return timeStamp;
    }
    
    public long stab()
    {
		Color c=Turtle.getColor("red");
        if(c!=null)this.penColor=c;
        this.isPenDown=true;
        long timeStamp=storeCurrentState();
        return timeStamp;
    }

    /**
     * Sets the turtle's path color.
     *
     * @param penColor Color of the turtle's path.
     * @return state change timestamp
     */
    public long penColor(String penColor)
    {
        Color c=Turtle.getColor(penColor);
        if(c!=null)this.penColor=c;
        long timeStamp=storeCurrentState();
        return timeStamp;
    }

    /**
     * Sets the turtle's path color.
     *
     * @param penColor Color of the turtle's path.
     * @return state change timestamp
     */
    public long penColor(Color penColor)
    {
        this.penColor=penColor;
        long timeStamp=storeCurrentState();
        return timeStamp;
    }

    /**
     * Sets the turtle's outlineColor color.
     *
     * @param outlineColor Color of the turtle's outlineColor.
     * @return state change timestamp
     */
    public long outlineColor(String outlineColor)
    {
        Color c=Turtle.getColor(outlineColor);
        if(c!=null)this.outlineColor=c;
        long timeStamp=storeCurrentState();
        return timeStamp;
    }

    /**
     * Sets the turtle's outlineColor color.
     *
     * @param outlineColor Color of the turtle's outlineColor.
     * @return state change timestamp
     */
    public long outlineColor(Color outlineColor)
    {
        this.outlineColor=outlineColor;
        long timeStamp=storeCurrentState();
        return timeStamp;
    }

    /**
     * Sets the turtle's fill color.
     *
     * @param fillColor Color of the turtle's fill.
     * @return state change timestamp
     */
    public long fillColor(String fillColor)
    {
        Color c=Turtle.getColor(fillColor);
        if(c!=null)this.fillColor=c;
        long timeStamp=storeCurrentState();
        return timeStamp;
    }

    /**
     * Sets the turtle's fill color.
     *
     * @param fillColor Color of the turtle's fill.
     * @return state change timestamp
     */
    public long fillColor(Color fillColor)
    {
        this.fillColor=fillColor;
        long timeStamp=storeCurrentState();
        return timeStamp;
    }

    /**
     * Sets the shape of the turtle using the built in shapes (turtle,square,
     * rectangle,triangle,arrow,circle) or to a image.
     *
     * @param shape shapename or filename of image
     * @return state change timestamp
     */
    public long shape(String shape)
    {
        try
        {
            image=ImageIO.read(new File(shape));
            this.shapeHeight=image.getHeight();
            this.shapeWidth=image.getWidth();
        }
        catch(Exception e)
        {
            if(shapes.containsKey(shape))
            {
                this.shape=shape;
                this.shapeHeight=33;
                this.shapeWidth=33;
                image=null;
            }
            else {System.out.println("Unrecognized filename or shape name.");}
        }
        //if(refreshMode!=REFRESH_MODE_ON_DEMAND)updateAll();
        long timeStamp=storeCurrentState();
        return timeStamp;
    }

    public long shapeSize(int width, int height)
    {
        this.shapeHeight=height;
        this.shapeWidth=width;
        long timeStamp=storeCurrentState();
        return timeStamp;
    }

    /**
     * Put a copy of the current turtle shape on the canvas.
     *
     * @return state change timestamp
     */
    public long stamp()
    {
        long timeStamp=storeCurrentState(true, true,0,null,null,null,0,null);
        return timeStamp;
    }

    /**
     * Put a dot 3 times the size of the penWidth on the canvas.
     *
     * @return state change timestamp
     */
    public long dot()
    {
        long timeStamp=storeCurrentState(true, false,penWidth*3,penColor,null,null,0,null);
        return timeStamp;
    }

    /**
     * Put a dot 3 times the size of the penWidth on the canvas.
     *
     * @param color color of dot
     * @return state change timestamp
     */
    public long dot(String color)
    {
        Color c=Turtle.getColor(color);
        if(c==null)c=penColor;
        long timeStamp=storeCurrentState(true, false,penWidth*3,c,null,null,0,null);
        return timeStamp;
    }

    /**
     * Put a dot 3 times the size of the penWidth on the canvas.
     *
     * @param color color of dot
     * @return state change timestamp
     */
    public long dot(Color color)
    {
        long timeStamp=storeCurrentState(true, false,penWidth*3,color,null,null,0,null);
        return timeStamp;
    }

    /**
     * Put a dot on the canvas.
     *
     * @param color color of dot
     * @param dotSize diameter of the dot
     * @return state change timestamp
     */
    public long dot(String color,double dotSize)
    {
        Color c=Turtle.getColor(color);
        if(c==null)c=penColor;
        long timeStamp=storeCurrentState(true, false,dotSize,c,null,null,0,null);
        return timeStamp;
    }

    /**
     * Put a dot on the canvas.
     *
     * @param color color of dot
     * @param dotSize diameter of the dot
     * @return state change timestamp
     */
    public long dot(Color color,double dotSize)
    {
        long timeStamp=storeCurrentState(true, false,dotSize,color,null,null,0,null);
        return timeStamp;
    }

    public long write(String text, String fontName, int fontSize, int justification, double xOffset, double yOffset)
    {
        return 0;
    }

    /**
     * Undo turtle state changes.
     *
     * @param steps the number of state changes to remove
     */
    public void undo(int steps)
    {
        for(int i=0;i<steps;i++) rollback();
        lastUpdate=0;
        if(refreshMode!=REFRESH_MODE_ON_DEMAND)updateAll();
    }

    /**
     * Undo the last turtle state change.
     */
    public void undo()
    {
        rollback();
        lastUpdate=0;
        if(refreshMode!=REFRESH_MODE_ON_DEMAND)updateAll();
    }

   /**
     * Redo turtle state changes.
     *
     * @param steps the number of state changes to restore
     */
    public void redo(int steps)
    {
        for(int i=0;i<steps;i++) rollforward();
        lastUpdate=0;
        if(refreshMode!=REFRESH_MODE_ON_DEMAND)updateAll();
    }

    /**
     * Redo turtle state changes.
     */
    public void redo()
    {
        rollforward();
        lastUpdate=0;
        if(refreshMode!=REFRESH_MODE_ON_DEMAND)updateAll();
    }

    /**
     * Clears all the drawing that a turtle has done but all the turtle
     * settings remain the same. (color, location, direction, shape)
     */
    public void clear()
    {
        synchronized(turtleLock)
        {
            long removeKey=0;
            TreeMap<Long, ArrayList> copy_turtleStates=(TreeMap<Long, ArrayList>)turtleStates.clone();
            for (Map.Entry<Long, ArrayList> entry:copy_turtleStates.entrySet())
            {
                ArrayList state=entry.getValue();
                long time=entry.getKey();
                if(getStateTurtle(state)==this)
                {
                    if(removeKey!=0)
                    {
                        turtleStates.remove(removeKey);
                    }
                    removeKey=time;

                }
            }
            redoStates.clear();
            restoreState(removeKey);
        }
        lastUpdate=0;
        if(refreshMode!=REFRESH_MODE_ON_DEMAND)updateAll();
    }
    
    private void rollback()
    {
        int steps=0;

        synchronized(turtleLock)
        {
            long removeKey=0;
            long restoreTime=0;
            for (Map.Entry<Long, ArrayList> entry:turtleStates.descendingMap().entrySet())
            {
                ArrayList state=entry.getValue();
                long time=entry.getKey();
                if(getStateTurtle(state)==this)
                {
                    if (steps==0)
                    {
                        removeKey=time;
                        steps+=1;
                    }
                    else
                    {
                        restoreTime=time;
                        break;
                    }
                }
            }
            if (removeKey!=0 && restoreTime!=0)
            {
                restoreState(restoreTime);
                redoStates.put(removeKey, turtleStates.remove(removeKey));
            }
        }
    }
    
    private void rollforward()
    {
        synchronized(turtleLock)
        {
            for (Map.Entry<Long, ArrayList> entry:redoStates.entrySet())
            {
                ArrayList state=entry.getValue();
                long time=entry.getKey();
                if(getStateTurtle(state)==this)
                {
                    turtleStates.put(entry.getKey(), redoStates.remove(entry.getKey()));
                    restoreState(time);
                    return;
                }
            }
        }
    }

    /**
     * This specifies when the screen gets refreshed.
     * 0(default)=Animated (The turtle will slide from one state to another without being jerky.)
     * 1=State Change (The turtle will refresh immediately to the last state. Jerky motion.)
     * 2=On Demand (The turtle will refresh only when you call update())
     *
     * @param mode refresh mode
     */
    public static void refreshMode(int mode)
    {
        refreshMode=mode;
        updateAll();
    }

    /**
     * This specifies how the background is drawn.
     * 0=The image if present is stretched to fill the screen.
     * 1=The image is centered on the middle of the screen and will not scale/pan
     * 2=The image is tiled and will not scale/pan
     * 3=The image is centered on (0,0) and will scale/pan
     * 4(default)=The image is tiled and will scale/pan
     *
     * @param mode background mode
     */
    public static void backgroundMode(int mode)
    {
        backgroundMode=mode;
        updateAll();
    }

    /**
     * Sets the background color.
     *
     * @param color Color of the background.
     */
    public static void bgcolor(String color)
    {
        Color c=Turtle.getColor(color);
        if(c!=null)backgroundColor=c;
        if(refreshMode!=REFRESH_MODE_ON_DEMAND)updateAll();
    }

    /**
     * Sets the background color.
     *
     * @param color Color of the background.
     */
    public static void bgcolor(Color color)
    {
        backgroundColor=color;
        if(refreshMode!=REFRESH_MODE_ON_DEMAND)updateAll();
    }

    /**
     * Set the background image.
     *
     * @param filename filename for a background image
     */
    public static void bgpic(String filename)
    {
        try{backgroundImage=ImageIO.read(new File(filename));}
        catch(Exception e){e.printStackTrace();}
        if(refreshMode!=REFRESH_MODE_ON_DEMAND)updateAll();
    }

    private static boolean addMouseBinding(String methodName,Turtle t,boolean append,boolean click, boolean repeat)
    {
        String className="";
        try
        {
         throw new Exception("Who called me?");
        }
        catch( Exception e )
        {
            className=e.getStackTrace()[2].getClassName();
        }
        try
        {
            boolean works=false;
            for(Method m : Class.forName(className).getDeclaredMethods())
            {
                if (m.getName().equals(methodName))
                {
                    //System.out.println(m);
                    works=true;
                    for(Class paramType : m.getParameterTypes())
                    {
                        //System.out.println(paramType.getName());
                        if (!paramType.getName().equals("double") && !paramType.getName().equals("java.lang.Double") && !paramType.getName().equals("Turtle"))
                        {
                            works=false;
                            break;
                        }
                    }
                    if (works) break;
                }
            }
            if(works)
            {
                //System.out.println("Method found!");
            }
            else
            {
                System.out.println("ERROR");
                return false;
            }
        }
        catch(Exception e)
        {
            System.out.println("Calling Class not found.");
            return false;
        }
        if(!append || !mouseBindings.containsKey(t)) mouseBindings.put(t,new ArrayList<ArrayList>());
        ArrayList binding=new ArrayList();
        binding.add(t);
        binding.add(className);
        binding.add(methodName);
        binding.add(click);
        binding.add(repeat);
        mouseBindings.get(t).add(binding);
        return true;
    }

    private boolean addKeyBinding(String methodName,String keyText,boolean append,boolean repeat)
    {
        keyText=keyText.toLowerCase();
        String className="";
        try
        {
         throw new Exception("Who called me?");
        }
        catch( Exception e )
        {
            className=e.getStackTrace()[2].getClassName();
        }
        try
        {
            boolean works=false;
            for(Method m : Class.forName(className).getDeclaredMethods())
            {
                if (m.getName().equals(methodName))
                {
                    //System.out.println(m);
                    works=true;
                    for(Class paramType : m.getParameterTypes())
                    {
                        //System.out.println(paramType.getName());
                        if (!paramType.getName().equals("java.lang.String") && !paramType.getName().equals("Turtle"))
                        {
                            works=false;
                            break;
                        }
                    }
                    if (works) break;
                }
            }
            if(works)
            {
                //System.out.println("Method found!");
            }
            else
            {
                System.out.println("ERROR");
                return false;
            }
        }
        catch(Exception e)
        {
            System.out.println("Calling Class not found.");
            return false;
        }
        if(!append || !keyBindings.containsKey(keyText)) keyBindings.put(keyText,new ArrayList<ArrayList>());
        ArrayList binding=new ArrayList();
        binding.add(this);
        binding.add(className);
        binding.add(methodName);
        binding.add(repeat);
        keyBindings.get(keyText).add(binding);
        return true;
    }

    /**
     * Links a method to a key.
     *
     * @param methodName method to be executed when the key is pressed
     * @param keyText key that triggers the method
     * @return
     */
    public boolean onKey(String methodName,String keyText)
    {
        return addKeyBinding(methodName,keyText,false,false);
    }

    /**
     * Links a method to a key.
     *
     * @param methodName method to be executed when the key is pressed
     * @param keyText key that triggers the method
     * @param append true if you want to have multiple methods per key
     * @return
     */
    public boolean onKey(String methodName,String keyText,boolean append)
    {
        return addKeyBinding(methodName,keyText,append,false);
    }

    /**
     * Links a method to a key.
     *
     * @param methodName method to be executed when the key is pressed
     * @param keyText key that triggers the method
     * @param append true if you want to have multiple methods per key
     * @param repeat true if you want call the method every screen refresh
     * @return
     */
    public boolean onKey(String methodName,String keyText,boolean append,boolean repeat)
    {
        return addKeyBinding(methodName,keyText,append,repeat);
    }

    /**
     *
     * Fits the indicated box in the center of the screen as large as possible.
     *
     * @param minx left x coordinate of box
     * @param miny bottom y coordinate of box
     * @param maxx right x coordinate of box
     * @param maxy top y coordinate of box
     */
    public static void zoom(double minx, double miny, double maxx, double maxy)
    {
        synchronized(turtleLock)
        {
            centerX=(minx+maxx)/2;
            centerY=(miny+maxy)/2;
            if (width/(maxx-minx)>height/(maxy-miny)) scale=height/(maxy-miny);
            else scale=width/(maxx-minx);
            updateAll();
        }
    }

    /**
     * Fits everything on the screen.
     */
    public static void zoomFit()
    {
        synchronized(turtleLock)
        {
            Point2D.Double loc;
            if (turtleStates.isEmpty())return;
            else loc=getStateLocation(turtleStates.firstEntry().getValue());
            double minx=loc.x, miny=loc.y;
            double maxx=minx, maxy=miny;
            double shapeWidth=0;
            double shapeHeight=0;
            long time=System.nanoTime();
            if(refreshMode!=REFRESH_MODE_ANIMATED)time=turtleStates.lastKey()+1;
            for (Map.Entry<Long, ArrayList> entry : turtleStates.headMap(time).entrySet())
            {
                    ArrayList state=entry.getValue();
                    if(!getStateIsPenDown(state))continue;
                    Point2D.Double location =getStateLocation(state);
                    if (location.x<minx) minx=location.x;
                    if (location.x>maxx) maxx=location.x;
                    if (location.y<miny) miny=location.y;
                    if (location.y>maxy) maxy=location.y;
                    shapeWidth=getStateShapeWidth(state);
                    shapeHeight=getStateShapeHeight(state);
            }

            if(turtleStates.lastKey()>time && getStateSpeed(turtleStates.lastEntry().getValue())>0)
            {
                double percent = 1 - (turtleStates.lastKey() - time) / getStateSpeed(turtleStates.lastEntry().getValue()) / 1000000.0;
                //System.out.println("trying");
                Turtle t=getStateTurtle(turtleStates.lastEntry().getValue());
                double x1=t._location.x,y1=t._location.y,x2=t.__location.x,y2=t.__location.y;
                x1=(x2-x1)*percent+x1;
                y1=(y2-y1)*percent+y1;
                if (x1<minx) minx=x1;
                if (x1>maxx) maxx=x1;
                if (y1<miny) miny=y1;
                if (y1>maxy) maxy=y1;
            }
            double shapeMax=Math.max(shapeWidth, shapeHeight);
            zoom(minx-shapeMax/2, miny-shapeMax/2, maxx+shapeMax/2, maxy+shapeMax/2);
        }
    }
    
    private static void updateAll()
    {
        lastUpdate=0;
        draw();
    }

    /**
     * Force redraw when the refreshMode is set to on demand.
     */
    public static void update()
    {
        if(refreshMode==REFRESH_MODE_ON_DEMAND) draw();
    }

    private static void draw()
    {
        synchronized(turtleLock)
        {
            
            long renderTime=System.nanoTime();
            if (turtleStates.isEmpty() || lastUpdate==0)
            {
                clearStorage();
                drawBackground(offscreen);
            }
            if(turtleStates.isEmpty())
            {
                onscreen.drawImage(offscreenImage,0,0, null);
                window.repaint();
                if(applet!=null)applet.repaint();
                return;
            }
            if(refreshMode!=REFRESH_MODE_ANIMATED)renderTime=turtleStates.lastKey()+1;
            if (lastUpdate>turtleStates.lastKey())
            {
                midscreen.drawImage(offscreenImage,0,0, null);
                for(Turtle t:turtles)
                {
                    if(t.isVisible) t.drawStamp(1, midscreen);
                    //if(t==selectedTurtle)t.drawCrossHairs(1,midscreen);
                }
                onscreen.drawImage(midscreenImage,0,0, null);
                window.repaint();
                if(applet!=null)applet.repaint();
                return;
            }
            for (Map.Entry<Long, ArrayList> entry : turtleStates.tailMap(lastUpdate).headMap(renderTime).entrySet())
            {
                retrieveState(entry.getKey());
                Turtle t=getStateTurtle(entry.getValue());
                t.drawLine(1,offscreen);
                if(t._isStamp)t.drawStamp(1, offscreen);
                t.drawDot(1, offscreen);
            }
        
            midscreen.drawImage(offscreenImage,0,0, null);
            Turtle animatedTurtle=null;
            double percent=1;
            Long t2;
            t2=Long.valueOf(0);
            if (renderTime<turtleStates.lastKey())
            {
                animatedTurtle=getStateTurtle(turtleStates.ceilingEntry(renderTime).getValue());
                t2=animatedTurtle._time;
                retrieveState(turtleStates.ceilingKey(renderTime));
                if(animatedTurtle._speed>0)
                {
                    percent = 1 - (turtleStates.ceilingKey(renderTime) - renderTime) / animatedTurtle._speed / 1000000.0;
                }
                else percent=1;
                if (percent<0)percent=0;
            }

            for(Turtle t:turtles)
            {
                if(t==animatedTurtle)
                {
                    //System.out.println(percent);
                    t.drawLine(percent, midscreen);
                    if(t._dotSize>0)t.drawDot(percent, midscreen);
                    if(t.isVisible)t.drawStamp(percent, midscreen,false);
                    if(t._isStamp)t.drawStamp(percent, midscreen,true);
                    //if(t==selectedTurtle)t.drawCrossHairs(percent,midscreen);
                    try{retrieveState(t2);}
                    catch(Exception e){}
                }
                else
                {
                    if(t.isVisible)t.drawStamp(1, midscreen);
                    //if(t==selectedTurtle)t.drawCrossHairs(1,midscreen);
                }
                
            }
            lastUpdate=renderTime;
            //zoomFit();
            onscreen.drawImage(midscreenImage,0,0, null);
            window.repaint();
            if(applet!=null)applet.repaint();
        }
        
        
    }

    private void drawLine(double percent,Graphics2D g)
    {
        if(!_isPenDown) return;
        g.setColor(_penColor);
        g.setStroke(new BasicStroke((float)(scale*_penWidth),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        if(__location!=null && !__location.equals(_location))
        {
            double x1=_location.x,y1=_location.y,x2=__location.x,y2=__location.y;
            if(percent<1 && percent>=0)
            {
                x1=(x1-x2)*percent+x2;
                y1=(y1-y2)*percent+y2;
            }
            //g.draw(new Line2D.Double((x1-centerX)*scale+width/2, (y1-centerY)*(-scale)+height/2, (x2-centerX)*scale+width/2, (y2-centerY)*(-scale)+height/2));
            g.drawLine((int)((x1-centerX)*scale+width/2), (int)((y1-centerY)*(-scale)+height/2), (int)((x2-centerX)*scale+width/2), (int)((y2-centerY)*(-scale)+height/2));
        }
    }

    private void drawStamp(double percent,Graphics2D g)
    {
        drawStamp(percent,g,false);
    }

    private void drawStamp(double percent,Graphics2D g,boolean isStamp)
    {
        if (_location==null)return;
        AffineTransform originalTransform=(AffineTransform)g.getTransform().clone();
        AffineTransform m = g.getTransform();
        double x1,x2,y1,y2,dir1,dir2,tilt1,tilt2;
        x1=_location.x;
        y1=_location.y;
        dir1=_direction;
        tilt1=_tilt;
        if(__location==null)
        {
            x2=x1;
            y2=y1;
            dir2=dir1;
            tilt2=tilt1;
        }
        else
        {
            x2=__location.x;
            y2=__location.y;
            dir2=__direction;
            tilt2=__tilt;
        }
        if(percent<1 && percent>=0)
        {
            x1=(x1-x2)*percent+x2;
            y1=(y1-y2)*percent+y2;
            dir1=(dir1-dir2)*percent+dir2;
            tilt1=(tilt1-tilt2)*percent+tilt2;
        }
        m.translate(((x1-centerX)*scale+width/2),((y1-centerY)*(-scale)+height/2));
        if(isStamp)m.scale(scale*percent, scale*percent);
        else m.scale(scale, scale);
        if (_image==null)
        {
            //_outlineWidth=0.0;
            m.rotate(-Math.toRadians(dir1+tilt1));
            m.scale(_shapeWidth/100.0, _shapeHeight/100.0);
            m.translate(-50,-50);
            g.setTransform(m);
            Polygon p =shapes.get(_shape);
            g.setColor(_fillColor);
            g.fillPolygon(p);
            g.setColor(_outlineColor);
            if(_outlineWidth>0)
            {
                g.setStroke(new BasicStroke((float)(_outlineWidth*scale),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                g.setTransform(originalTransform);
                GeneralPath gp=new GeneralPath();
                gp.append(p.getPathIterator(m),false);
                g.draw(gp);
            }
        }
        else
        {
            int w=_image.getWidth();
            int h=_image.getHeight();
            m.rotate(-Math.toRadians(dir1+tilt1));
            m.scale(_shapeWidth/1.0/w, _shapeHeight/1.0/h);
            m.translate(-w/2,-h/2);
            g.setTransform(m);
            g.drawImage(_image,0,0,null);
        }
        g.setTransform(originalTransform);
    }
    
    private void drawDot(double percent,Graphics2D g)
    {
        AffineTransform originalTransform=(AffineTransform)g.getTransform().clone();
        AffineTransform m = g.getTransform();
        m.translate(((_location.x-centerX)*scale+width/2),((_location.y-centerY)*(-scale)+height/2));
        m.scale(scale*percent/2, scale*percent/2);
        g.setTransform(m);
        g.setColor(_dotColor);
        int r=(int)(_dotSize*1.0);
        g.fillOval(-r, -r, 2*r, 2*r);
        g.setTransform(originalTransform);
    }

    private static void drawBackground(Graphics2D g)
    {
        g.setColor(backgroundColor);
        g.fillRect(0, 0, width, height);
        if(backgroundImage==null) return;
        int w=backgroundImage.getWidth();
        int h=backgroundImage.getHeight();
        if(backgroundMode==BACKGROUND_MODE_CENTER)
        {
            offscreen.drawImage(backgroundImage, (width-w)/2, (height-h)/2, w, h, null);
        }
        else if(backgroundMode==BACKGROUND_MODE_STRETCH)
        {
            offscreen.drawImage(backgroundImage, 0, 0, width, height, null);
        }
        else if(backgroundMode==BACKGROUND_MODE_CENTER_RELATIVE)
        {
            offscreen.drawImage(backgroundImage, (int)((-w/2-centerX)*scale+width/2),(int)((h/2-centerY)*(-scale)+height/2), (int)(w*scale), (int)(h*scale), null);
        }
        else if(backgroundMode==BACKGROUND_MODE_TILE)
        {
            for(int i=0;i<width;i+=w) for(int j=0;j<height;j+=h) offscreen.drawImage(backgroundImage, i,j, w, h, null);
        }
        else if(backgroundMode==BACKGROUND_MODE_TILE_RELATIVE)
        {
            double left=centerX-width/2/scale;
            double top=centerY+height/2/scale;
            double right=centerX+width/2/scale;
            double bottom=centerY-height/2/scale;
            for(double x=((int)(left/w)-1)*w;x<=right;x+=w) for(double y=((int)(bottom/h))*h;y<=top+h;y+=h) offscreen.drawImage(backgroundImage, (int)((x-centerX)*scale+width/2),(int)((y-centerY)*(-scale)+height/2), (int)Math.ceil(w*scale), (int)Math.ceil(h*scale), null);
        }
    }

    private void drawCrossHairs(double percent, Graphics2D g)
    {
        if(_location==null)return;
        double time=(System.nanoTime()/100000000)/10.0;
        
        AffineTransform originalTransform=(AffineTransform)g.getTransform().clone();
        AffineTransform m = g.getTransform();
        double x1,x2,y1,y2,dir1,dir2;
        x1=_location.x;
        y1=_location.y;
        if(__location==null)
        {
            x2=x1;
            y2=y1;
        }
        else
        {
            x2=__location.x;
            y2=__location.y;
        }
        if(percent<1 && percent>=0)
        {
            x1=(x1-x2)*percent+x2;
            y1=(y1-y2)*percent+y2;
        }
        m.translate(((x1-centerX)*scale+width/2),((y1-centerY)*(-scale)+height/2));
        int f=10;
        m.scale(scale/f, scale/f);
        g.setTransform(m);
        
        int period=50;
        int r=(int)(Math.sqrt(shapeWidth*shapeWidth+shapeHeight*shapeHeight)*f/2);
        g.setColor(new Color(255,255,255));
        g.setStroke(new BasicStroke((float)(6*f),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        g.drawOval(-r, -r, 2*r, 2*r);
        r+=f;
        for(int i=0;i<4;i++) g.drawLine((int)(r*Math.cos(Math.PI/2*i+2*Math.PI*time/period)),(int)(r*Math.sin(Math.PI/2*i+2*Math.PI*time/period)),
                (int)((r+r/5)*Math.cos(Math.PI/2*i+2*Math.PI*time/period)),(int)((r+r/5)*Math.sin(Math.PI/2*i+2*Math.PI*time/period)));
        r-=f;
        g.setColor(new Color(0,0,0));
        g.setStroke(new BasicStroke((float)(3*f),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        g.drawOval(-r, -r, 2*r, 2*r);
        r+=f;
        for(int i=0;i<4;i++) g.drawLine((int)(r*Math.cos(Math.PI/2*i+2*Math.PI*time/period)),(int)(r*Math.sin(Math.PI/2*i+2*Math.PI*time/period)),
                (int)((r+r/5)*Math.cos(Math.PI/2*i+2*Math.PI*time/period)),(int)((r+r/5)*Math.sin(Math.PI/2*i+2*Math.PI*time/period)));
        g.setTransform(originalTransform);
    }

    /**
     * Changes the size of the canvas effectively changing the size of the window.
     *
     * @param width width of the canvas
     * @param height height of the canvas
     */
    public static void setCanvasSize(int width,int height)
    {
        Turtle.width=width;
        Turtle.height=height;
        Turtle.setupBuffering();
        window.pack();
        updateAll();
    }

    /**
     * Saves the visible canvas to an image.
     *
     * @param filename image filename
     */
    public static void save(String filename)
    {
        save(new File(filename));
    }

    private static void save(File file)
    {
        WritableRaster raster = onscreenImage.getRaster();
        WritableRaster newRaster;
        newRaster = raster.createWritableChild(0, 0, width, height, 0, 0, new int[] {0, 1, 2});
        DirectColorModel cm = (DirectColorModel) onscreenImage.getColorModel();
        DirectColorModel newCM = new DirectColorModel(cm.getPixelSize(),
                                                      cm.getRedMask(),
                                                      cm.getGreenMask(),
                                                      cm.getBlueMask());
        BufferedImage rgbBuffer = new BufferedImage(newCM, newRaster, false,  null);
        try
        {
            String suffix = file.getName().substring(file.getName().lastIndexOf('.') + 1);
            if (!ImageIO.write(rgbBuffer, suffix,file)) throw new Exception("Didn't save file.");
        }
        catch(Exception e)
        {
            file.delete();
            JOptionPane.showMessageDialog(window,
                    "Sorry! We can not process your request at this time.",
                    "Image Save Failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Demo program
     *
     * @param a commandline args
     */
    public static void main(String[] a)
    {
        //Turtle bob = new Turtle();
        /*for(int i=0;i<360;i++)
        {
            bob.forward(i*1.25);
            bob.left(90.25);
        }
         */
        /*If you don't know what a for loop is yet this is equivalent to repeating the middle 4 lines 5 times in a row.*/
        Turtle bob = new Turtle();
        bgcolor("lightblue");
        bob.penColor("red");
        bob.width(10);
        for(int i=0;i<200;i++)
        {
            bob.forward(i/10.);
            bob.left(5);
            if(i%10==0)bob.dot("orange");//Draws dots when i is a multiple of 10.
        }
        bob.saveGCODE("test.gcode");
    }

    /**
     * Internal mehod for handling events.
     * @param e event
     */
    public void actionPerformed(ActionEvent e)
    {
        if(((JMenuItem)e.getSource()).getText().equals("Save..."))
        {
            JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
            chooser.setFileFilter(new FileNameExtensionFilter("Image (*.jpg, *.jpeg, *.gif, *.bmp, *.png)", "jpg", "png", "jpeg", "bmp", "gif"));
            int option = chooser.showSaveDialog(window);
            if(option == JFileChooser.APPROVE_OPTION)
            {
                if(chooser.getSelectedFile()!=null)
                {
                    File file = chooser.getSelectedFile();
                    save(file);
                }
            }
        }
    }

    /**
     * Internal mehod for handling events.
     * @param e event
     */
    public void mouseClicked(MouseEvent e)
    {
        if(e.getModifiers()==8 && e.getClickCount()==2)
        {
            centerX=0;
            centerY=0;
            scale=1;
            updateAll();
        }
    }

    /**
     * Internal mehod for handling events.
     * @param e event
     */
    public void mouseEntered(MouseEvent e) {}

    /**
     * Internal mehod for handling events.
     * @param e event
     */
    public void mouseExited(MouseEvent e) {}

    /**
     * Internal mehod for handling events.
     * @param e event
     */
    public void mousePressed(MouseEvent e)
    {
        dragx=e.getX();
        dragy=e.getY();
        modifiers+=e.getModifiers();
        synchronized (turtleLock) 
        {
            for(Turtle t:turtles)
            {
                if(t.contains(dragx,dragy))t.select();
                else t.unselect();
            }
        }
    }

    /**
     * Internal mehod for handling events.
     * @param e event
     */
    public void mouseReleased(MouseEvent e)
    {
        modifiers-=e.getModifiers();
    }

    /**
     * Internal mehod for handling events.
     * @param e event
     */
    public void mouseDragged(MouseEvent e)
    {
        modifiers=e.getModifiers();
        int dx,dy;
        if (e.getModifiers()==8)
        {
            x=e.getX();
            dx=x-dragx;
            y=e.getY();
            dy=y-dragy;
            dragx=x;
            dragy=y;
            synchronized(turtleLock)
            {
                centerX-=dx*1.0/scale;
                centerY+=dy*1.0/scale;
            }
            updateAll();
        }
        this.x=e.getX();
        this.y=e.getY();
    }

    /**
     * Internal mehod for handling events.
     * @param e event
     */
    public void mouseMoved(MouseEvent e)
    {
        modifiers=e.getModifiers();
        x=e.getX();
        y=e.getY();
        
    }

    /**
     * Internal mehod for handling events.
     * @param e event
     */
    public void keyTyped(KeyEvent e) {}

    /**
     * Internal mehod for handling events.
     * @param e event
     */
    public void keyPressed(KeyEvent e)
    {
        String keyText=KeyEvent.getKeyText(e.getKeyCode()).toLowerCase();
        synchronized (keyLock) 
        {
            keysDown.add(keyText);
            if (keyBindings.containsKey(keyText))
            {
                unprocessedKeys.add(keyText);
            }
        }
    }

    /**
     * Internal mehod for handling events.
     * @param e event
     */
    public void keyReleased(KeyEvent e)
    {
        String keyText=KeyEvent.getKeyText(e.getKeyCode()).toLowerCase();
        synchronized (keyLock) {keysDown.remove(keyText);processedKeys.remove(keyText);}
    }

    private void processKeys()
    {
        //System.out.println(keysDown);
        TreeSet<String> keysDownCopy=new TreeSet<String>();
        synchronized (keyLock) {keysDownCopy=(TreeSet<String>)keysDown.clone();}
        keysDownCopy.addAll(unprocessedKeys);
        for(String keyText:keysDownCopy)
        {
            if (keyBindings.containsKey(keyText))
            {
                for(ArrayList binding:keyBindings.get(keyText))
                {
                    Turtle t=(Turtle)binding.get(0);
                    String className=(String)binding.get(1);
                    String methodName=(String)binding.get(2);
                    Boolean repeat=(Boolean)binding.get(3);
                    if(!repeat && processedKeys.contains(keyText)) break;
                    unprocessedKeys.remove(keyText);
                    processedKeys.add(keyText);
                    try
                    {
                        Class cls = Class.forName(className);
                        Object clsInstance = (Object) cls.newInstance();
                        Method m = clsInstance.getClass().getMethod(methodName, t.getClass());
                        m.invoke(clsInstance, t);
                    }
                    catch(Exception e1)
                    {
                        try
                        {
                            Class cls = Class.forName(className);
                            Object clsInstance = (Object) cls.newInstance();
                            Method m = clsInstance.getClass().getMethod(methodName, t.getClass(), keyText.getClass());
                            m.invoke(clsInstance, t, keyText);
                        }
                        catch(Exception e2)
                        {
                            try
                            {
                                Class cls = Class.forName(className);
                                Object clsInstance = (Object) cls.newInstance();
                                Method m = clsInstance.getClass().getMethod(methodName);
                                m.invoke(clsInstance);
                            }
                            catch(Exception e3) 
                            {
                                System.out.println("KeyBinding for "+keyText+" has failed.");
                                e1.printStackTrace();
                                e2.printStackTrace();
                                e3.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Internal mehod for handling events.
     * @param e event
     */
    public void componentHidden(ComponentEvent e) {}

    /**
     * Internal mehod for handling events.
     * @param e event
     */
    public void componentMoved(ComponentEvent e) {}

    /**
     * Internal mehod for handling events.
     * @param e event
     */
    public void componentResized(ComponentEvent e)
    {
        width=(int)draw.getBounds().getWidth();
        height=(int)draw.getBounds().getHeight();
        setupBuffering();
        updateAll();
    }

    /**
     * Internal mehod for handling events.
     * @param e event
     */
    public void componentShown(ComponentEvent e) {}

    /**
     * Internal mehod for handling events.
     * @param e event
     */
    public void mouseWheelMoved(MouseWheelEvent e)
    {
        int notches = e.getWheelRotation();
        double ds=Math.pow(1.1,notches);
        x=e.getX();
        y=e.getY();
        double dx=width/2-x;
        double dy=height/2-y;
        synchronized(turtleLock)
        {
            centerX-=(dx*ds-dx)/scale/ds;
            centerY+=(dy*ds-dy)/scale/ds;
            scale*=ds;
        }
        updateAll();
    }

    /**
     * Get the pressed keys.
     *
     * @return a list of pressed keys
     */
    public static String[] keysDown()
    {
        return keysDown.toArray(new String[]{});
    }

    /**
     * Test if a key is pressed or not.
     *
     * @param key key you are testing
     * @return true if the key is pressed
     */
    public static boolean isKeyDown(String key)
    {
        return keysDown.contains(key);
    }

     /**
     * Get the mouse x coordinate using the screens coordinate system.
     *
     * @return x coordinate
     */
    public static int mouseX()
    {
        return turtle.x;
    }

    /**
     * Get the mouse y coordinate using the screens coordinate system.
     *
     * @return y coordinate
     */
    public static int mouseY()
    {
        return turtle.y;
    }

    /**
     * Check to see if a  mouse button is down.
     *
     * @return true if a button is down
     */
    public static boolean mouseButton()
    {
        return mouseButton1() || mouseButton2() || mouseButton3();
    }

    /**
     * Check to see if the first mouse button is down.
     *
     * @return true if button 1 is down
     */
    public static boolean mouseButton1()
    {
        return (turtle.modifiers&16)==16;
    }

     /**
     * Check to see if the second mouse button is down.
     *
     * @return true if button 2 is down
     */
    public static boolean mouseButton2()
    {
        return (turtle.modifiers&8)==8;
    }

    /**
     * Check to see if the third mouse button is down.
     *
     * @return true if button 3 is down
     */
    public static boolean mouseButton3()
    {
        return (turtle.modifiers&4)==4;
    }

    /**
     * Converts screen coordinates to canvas coordinates.
     *
     * @param screenX screen x coordinate
     * @return canvas x coordinate
     */
    public static double canvasX(double screenX)
    {
        return (screenX-width/2.0)/scale+centerX;
    }

    /**
     * Converts screen coordinates to canvas coordinates.
     *
     * @param screenY screen y coordinate
     * @return canvas y coordinate
     */
    public static double canvasY(double screenY)
    {
        return (-screenY+height/2.0)/scale+centerY;
    }
    public static double screenX(double canvasX)
    {
        return (canvasX-centerX)*scale+width/2.0;
    }
    public static double screenY(double canvasY)
    {
        return (canvasY-centerY)*scale+height/2.0;
    }
    
    
    private static void saveGCODE(String filename)
    {
		PrintWriter out=new PrintWriter(System.out);
		try
		{
			out=new PrintWriter(filename);
		}
		catch(Exception e)
		{
			
		}
		out.println("M104 S200");
		out.println("M109 S200");
		out.println("G21");
		out.println("G90");
		out.println("M82");
		out.println("M106");
		out.println("G28 X0 Y0");
		out.println("G28 Z0");
		out.println("G29");
		out.println("G1 Z15.0 F9000");
		out.println("G92 E0");
		out.println("G1 F200 E5");
		out.println("G92 E0");
		out.println("G1 X50 Y50 F1800");
		
		double e=0;
		synchronized(turtleLock)
        {
			int i=0;
            for (Map.Entry<Long, ArrayList> entry : turtleStates.entrySet())
            {
                retrieveState(entry.getKey());
                Turtle t=getStateTurtle(entry.getValue());
                i++;
                if(i==1)continue;
                if(t.__location!=null && !t.__location.equals(t._location))
				{
					double x1=t._location.x,y1=t._location.y,x2=t.__location.x,y2=t.__location.y;
					double d=Math.hypot(x1-x2,y1-y2);
					e+=d*0.05;
					//System.out.printf("%f %f %f %f",x1,y1,x2,y2);
					if(t._isPenDown)
					{
						out.printf("G1 X%.4f Y%.4f E%.4f\n",screenX(x1)*1.0/width*100,screenY(y1)*1.0/height*100,e);
					}
					else
					{
						
					}
						
				}
			}
			out.println("G1 Z15");
			out.println("M104 S0");
			out.println("M140 S0");
			out.close();
		}

        
    }
}