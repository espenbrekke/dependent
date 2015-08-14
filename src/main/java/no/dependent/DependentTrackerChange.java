package no.dependent;

public interface DependentTrackerChange {
    void artifactChanged(String artifactId, DependentTrackerChangeDescription whatHappened);
}
