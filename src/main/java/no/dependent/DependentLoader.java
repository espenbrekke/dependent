package no.dependent;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.SecureClassLoader;
import java.util.Set;

public abstract class DependentLoader extends SecureClassLoader {
    public DependentLoader(ClassLoader parent){
        super(parent);
    }
    public abstract void tag(String tag);
    public abstract String getTag();
    public abstract DependentLoaderGraph getGraph();

    public abstract  void addLibraryPath(String path);

    public abstract  void addDependency(String toLoader);
    public abstract  void addDependency(DependentLoader toLoader);

    public abstract DependentLoader quarantine();
    public abstract  boolean isInQuarantine();

    public abstract String getArtifact();
    public abstract void extractTo(File target);

    public abstract String[] getEntries();
    public abstract DependentLoaderConfiguration[] getConfigurations();
    public abstract DependentLoader getConfigured(String configName);
}
