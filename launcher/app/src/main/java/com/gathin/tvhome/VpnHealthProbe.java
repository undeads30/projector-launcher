package com.gathin.tvhome;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/** Never falls back to a direct connection. DNS resolution is delegated to the SOCKS core. */
final class VpnHealthProbe {
    static boolean isSuccess(String line) {
        return line.trim().matches("HTTP/1\\.[01] 204(?: .*)?");
    }
    static boolean reachable() {
        return check("www.google.com", "/generate_204")
                || check("www.youtube.com", "/generate_204");
    }
    private static boolean check(String host, String path) {
        Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", 10808));
        try (Socket socket = new Socket(proxy)) {
            socket.setSoTimeout(2500);
            socket.connect(InetSocketAddress.createUnresolved(host, 443), 2500);
            try (SSLSocket tls = (SSLSocket) ((SSLSocketFactory) SSLSocketFactory.getDefault())
                    .createSocket(socket, host, 443, true)) {
                tls.setSoTimeout(2500);
                SSLParameters parameters = tls.getSSLParameters();
                parameters.setEndpointIdentificationAlgorithm("HTTPS");
                tls.setSSLParameters(parameters);
                tls.startHandshake();
                tls.getOutputStream().write(("GET " + path + " HTTP/1.1\r\nHost: " + host
                        + "\r\nConnection: close\r\nCache-Control: no-cache\r\n\r\n")
                        .getBytes(StandardCharsets.US_ASCII));
                StringBuilder line = new StringBuilder();
                for (int i = 0; i < 256; i++) {
                    int ch = tls.getInputStream().read();
                    if (ch < 0 || ch == '\n') break;
                    line.append((char) ch);
                }
                return isSuccess(line.toString());
            }
        } catch (Exception ignored) { return false; }
    }
}
