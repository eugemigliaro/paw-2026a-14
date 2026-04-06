package ar.edu.itba.paw.services;

public class VerificationPreviewDetail {

    private final String label;
    private final String value;

    public VerificationPreviewDetail(final String label, final String value) {
        this.label = label;
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public String getValue() {
        return value;
    }
}
