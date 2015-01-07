package org.lable.dynamicconfig.webviewer.servlet;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class SomeServletIT {
    private final static String baseURL = "http://localhost:8080/servlets/config/test";

    @Test
    public void testOK() throws IOException {
        // Simple smoke-test.
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(baseURL);
        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                fail();
            }
            System.out.println(EntityUtils.toString(entity));

            assertThat(response.getStatusLine().getStatusCode(), is(200));
        }
    }
}
