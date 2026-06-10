package ar.edu.itba.paw.webapp.utils;

import ar.edu.itba.paw.services.ImageUpload;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.web.multipart.MultipartFile;

public final class MultipartImageUpload implements ImageUpload {

    private final MultipartFile file;

    private MultipartImageUpload(final MultipartFile file) {
        this.file = file;
    }

    public static ImageUpload from(final MultipartFile file) {
        return file == null ? null : new MultipartImageUpload(file);
    }

    @Override
    public String getContentType() {
        return file.getContentType();
    }

    @Override
    public long getContentLength() {
        return file.getSize();
    }

    @Override
    public String getOriginalFilename() {
        return file.getOriginalFilename();
    }

    @Override
    public InputStream getContentStream() throws IOException {
        return file.getInputStream();
    }
}
