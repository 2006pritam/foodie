package com.foodie.web;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Makes page URLs end in ".jsp".
 *
 * <p>The app is a clean-URL front controller ({@link FoodieServlet} switches on
 * paths like {@code /menu}). This filter redirects an extension-less page
 * {@code GET} to its {@code .jsp} form (e.g. {@code /menu} -&gt; {@code /menu.jsp}),
 * so the address bar shows {@code .jsp} without touching any of the existing
 * links or post-redirects. The servlet strips the {@code .jsp} suffix again
 * before routing, so every existing route keeps working.</p>
 *
 * <p>Left untouched: non-GET requests (form posts, the {@code /chat} AJAX call),
 * anything that already carries an extension (static assets, {@code *.jsp}), and
 * the site root.</p>
 */
public class JspUrlFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        if (shouldAppendJsp(req)) {
            String query = req.getQueryString();
            String target = req.getContextPath() + req.getServletPath() + ".jsp"
                    + (query != null ? "?" + query : "");
            res.sendRedirect(target);
            return;
        }

        chain.doFilter(request, response);
    }

    /** Only plain page GETs with no existing extension get rewritten to *.jsp. */
    private boolean shouldAppendJsp(HttpServletRequest req) {
        if (!"GET".equalsIgnoreCase(req.getMethod())) {
            return false;
        }
        String path = req.getServletPath();
        if (path == null || path.isEmpty() || "/".equals(path)) {
            return false; // leave the site root alone
        }
        // Last segment already carries a "." (static asset, or already .jsp) -> leave it.
        String lastSegment = path.substring(path.lastIndexOf('/') + 1);
        return !lastSegment.isEmpty() && lastSegment.indexOf('.') < 0;
    }
}
