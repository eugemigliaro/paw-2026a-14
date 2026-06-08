package ar.edu.itba.paw.services;

import java.io.IOException;
import java.io.InputStream;

public interface ImageUpload {

    String getContentType();

    long getContentLength();

    InputStream getContentStream() throws IOException;

    default String getOriginalFilename() {
        return null;
    }
}
