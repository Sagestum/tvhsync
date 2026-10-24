package tvhsync.conflicts;

import java.util.HashSet;
import java.util.Set;

import tvhsync.TvhTimer;
import tvhsync.utils.Period;

public class Conflict {
    private Period period = new Period();
    private Set<TvhTimer> involvedTimers = new HashSet<>();

    public Period getPeriod() {
        return period;
    }

    public void setPeriod(Period period) {
        this.period = period;
    }

    public Set<TvhTimer> getInvolvedTimers() {
        return involvedTimers;
    }

    public void setInvolvedTimers(Set<TvhTimer> involvedTimers) {
        this.involvedTimers = involvedTimers;
    }
}
