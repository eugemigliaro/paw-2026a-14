package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class SearchForm {

    @Size(max = 150, message = "{SearchForm.q.Size}")
    @Pattern(regexp = "^[\\p{L}\\p{N} ]*$", message = "{SearchForm.q.Pattern}")
    private String q = "";

    public String getQ() {
        return q;
    }

    public void setQ(final String q) {
        this.q = q;
    }
}
