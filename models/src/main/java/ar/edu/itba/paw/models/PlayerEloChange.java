package ar.edu.itba.paw.models;

public class PlayerEloChange {

    private final User user;
    private final int previousElo;
    private final int newElo;
    private final boolean previouslyUnrated;

    public PlayerEloChange(
            final User user,
            final int previousElo,
            final int newElo,
            final boolean previouslyUnrated) {
        this.user = user;
        this.previousElo = previousElo;
        this.newElo = newElo;
        this.previouslyUnrated = previouslyUnrated;
    }

    public User getUser() {
        return user;
    }

    public int getPreviousElo() {
        return previousElo;
    }

    public int getNewElo() {
        return newElo;
    }

    public int getDelta() {
        return newElo - previousElo;
    }

    public boolean isPreviouslyUnrated() {
        return previouslyUnrated;
    }

    @Override
    public String toString() {
        return "PlayerEloChange{"
                + "userId="
                + (user == null ? null : user.getId())
                + ", previousElo="
                + previousElo
                + ", newElo="
                + newElo
                + ", delta="
                + getDelta()
                + ", previouslyUnrated="
                + previouslyUnrated
                + '}';
    }
}
