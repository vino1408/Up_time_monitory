
package com.vinoth.uptimemonitor.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.jupiter.api.Test;

class MonitorUrlValidatorTest {

    private InetAddress address(String ip)
            throws UnknownHostException {
        return InetAddress.getByAddress(
                new byte[] {
                    (byte) Integer.parseInt(ip.split("\\.")[0]),
                    (byte) Integer.parseInt(ip.split("\\.")[1]),
                    (byte) Integer.parseInt(ip.split("\\.")[2]),
                    (byte) Integer.parseInt(ip.split("\\.")[3])
                });
    }

    @Test
    void acceptsPublicAddressWithoutDnsLookup() throws Exception {
        InetAddress publicAddress = address("93.184.216.34");

        MonitorUrlValidator validator =
                new MonitorUrlValidator(host -> new InetAddress[] {
                    publicAddress
                });

        assertDoesNotThrow(
                () -> validator.validate("https://example.test"));
    }

    @Test
    void rejectsHostnameResolvingToLoopback() throws Exception {
        InetAddress loopback = address("127.0.0.1");

        MonitorUrlValidator validator =
                new MonitorUrlValidator(host -> new InetAddress[] {
                    loopback
                });

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate("https://example.test"));
    }

    @Test
    void rejectsHostnameResolvingToPrivateAddress() throws Exception {
        InetAddress privateAddress = address("192.168.1.10");

        MonitorUrlValidator validator =
                new MonitorUrlValidator(host -> new InetAddress[] {
                    privateAddress
                });

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate("https://example.test"));
    }

    @Test
    void rejectsHostnameWithMixedPublicAndPrivateAddresses()
            throws Exception {
        InetAddress publicAddress = address("93.184.216.34");
        InetAddress privateAddress = address("10.0.0.10");

        MonitorUrlValidator validator =
                new MonitorUrlValidator(host -> new InetAddress[] {
                    publicAddress, privateAddress
                });

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate("https://example.test"));
    }

    @Test
    void rejectsLocalhostWithoutDnsLookup() {
        MonitorUrlValidator validator =
                new MonitorUrlValidator(host -> {
                    throw new AssertionError(
                            "Localhost must be rejected before DNS lookup.");
                });

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate("http://localhost"));
    }

    @Test
    void rejectsFileScheme() {
        MonitorUrlValidator validator =
                new MonitorUrlValidator(host -> new InetAddress[0]);

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate("file:///etc/passwd"));
    }

    @Test
    void rejectsEmbeddedCredentials() {
        MonitorUrlValidator validator =
                new MonitorUrlValidator(host -> new InetAddress[0]);

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(
                        "https://user:pass@example.test"));
    }

    @Test
    void rejectsEmptyDnsResult() {
        MonitorUrlValidator validator =
                new MonitorUrlValidator(host -> new InetAddress[0]);

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate("https://example.test"));
    }
}
