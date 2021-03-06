package com.googlecode.utterlyidle.handlers;

import com.googlecode.totallylazy.functions.Functions;
import com.googlecode.utterlyidle.Entity;
import com.googlecode.utterlyidle.HttpHandler;
import com.googlecode.utterlyidle.Request;
import com.googlecode.utterlyidle.Response;
import com.googlecode.utterlyidle.annotations.HttpMethod;
import com.googlecode.utterlyidle.bindings.BindingMatcher;

public class HeadRequestHandler implements HttpHandler {
    private final HttpHandler decorated;
    private final BindingMatcher matcher;

    public HeadRequestHandler(HttpHandler decorated, BindingMatcher matcher) {
        this.decorated = decorated;
        this.matcher = matcher;
    }

    @Override
    public Response handle(Request request) throws Exception {
        if (request.method().equals(HttpMethod.HEAD) && matcher.match(request).isLeft()) {
            Response response = decorated.handle(Functions.modify(request, Request.Builder.method(HttpMethod.GET)));
            return response.entity(Entity.empty());
        }
        return decorated.handle(request);
    }

}
