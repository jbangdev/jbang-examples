package java18;

import java18.java18;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.spi.InetAddressResolver;
import java.net.spi.InetAddressResolverProvider;
import java.util.stream.Stream;

public class J18InetAddressResolverProvider extends InetAddressResolverProvider {
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
