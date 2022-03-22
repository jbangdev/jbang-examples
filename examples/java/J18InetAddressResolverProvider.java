///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 18+
//FILES META-INF/services/java.net.spi.InetAddressResolverProvider=java.net.spi.InetAddressResolverProvider
package java18;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.spi.InetAddressResolver;
import java.net.spi.InetAddressResolverProvider;
import java.util.Arrays;
import java.util.stream.Stream;

public class J18InetAddressResolverProvider extends InetAddressResolverProvider {

    public static void main(String... args) throws UnknownHostException {
        String host = args.length > 0 ? args[1] : "www.jbang.dev";
        InetAddress[] addresses = InetAddress.getAllByName(host);
        System.out.println("addresses for " + host + ": " + Arrays.toString(addresses));
    }

    @Override
    public InetAddressResolver get(Configuration configuration) {
        return new J18InetAddressResolver();
    }

    @Override
    public String name() {
        return "JBang Internet Address Resolver Provider";
    }

    static public class J18InetAddressResolver implements InetAddressResolver {
        @Override
        public Stream<InetAddress> lookupByName(String host, LookupPolicy lookupPolicy)
                throws UnknownHostException {
            return Stream.of(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}));
        }

        @Override
        public String lookupByAddress(byte[] addr) {
            throw new UnsupportedOperationException();
        }
    }
}
