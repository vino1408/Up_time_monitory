
package com.vinoth.uptimemonitor.service;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

public class MonitorUrlValidator {

    @FunctionalInterface
    interface HostResolver {
        InetAddress[] resolve(String host)
                throws java.net.UnknownHostException;
    }

    private final HostResolver hostResolver;

    public MonitorUrlValidator() {
        this(InetAddress::getAllByName);
    }

    MonitorUrlValidator(HostResolver hostResolver) {
        this.hostResolver = hostResolver;
    }

    public void validate(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException(
                    "Monitor URL is required.");
        }

        final URI uri;

        try {
            uri = new URI(url.trim());
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException(
                    "Invalid monitor URL format.");
        }

        String scheme = uri.getScheme();
        String host = uri.getHost();

        if (scheme == null
                || !(scheme.equalsIgnoreCase("http")
                || scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException(
                    "Only HTTP and HTTPS URLs are allowed.");
        }

        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException(
                    "Monitor URL must contain a valid hostname.");
        }

        if (uri.getUserInfo() != null) {
            throw new IllegalArgumentException(
                    "URLs containing embedded credentials are not allowed.");
        }

        String normalizedHost = host.toLowerCase(Locale.ROOT);

        if (normalizedHost.equals("localhost")
                || normalizedHost.endsWith(".localhost")
                || normalizedHost.endsWith(".local")
                || normalizedHost.endsWith(".internal")) {
            throw new IllegalArgumentException(
                    "Local and internal hostnames are not allowed.");
        }

        try {
            InetAddress[] addresses =
                    hostResolver.resolve(normalizedHost);

            if (addresses == null || addresses.length == 0) {
                throw new IllegalArgumentException(
                        "Monitor hostname returned no IP addresses.");
            }

            for (InetAddress address : addresses) {
                if (address == null || isBlockedAddress(address)) {
                    throw new IllegalArgumentException(
                            "Private, local, and special IP addresses "
                                    + "are not allowed.");
                }
            }
        } catch (java.net.UnknownHostException exception) {
            throw new IllegalArgumentException(
                    "Monitor hostname could not be resolved.");
        }
    }

    private boolean isBlockedAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }

        byte[] bytes = address.getAddress();

        if (bytes.length == 4) {
            int first = bytes[0] & 0xFF;
            int second = bytes[1] & 0xFF;
            int third = bytes[2] & 0xFF;

            if (first == 0 || first == 10 || first == 127
                    || (first == 100 && second >= 64 && second <= 127)
                    || (first == 169 && second == 254)
                    || (first == 172 && second >= 16 && second <= 31)
                    || (first == 192 && second == 168)) {
                return true;
            }

            return first >= 224
                    || (first == 192 && second == 0)
                    || (first == 192 && second == 2)
                    || (first == 198 && (second == 18 || second == 19))
                    || (first == 198 && second == 51 && third == 100)
                    || (first == 203 && second == 0 && third == 113);
        }

        if (bytes.length == 16) {
            int first = bytes[0] & 0xFF;
            int second = bytes[1] & 0xFF;

            if ((first & 0xFE) == 0xFC
                    || (first == 0xFE && (second & 0xC0) == 0x80)
                    || (first == 0x20 && second == 0x01
                        && (bytes[2] & 0xFF) == 0x0D
                        && (bytes[3] & 0xFF) == 0xB8)) {
                return true;
            }

            boolean ipv4Mapped = true;

            for (int i = 0; i < 10; i++) {
                if (bytes[i] != 0) {
                    ipv4Mapped = false;
                    break;
                }
            }

            if (ipv4Mapped
                    && bytes[10] == (byte) 0xFF
                    && bytes[11] == (byte) 0xFF) {
                try {
                    byte[] ipv4 = {
                        bytes[12], bytes[13], bytes[14], bytes[15]
                    };

                    return isBlockedAddress(
                            InetAddress.getByAddress(ipv4));
                } catch (java.net.UnknownHostException exception) {
                    return true;
                }
            }

            return false;
        }

        return true;
    }
}
