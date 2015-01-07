package org.lable.dynamicconfig.webviewer.servlet;

import org.apache.commons.configuration.Configuration;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Singleton
public class SomeServlet extends HttpServlet {

    private Configuration config;

    @Inject
    public SomeServlet(Configuration config) {
        this.config = config;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String key = getFirstPathPart(request.getPathInfo());
        if (key == null) {
            response.sendError(404, "Nope.");
            return;
        }

        String value = config.getString(key);

        response.setContentType("text/plain; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().println(value);
    }

    static String getFirstPathPart(String pathInfo) {
        if (pathInfo == null) {
            return null;
        }

        if (!pathInfo.startsWith("/") || pathInfo.length() < 2) {
            return null;
        }

        return pathInfo.substring(1);
    }
}
