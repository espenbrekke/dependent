package no.dependent;

import java.io.File;

public interface Repository {
    File getArtifact(String artifactId);
}
