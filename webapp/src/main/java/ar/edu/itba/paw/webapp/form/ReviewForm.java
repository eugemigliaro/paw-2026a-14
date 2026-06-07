package ar.edu.itba.paw.webapp.form;

import ar.edu.itba.paw.models.types.PlayerReviewReaction;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class ReviewForm {

    @NotNull(message = "{NotNull.reviewForm.reaction}")
    private PlayerReviewReaction reaction;

    @Size(max = 1000, message = "{Size.reviewForm.comment}")
    @Pattern(
            regexp = "^[\\p{L}\\p{N} ,.;:()\"'\\-\\/ .!?\\n@&_*+=\\[\\]{}\\$#%^~|<>\\\\]*$",
            message = "{Pattern.reviewForm.comment}")
    private String comment;

    public PlayerReviewReaction getReaction() {
        return reaction;
    }

    public void setReaction(PlayerReviewReaction reaction) {
        this.reaction = reaction;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
