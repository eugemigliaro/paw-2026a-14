package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.Size;

public class FeedSearchForm {

    @Size(max = 100, message = "Search cannot exceed 100 characters")
    private String q = "";

    public String getQ() {
        return q;
    }

    public void setQ(final String q) {
        this.q = q;
    }
}
