package com.googlecode.utterlyidle;

import com.googlecode.totallylazy.io.Uri;

import java.io.Closeable;

public interface Server extends Closeable {
    Application application();
    Uri uri();
}
