package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class FeedSearchForm {

    @Size(max = 150, message = "Search cannot exceed 150 characters")
    @Pattern(
            regexp = "^[\\p{L}\\p{N} ]*$",
            message = "Search must contain only letters, numbers, and spaces")
    private String q = "";

    public String getQ() {
        return q;
    }

    public void setQ(final String q) {
        this.q = q;
    }
}
