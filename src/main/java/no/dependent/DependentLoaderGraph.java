package no.dependent;

import org.eclipse.aether.artifact.Artifact;
import java.io.File;
import java.net.URL;

public interface DependentLoaderGraph {
	public DependentLoader nameArtifact(String artifactId, URL[] jar, Object... dependsOn);
	public DependentLoader nameArtifact(Artifact artifactId, URL[] jar, Object... dependsOn);
	public void nameClassLoader(String artifactId, ClassLoader loader, boolean recursive);
	public void nameClassLoader(Artifact artifactId, ClassLoader loader,boolean recursive);
	public DependentLoader enshureJarLoaded(String artifactId);
    public DependentLoader findOverride(Artifact artifactId);
    public DependentLoader[] getLoaded(String filter);
    public DependentLoader enshureJarLoaded(Artifact artifactId);
    public void getJar(String artifactId);
	public void getJar(Artifact artifactId);
	public void logGraph(String logFile) ;
	public void connect(String connectThis, String toThis) ;
	public File getAsFile(String resourceId) ;
	public DependentLoader cloneLoader(DependentLoader loader, String aditionalDependency);
	public DependentLoader cloneLoader(DependentLoader loader);
	public void expose(String what, String toPackage) ;
    public void registerDependency(String from, String to);
    public void registerUnpackedJar(String what, String ... where);
	public void unifyGroupVersion(String group);
    public void unpack(String artifact, File destination);
    public String[] getCompileClasspath(String ... artifactId);

	public void addLoaderVisitor(DependentLoaderVisitor visitor);

	public void reloadJar(String artifactId);
}
