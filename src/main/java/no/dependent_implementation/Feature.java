package no.dependent_implementation;

import no.dependent.Artifact;

public class Feature {
    public Artifact[] getDirectDependencies(Artifact artifact) throws Exception {
        return new Artifact[0];
    }
}