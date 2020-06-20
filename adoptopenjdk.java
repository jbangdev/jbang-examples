//usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS net.adoptopenjdk:net.adoptopenjdk.v3.api:0.3.3
//DEPS net.adoptopenjdk:net.adoptopenjdk.v3.vanilla:0.3.3


import net.adoptopenjdk.v3.api.AOV3Architecture;
import net.adoptopenjdk.v3.api.AOV3Exception;
import net.adoptopenjdk.v3.api.AOV3HeapSize;
import net.adoptopenjdk.v3.api.AOV3ImageKind;
import net.adoptopenjdk.v3.api.AOV3JVMImplementation;
import net.adoptopenjdk.v3.api.AOV3OperatingSystem;
import net.adoptopenjdk.v3.api.AOV3ReleaseKind;
import net.adoptopenjdk.v3.api.AOV3Vendor;
import net.adoptopenjdk.v3.vanilla.AOV3Clients;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Optional;

class adoptopenjdk {

    public static void main(String... args) throws Exception {
        var clients = new AOV3Clients();
        try (var client = clients.createClient()) {
            var result = client.binaryForLatest(e -> System.out.println(e),
                    AOV3Architecture.of(arch()),
                    BigInteger.valueOf(11),
                    AOV3HeapSize.NORMAL,
                    AOV3ImageKind.JDK,
                    AOV3JVMImplementation.HOTSPOT,
                    AOV3OperatingSystem.of(os()),
                    AOV3ReleaseKind.GENERAL_AVAILABILITY,
                    AOV3Vendor.ADOPT_OPENJDK, Optional.of("jdk")).execute();
            System.out.println(result);
        }
    }

    static String arch() {
        String arch = System.getProperty("os.arch");

        if(arch.contains("32")) {
            return "x32";
        } else if(arch.contains("64")) {
            return "x64";
        } else {
            return arch;
        }

    }
    static String os() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return "windows";
        } else if (os.contains("mac")) {
            return "mac";
        } else if (os.contains("linux")) {
            return "linux";
        } else {
            return os;
        }
    }
}