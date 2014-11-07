package me.thekey.cas.client;

import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_OK;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

public class RestClient {
    private static final Logger LOG = LoggerFactory.getLogger(RestClient.class);

    private static final int DEFAULT_CONNECT_TIMEOUT = 2000;
    private static final int DEFAULT_READ_TIMEOUT = 10000;

    private final String casServerUrl;

    private int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    private int readTimeout = DEFAULT_READ_TIMEOUT;

    public RestClient(final String casServerUrl) {
        this.casServerUrl = casServerUrl + (casServerUrl.endsWith("/") ? "" : "/");
    }

    public void setConnectTimeout(final int timeout) {
        this.connectTimeout = timeout;
    }

    public void setReadTimeout(final int timeout) {
        this.readTimeout = timeout;
    }

    public void setTimeout(final int timeout) {
        this.connectTimeout = timeout;
        this.readTimeout = timeout;
    }

    private String getRestUrl() {
        return casServerUrl + "v1/tickets";
    }

    public String getTicketGrantingTicket(final String username, final String password) {
        HttpURLConnection conn = null;
        try {
            conn = this.openHttpConnection(new URL(getRestUrl()));
            if (conn == null) {
                return null;
            }

            // generate POST request
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()))) {
                out.write("username=" + URLEncoder.encode(username, "UTF-8") + "&password=" + URLEncoder.encode
                        (password, "UTF-8"));
            }

            // extract the tgt from the Location header if this was a successful request
            if (conn.getResponseCode() == HTTP_CREATED) {
                final String location = conn.getHeaderField("Location");
                return location.substring(location.lastIndexOf("/") + 1);
            }
        } catch (final Exception e) {
            LOG.error("error fetching a TicketGrantingTicket", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        // return null since no tgt was found
        return null;
    }

    public String getTicket(final String tgt, final String serviceUrl) {
        HttpURLConnection conn = null;
        try {
            conn = this.openHttpConnection(new URL(getRestUrl() + "/" + tgt));
            if (conn == null) {
                return null;
            }

            // generate POST request
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()))) {
                out.write("service=" + URLEncoder.encode(serviceUrl, "UTF-8"));
            }

            // extract the st from the response
            if (conn.getResponseCode() == HTTP_OK) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    return in.readLine();
                }
            }
        } catch (final Exception e) {
            LOG.error("error fetching a ServiceTicket", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        return null;
    }

    private HttpURLConnection openHttpConnection(final URL url) throws IOException {
        // create HttpURLConnection object
        final URLConnection conn = url.openConnection();
        if (!(conn instanceof HttpURLConnection)) {
            LOG.error("'{}' is not an Http url", url);
            return null;
        }

        // set connection timeout values
        conn.setConnectTimeout(this.connectTimeout);
        conn.setReadTimeout(this.readTimeout);

        // return connection object
        return (HttpURLConnection) conn;
    }
}
