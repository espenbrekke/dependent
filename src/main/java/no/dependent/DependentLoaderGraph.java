package no.dependent;

import java.io.File;
import java.net.URL;

public interface DependentLoaderGraph {
	public DependentLoader nameArtifact(String artifactId, URL[] jar, Object... dependsOn);
	public void nameClassLoader(String artifactId, ClassLoader loader, boolean recursive);
	public DependentLoader enshureJarLoaded(String artifactId);
	public DependentLoader findOverride(String artifactId);
    public DependentLoader[] getLoaded(String filter);
    public void getJar(String artifactId);
	public void logGraph(String logFile) ;
	public void connect(String connectThis, String toThis) ;
	public File getAsFile(String resourceId) ;
	public DependentLoader cloneLoader(DependentLoader loader, String aditionalDependency);
	public DependentLoader cloneLoader(DependentLoader loader);
	public void expose(String what, String toPackage) ;
    public void registerDependency(String from, String to);
    public void registerUnpackedJar(String what, String ... where);
    public void unpack(String artifact, File destination);
    public String[] getCompileClasspath(String ... artifactId);

	public void addLoaderVisitor(DependentLoaderVisitor visitor);

	public void reloadJar(String artifactId);
}
