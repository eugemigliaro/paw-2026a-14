package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class FeedSearchForm {

    @Size(max = 150, message = "{FeedSearchForm.q.Size}")
    @Pattern(regexp = "^[\\p{L}\\p{N} ]*$", message = "{FeedSearchForm.q.Pattern}")
    private String q = "";

    public String getQ() {
        return q;
    }

    public void setQ(final String q) {
        this.q = q;
    }
}
