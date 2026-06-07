package ar.edu.itba.paw.webapp.validation;

import java.util.Set;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ImageValidator implements ConstraintValidator<ValidImage, MultipartFile> {

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024L * 1024L;

    @Override
    public boolean isValid(final MultipartFile image, final ConstraintValidatorContext context) {

        if (image == null) {
            return true;
        }

        boolean valid = true;
        context.disableDefaultConstraintViolation();

        valid = validateContentType(image, context) && valid;
        valid = validateContentSize(image, context) && valid;

        return valid;
    }

    private static boolean validateContentType(
            final MultipartFile image, final ConstraintValidatorContext context) {
        final String contentType = image.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            context.buildConstraintViolationWithTemplate(
                            "{exception.imageUpload.unsupportedFormat}")
                    .addConstraintViolation();
            return false;
        }
        return true;
    }

    private static boolean validateContentSize(
            final MultipartFile image, final ConstraintValidatorContext context) {
        if (image.getSize() > MAX_IMAGE_SIZE_BYTES) {
            context.buildConstraintViolationWithTemplate("{exception.imageUpload.tooLarge}")
                    .addConstraintViolation();
            return false;
        } else if (image.getSize() <= 0) {
            context.buildConstraintViolationWithTemplate("{exception.imageUpload.empty}")
                    .addConstraintViolation();
            return false;
        }
        return true;
    }
}
