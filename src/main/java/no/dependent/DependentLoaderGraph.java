package no.dependent;

import java.io.File;
import java.net.URL;

public interface DependentLoaderGraph {
	public DependentLoader enshureJarLoaded(String artifactId);
	public DependentLoader findOverride(String artifactId);
    public DependentLoader[] getLoaded(String filter);
	public void logGraph(String logFile) ;
	public File getAsFile(String resourceId) ;
	public void expose(String what, String toPackage) ;
    public void registerDependency(String from, String to);
    public void registerUnpackedJar(String what, String ... where);
    public void unpack(String artifact, File destination);
}
