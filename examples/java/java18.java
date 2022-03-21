///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 18+
//FILES META-INF/services/java.net.spi.InetAddressResolverProvider=java.net.spi.InetAddressResolverProvider
//SOURCES *.java
package java18;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * {@snippet :
 * try (BufferedWriter writer = Files.newBufferedWriter(path)) {
 *   writer.write(text);  // @highlight substring="text"
 * }
 * }
 */
public class java18 {

    public static void main(String... args) throws UnknownHostException {
        String host = "www.google.com";
        InetAddress[] addresses = InetAddress.getAllByName(host);
        System.out.println("addresses for " + host + ": " + Arrays.toString(addresses));
    }
}
