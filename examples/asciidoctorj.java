///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS org.asciidoctor:asciidoctorj:3.0.0
//DEPS org.asciidoctor:asciidoctorj-api:3.0.0
//DEPS org.asciidoctor:asciidoctorj-cli:3.0.0
//DEPS org.asciidoctor:asciidoctorj-epub3:2.1.3
//DEPS org.asciidoctor:asciidoctorj-diagram:3.0.1
//DEPS org.asciidoctor:asciidoctorj-diagram-batik:1.17
//DEPS org.asciidoctor:asciidoctorj-diagram-ditaamini:1.0.3
//DEPS org.asciidoctor:asciidoctorj-diagram-plantuml:1.2025.3
//DEPS org.asciidoctor:asciidoctorj-diagram-jsyntrax:1.38.2
//DEPS org.asciidoctor:asciidoctorj-pdf:2.3.19
//DEPS org.asciidoctor:asciidoctorj-revealjs:5.2.0
//DEPS com.beust:jcommander:1.82
//DEPS org.jruby:jruby-complete:9.4.8.0

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class asciidoctorj {

    static String REVEALJSDIR = "https://cdn.jsdelivr.net/npm/reveal.js@5.2.0";

    public static void main(String[] args) throws IOException {
        // Check if revealjsdir is specified in the arguments
        // using template "-a revealjsdir=path/to/revealjs"
        String revealjsdir = null;
        boolean revealjsBackendEnabled = false;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-a")) {
                if (i + 1 < args.length) {
                    String attribute = args[i + 1];
                    if (attribute.startsWith("revealjsdir=")) {
                        if (revealjsdir == null) {
                            revealjsdir = attribute;
                        }
                    }
                    i++; // Skip the next argument
                }
            } else if (arg.equals("-b")) {
                if (i + 1 < args.length) {
                    String attribute = args[i+1];
                    revealjsBackendEnabled = attribute.equals("revealjs");
                    i++; // Skip the next argument
                }
            }
        }

        // If revealjsdir is required but not specified, set it to the default value
        String[] argz = args;
        if (revealjsBackendEnabled) {
            if (revealjsdir == null) {
                argz = new String[args.length + 2];
                System.arraycopy(args, 0, argz, 0, args.length);
                argz[args.length] = "-a";
                argz[args.length + 1] = "revealjsdir=" + REVEALJSDIR;
            }
        }

        boolean debugEnabled = false;
        if (debugEnabled) {
            System.out.println("Using revealjsdir: " + (revealjsdir != null ? revealjsdir : REVEALJSDIR));
            System.out.println("Arguments: " + Arrays.toString(argz));
        }

        // Call the Asciidoctor CLI main method with the modified arguments
        org.asciidoctor.cli.jruby.AsciidoctorInvoker.main(argz);
    }
}