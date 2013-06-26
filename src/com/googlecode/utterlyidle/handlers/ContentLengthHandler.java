package com.googlecode.utterlyidle.handlers;

import com.googlecode.utterlyidle.Entity;
import com.googlecode.utterlyidle.HeaderParameters;
import com.googlecode.utterlyidle.HttpHandler;
import com.googlecode.utterlyidle.Request;
import com.googlecode.utterlyidle.Response;

import static com.googlecode.totallylazy.numbers.Numbers.greaterThan;
import static com.googlecode.totallylazy.numbers.Numbers.greaterThanOrEqualTo;
import static com.googlecode.utterlyidle.HttpHeaders.CONTENT_LENGTH;
import static com.googlecode.utterlyidle.Response.functions.replaceHeader;

public class ContentLengthHandler implements HttpHandler {
    private final HttpHandler httpHandler;

    public ContentLengthHandler(HttpHandler httpHandler) {
        this.httpHandler = httpHandler;
    }

    @Override
    public Response handle(Request request) throws Exception {
        return setContentLength(httpHandler.handle(request));
    }

    public static Response setContentLength(Response response) {
        return response.entity().length().fold(response, replaceHeader(CONTENT_LENGTH));
    }

    public static HeaderParameters setContentLength(Entity entity, HeaderParameters headers) {
        if (entity.length().is(greaterThan(0))) {
            return headers.replace(CONTENT_LENGTH, String.valueOf(entity.length().get()));
        }
        return headers;
    }
}