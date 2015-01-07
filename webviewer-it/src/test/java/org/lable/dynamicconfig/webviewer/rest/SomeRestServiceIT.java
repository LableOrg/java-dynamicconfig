package org.lable.dynamicconfig.webviewer.rest;


import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class SomeRestServiceIT {
    private final static String baseURL = "http://localhost:8080/api/config/test";

    @Ignore
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

            String reply = EntityUtils.toString(entity);

            assertThat(reply, is("ZZZ"));
            assertThat(response.getStatusLine().getStatusCode(), is(200));
        }
    }
}
