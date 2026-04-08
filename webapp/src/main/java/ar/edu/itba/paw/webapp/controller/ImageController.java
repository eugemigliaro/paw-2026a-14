package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.ImageMetadata;
import ar.edu.itba.paw.services.ImageService;
import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Controller
public class ImageController {

    private static final CacheControl IMAGE_CACHE_CONTROL =
            CacheControl.maxAge(Duration.ofDays(30)).cachePublic();

    private final ImageService imageService;

    @Autowired
    public ImageController(final ImageService imageService) {
        this.imageService = imageService;
    }

    @GetMapping("/images/{id}")
    public ResponseEntity<StreamingResponseBody> getImage(
            @PathVariable("id") final Long imageId, final WebRequest request) {
        final Optional<ImageMetadata> metadataOptional = imageService.findMetadataById(imageId);
        if (metadataOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        final ImageMetadata metadata = metadataOptional.get();
        final String eTag = toWeakEtag(metadata);
        if (request.checkNotModified(eTag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .cacheControl(IMAGE_CACHE_CONTROL)
                    .eTag(eTag)
                    .build();
        }

        final StreamingResponseBody responseBody =
                outputStream -> {
                    final boolean found = imageService.streamContentById(imageId, outputStream);
                    if (!found) {
                        throw new IllegalStateException(
                                "Image content not found for id " + imageId);
                    }
                };

        return ResponseEntity.ok()
                .cacheControl(IMAGE_CACHE_CONTROL)
                .eTag(eTag)
                .contentType(MediaType.parseMediaType(metadata.getContentType()))
                .contentLength(metadata.getContentLength())
                .body(responseBody);
    }

    private static String toWeakEtag(final ImageMetadata metadata) {
        return "W/\"img-" + metadata.getId() + "-" + metadata.getContentLength() + "\"";
    }
}
