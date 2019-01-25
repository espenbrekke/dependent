package no.dependent.feature;

import no.dependent.DependentLoader;

public interface DependentFeatureManager {
    void registerFeatureRepository(String featureRepository);
    void signalChange(String featureRepository);
    DependentLoader getArtifact(String id);
}
