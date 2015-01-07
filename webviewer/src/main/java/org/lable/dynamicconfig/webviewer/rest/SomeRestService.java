package org.lable.dynamicconfig.webviewer.rest;

import com.google.inject.Inject;
import org.apache.commons.configuration.Configuration;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("config")
public class SomeRestService {

    private Configuration config;

    @Inject
    public SomeRestService(Configuration config) {
        this.config = config;
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getConfigKey() {
        String value = "";
        return Response.status(200).entity(value).build();
    }
}
