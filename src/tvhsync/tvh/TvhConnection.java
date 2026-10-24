/*
 * Copyright (c) Henrik Niehaus
 * Copyright (c) 2026 Christian Echterling
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of the project (Lazy Bones) nor the names of its
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package tvhsync.tvh;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tvhsync.TVHsync;
import tvhsync.logging.LoggingConstants;

/**
 * Sends requests to the HTTP API of tvheadend
 */
public class TvhConnection {

    private static Logger logger = LoggerFactory.getLogger(LoggingConstants.CONNECTION_LOGGER);

    /** base URL of the tvheadend web interface, e.g. http://localhost:9981 */
    public static String url = "http://localhost:9981"; // NOSONAR

    public static String user = ""; // NOSONAR

    public static String password = ""; // NOSONAR

    /** connect and read timeout in ms */
    public static int timeout = 5000; // NOSONAR

    private TvhConnection() {}

    public static Response get(String path, Map<String, ?> params) {
        String query = encode(params);
        return send("GET", path + (query.isEmpty() ? "" : "?" + query), null);
    }

    public static Response post(String path, Map<String, ?> params) {
        return send("POST", path, encode(params));
    }

    private static Response send(String method, String path, String form) {
        String requestUrl = getBaseUrl() + path;
        logger.debug("{} {} {}", method, requestUrl, form != null ? form : "");
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) new URL(requestUrl).openConnection();
            con.setRequestMethod(method);
            con.setConnectTimeout(timeout);
            con.setReadTimeout(timeout * 4);
            con.setRequestProperty("Accept", "application/json");
            if (user != null && !user.isEmpty()) {
                // works for basic and digest authentication
                con.setAuthenticator(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(user, password == null ? new char[0] : password.toCharArray());
                    }
                });
            }
            if (form != null) {
                // no streaming mode here, otherwise the request can't be repeated for digest authentication
                byte[] data = form.getBytes(StandardCharsets.UTF_8);
                con.setDoOutput(true);
                con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                try (OutputStream out = con.getOutputStream()) {
                    out.write(data);
                }
            }

            int code = con.getResponseCode();
            InputStream in = code >= 400 ? con.getErrorStream() : con.getInputStream();
            String content = in != null ? readFully(in) : "";
            logger.trace("Response {}: {}", code, content);

            if (code >= 200 && code < 300) {
                Object body = null;
                String trimmed = content.trim();
                if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                    body = Json.parse(trimmed);
                }
                return new Response(code, con.getResponseMessage(), body);
            } else {
                String msg = code == 401 || code == 403 ? TVHsync.getTranslation("access_denied", "Access denied. Check user name and password.")
                        : "HTTP " + code + " " + con.getResponseMessage();
                logger.warn("Request {} {} failed: {}", method, path, msg);
                return new Response(code, msg);
            }
        } catch (Exception e) {
            String msg = TVHsync.getTranslation("couldnt_connect", "Couldn't connect to tvheadend") + ": " + e.getLocalizedMessage();
            logger.error(msg, e);
            return new Response(0, msg);
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }

    private static String readFully(InputStream in) throws IOException {
        try (InputStream is = in) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int length;
            while ((length = is.read(buffer)) > 0) {
                bos.write(buffer, 0, length);
            }
            return new String(bos.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static String encode(Map<String, ?> params) {
        if (params == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Entry<String, ?> entry : params.entrySet()) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            Object value = entry.getValue();
            String stringValue = value instanceof Map || value instanceof Iterable ? Json.stringify(value) : String.valueOf(value);
            sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            sb.append('=');
            sb.append(URLEncoder.encode(stringValue, StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    public static String getBaseUrl() {
        String base = url == null ? "" : url.trim();
        // the user may have copied the URL of the web interface
        if (base.endsWith("/extjs.html")) {
            base = base.substring(0, base.length() - "/extjs.html".length());
        }
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }

    /**
     * @return the base URL including the credentials, so that external players can authenticate
     */
    public static String getBaseUrlWithCredentials() {
        String base = getBaseUrl();
        if (user == null || user.isEmpty()) {
            return base;
        }
        int schemeEnd = base.indexOf("://");
        if (schemeEnd < 0) {
            return base;
        }
        String credentials = URLEncoder.encode(user, StandardCharsets.UTF_8) + ':' + URLEncoder.encode(password == null ? "" : password, StandardCharsets.UTF_8);
        return base.substring(0, schemeEnd + 3) + credentials + '@' + base.substring(schemeEnd + 3);
    }
}
