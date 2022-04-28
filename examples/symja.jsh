//DEPS org.matheclipse:matheclipse-core:2.0.0,org.matheclipse:matheclipse-io:2.0.0
// below is copied from https://github.com/axkr/symja_android_library/blob/master/symja_android_library/start-symja.jsh
import java.io.*;
import java.math.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.prefs.*;
import java.util.regex.*;
import java.util.stream.*;
import org.matheclipse.io.eval.*;
import org.matheclipse.core.expression.*;
import org.matheclipse.core.eval.*;
import org.matheclipse.core.interfaces.*;
import static org.matheclipse.core.expression.F.*;

ExprEvaluator ev = new ExprEvaluator(); 
