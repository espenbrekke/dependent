package no.dependent;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;

public abstract class DependentLoader extends URLClassLoader {
    public DependentLoader(URL[] urls, ClassLoader parent){
        super(urls,parent);
    }
    public DependentLoader(URL[] urls){
        super(urls);
    }
    public abstract void tag(String tag);
    public abstract String getTag();
    public abstract DependentLoaderGraph getGraph();

    public abstract  void addLibraryPath(String path);

    public abstract  void addDependency(String toLoader);
    public abstract  void addDependency(DependentLoader toLoader);
    
    public abstract DependentLoader addJarFileDependency(String artifact,URL jarFile);
    public abstract DependentLoader replaceInGraph(URL ... urls);

    public abstract DependentLoader quarantine();
    public abstract  boolean isInQuarantine();

    public abstract String getArtifact();
    public abstract void extractTo(File target);

    public abstract Set<String> getEntries();
    public abstract DependentLoaderConfiguration[] getConfigurations();
    public abstract DependentLoader getConfigured(String configName);
}
