package org.ccci.gto.cas.client;

import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_OK;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestClient {
    private static final Logger LOG = LoggerFactory.getLogger(RestClient.class);

    private final String casServerUrl;

    public RestClient(final String casServerUrl) {
        this.casServerUrl = casServerUrl + (casServerUrl.endsWith("/") ? "" : "/");
    }

    private String getRestUrl() {
        return casServerUrl + "v1/tickets";
    }

    public String getTicketGrantingTicket(final String username, final String password) {
        URLConnection conn = null;
        try {
            conn = new URL(getRestUrl()).openConnection();
            if (!(conn instanceof HttpURLConnection)) {
                LOG.error("RestUrl is not an Http url");
                return null;
            }

            // generate POST request
            ((HttpURLConnection) conn).setRequestMethod("POST");
            conn.setDoOutput(true);
            final BufferedWriter out = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
            out.write("username=" + URLEncoder.encode(username, "UTF-8") + "&password="
                    + URLEncoder.encode(password, "UTF-8"));
            out.flush();
            out.close();

            // extract the tgt from the Location header if this was a successful
            // request
            if (((HttpURLConnection) conn).getResponseCode() == HTTP_CREATED) {
                final String location = conn.getHeaderField("Location");
                return location.substring(location.lastIndexOf("/") + 1);
            }
        } catch (final Exception e) {
            LOG.error("error fetching a TicketGrantingTicket", e);
        } finally {
            if (conn != null && conn instanceof HttpURLConnection) {
                ((HttpURLConnection) conn).disconnect();
            }
        }

        // return null since no tgt was found
        return null;
    }

    public String getTicket(final String tgt, final String serviceUrl) {
        URLConnection conn = null;
        try {
            conn = new URL(getRestUrl() + "/" + tgt).openConnection();
            if (!(conn instanceof HttpURLConnection)) {
                LOG.error("RestUrl is not an Http url");
                return null;
            }

            // generate POST request
            ((HttpURLConnection) conn).setRequestMethod("POST");
            conn.setDoOutput(true);
            final BufferedWriter out = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
            out.write("service=" + URLEncoder.encode(serviceUrl, "UTF-8"));
            out.flush();
            out.close();

            // extract the st from the response
            if (((HttpURLConnection) conn).getResponseCode() == HTTP_OK) {
                final BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                final String ticket = in.readLine();
                in.close();
                return ticket;
            }
        } catch (final Exception e) {
            LOG.error("error fetching a ServiceTicket", e);
        } finally {
            if (conn != null && conn instanceof HttpURLConnection) {
                ((HttpURLConnection) conn).disconnect();
            }
        }

        return null;
    }
}
