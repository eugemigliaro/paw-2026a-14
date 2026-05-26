package ar.edu.itba.paw.models;

import ar.edu.itba.paw.models.types.Sport;
import java.util.List;

public class EloUpdatedResult {

    private final Sport sport;
    private final List<PlayerEloChange> changes;

    public EloUpdatedResult(final Sport sport, final List<PlayerEloChange> changes) {
        this.sport = sport;
        this.changes = List.copyOf(changes);
    }

    public Sport getSport() {
        return sport;
    }

    public List<PlayerEloChange> getChanges() {
        return changes;
    }

    @Override
    public String toString() {
        return "EloUpdatedResult{" + "sport=" + sport + ", changeCount=" + changes.size() + '}';
    }
}
