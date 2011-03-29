package com.googlecode.utterlyidle;

import com.googlecode.totallylazy.Callable1;
import com.googlecode.totallylazy.Either;
import com.googlecode.totallylazy.Option;
import com.googlecode.utterlyidle.cookies.CookieParameters;
import com.googlecode.utterlyidle.handlers.ExceptionHandler;
import com.googlecode.utterlyidle.handlers.ResponseHandlers;
import com.googlecode.utterlyidle.io.HierarchicalPath;
import com.googlecode.utterlyidle.io.Url;
import com.googlecode.utterlyidle.modules.ApplicationScopedModule;
import com.googlecode.utterlyidle.modules.ArgumentScopedModule;
import com.googlecode.utterlyidle.modules.CoreModule;
import com.googlecode.utterlyidle.modules.Module;
import com.googlecode.utterlyidle.modules.RequestScopedModule;
import com.googlecode.utterlyidle.modules.ResourcesModule;
import com.googlecode.utterlyidle.modules.ResponseHandlersModule;
import com.googlecode.yadic.Container;
import com.googlecode.yadic.Resolver;
import com.googlecode.yadic.SimpleContainer;
import com.googlecode.yadic.generics.TypeFor;
import com.googlecode.yadic.resolvers.OptionResolver;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.googlecode.totallylazy.Closeables.using;
import static com.googlecode.totallylazy.Predicates.instanceOf;
import static com.googlecode.totallylazy.Sequences.sequence;
import static com.googlecode.utterlyidle.modules.Modules.addPerApplicationObjects;
import static com.googlecode.utterlyidle.modules.Modules.addPerArgumentObjects;
import static com.googlecode.utterlyidle.modules.Modules.addPerRequestObjects;
import static com.googlecode.utterlyidle.modules.Modules.addResources;
import static com.googlecode.utterlyidle.modules.Modules.addResponseHandlers;

public class RestApplication implements Application {
    private final Container applicationScope = new SimpleContainer();
    private final List<Module> modules = new ArrayList<Module>();

    public RestApplication() {
        applicationScope.addInstance(Application.class, this);
        add(new CoreModule());
    }

    public Application add(Module module) {
        sequence(module).safeCast(ApplicationScopedModule.class).forEach(addPerApplicationObjects(applicationScope));
        sequence(module).safeCast(ResourcesModule.class).forEach(addResources(applicationScope.get(Resources.class)));
        sequence(module).safeCast(ResponseHandlersModule.class).forEach(addResponseHandlers(applicationScope.get(ResponseHandlers.class)));
        modules.add(module);
        return this;
    }

    public Container applicationScope() {
        return applicationScope;
    }

    public Response handle(final Request request) throws Exception {
        return usingRequestScope(handleRequest(request));
    }

    public <T> T usingRequestScope(Callable1<Container, T> callable) {
        return using(createRequestScope(), callable);
    }

    private Container createRequestScope() {
        final Container requestScope = new SimpleContainer(applicationScope);
        requestScope.addInstance(Container.class, requestScope);
        requestScope.addActivator(Resolver.class, requestScope.getActivator(Container.class));
        requestScope.add(HttpHandler.class, BaseHandler.class);
        sequence(modules).safeCast(RequestScopedModule.class).forEach(addPerRequestObjects(requestScope));
        requestScope.decorate(HttpHandler.class, AbsoluteLocationHandler.class);
        requestScope.decorate(HttpHandler.class, ExceptionHandler.class);
        return requestScope;
    }

    public <T> T usingArgumentScope(Request request, Callable1<Container, T> callable) {
        return using(createArgumentScope(request), callable);
    }

    private Container createArgumentScope(Request request) {
        final Container argumentScope = new SimpleContainer();
        argumentScope.addInstance(Request.class, request);
        argumentScope.addInstance(Url.class, request.url());
        argumentScope.addInstance(HierarchicalPath.class, request.url().path());
        argumentScope.addActivator(PathParameters.class, PathParametersActivator.class);
        argumentScope.addInstance(HeaderParameters.class, request.headers());
        argumentScope.addInstance(QueryParameters.class, request.query());
        argumentScope.addInstance(FormParameters.class, request.form());
        argumentScope.addInstance(CookieParameters.class, request.cookies());
        argumentScope.addInstance(InputStream.class, request.input());
        argumentScope.add(new TypeFor<Option<?>>() {}.get(), new OptionResolver(argumentScope, instanceOf(IllegalArgumentException.class)));
        argumentScope.add(new TypeFor<Either<?, ?>>() {}.get(), new EitherResolver(argumentScope));
        sequence(modules).safeCast(ArgumentScopedModule.class).forEach(addPerArgumentObjects(argumentScope));
        return argumentScope;
    }

    public static Callable1<Container, Response> handleRequest(final Request request) {
        return new Callable1<Container, Response>(){
            public Response call(Container container) throws Exception {
                return container.get(HttpHandler.class).handle(request);
            }
        };
    }

    public void close() throws IOException {
        applicationScope().close();
    }
}