package no.dependent_implementation;

import java.io.File;
import java.util.ArrayList;import java.util.List;
import java.util.LinkedList;

import no.dependent_implementation.utils.Booter;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

public class DependentRepositoryManager {
	private RepositorySystem system = Booter.newRepositorySystem();
	
	RepositorySystemSession session = Booter.newRepositorySystemSession( system, "target/local-repo" );
 //   RemoteRepository repo = Booter.newCentralRepository();
 //   RemoteRepository localrepo = Booter.newLocalRepository();
   
    private LinkedList<ArtifactSource> sources=new LinkedList<ArtifactSource>();
    
    private static class ArtifactSource{

		public ArtifactSource(String artifactsourceName,
				String artifactsourceUrl) {
			this.artifactsourceName=artifactsourceName;
			this.artifactsourceUrl=artifactsourceUrl;
			this.repo=Booter.newRepository(artifactsourceName,artifactsourceUrl);
		}
		public final String artifactsourceName;
		public final String artifactsourceUrl;
		public final RemoteRepository repo;
    		
    }
    
	private ArtifactResult resolveArtifact(
			Artifact artifact) throws ArtifactResolutionException {
        
        ArtifactResult artifactResult=null;
        for (ArtifactSource source : sources) {
        	try{
        		artifactResult =resolveArtifact(artifact, source);
        		if(!artifactResult.isMissing()) return artifactResult;
        	} catch (Exception e){
    //    		e.printStackTrace();
        	}
		}
		return artifactResult;
	}

	private ArtifactResult resolveArtifact(Artifact artifact, ArtifactSource source) throws ArtifactResolutionException {
        ArtifactRequest artifactRequest = new ArtifactRequest();
        artifactRequest.setArtifact( artifact );
        List<RemoteRepository> listOfSingleSource=new ArrayList<RemoteRepository>();
        listOfSingleSource.add(source.repo);
        artifactRequest.setRepositories(listOfSingleSource);
        
		ArtifactResult artifactResult = system.resolveArtifact( session, artifactRequest );
		return artifactResult;
	}
	
    private ArtifactDescriptorResult getArtifactDescriptor(
			Artifact artifact) throws ArtifactDescriptorException {
		ArtifactDescriptorRequest descriptorRequest = new ArtifactDescriptorRequest();
		descriptorRequest.setArtifact( artifact );
		
        for (ArtifactSource source : sources) {
        	descriptorRequest.addRepository(source.repo);
		}
                
		ArtifactDescriptorResult descriptorResult = system.readArtifactDescriptor( session, descriptorRequest );
		return descriptorResult;
	}

	public Result<File> getLocalFile(Artifact artifact){
		try {
//			Artifact artifact = new DefaultArtifact(artifactId );
			ArtifactResult artifactResult;
			artifactResult = resolveArtifact(artifact);
	        Artifact describedArtifact=artifactResult.getArtifact();
	        return Result.res(describedArtifact.getFile());
		} catch (Exception e) {
			 return Result.error(e);
		}
	}

	public List<Artifact> getDirectDependencies(Artifact artifact) throws ArtifactDescriptorException {
  //      Artifact artifact = new DefaultArtifact(artifactId );
        ArtifactDescriptorResult descriptorResult = getArtifactDescriptor(artifact);
        List<Dependency> dependencies=descriptorResult.getDependencies();
        List<Artifact> retVal=new LinkedList<Artifact>();
        for (Dependency dependency : dependencies) {
        	if(isRuntime(dependency)&& !dependency.isOptional())
        		retVal.add(dependency.getArtifact());
		}
        return retVal;
	}

	private boolean isRuntime(Dependency dependency) {
		switch (dependency.getScope()) {
		case "runtime":
		case "compile":
			return true;
		case "test":
		case "provided":
			return false;
			
		default:
			System.err.println("Discovered uhandled scope: "+dependency.getScope()+" For dependency "+dependency+". Ignoring");
			return false;
		}
	}

	public void setLocalRepo(String localRepo) {
		session = Booter.newRepositorySystemSession( system, localRepo );
	}

	public void addSource(String artifactsourceName, String artifactsourceUrl) {
		ArtifactSource newSource=new ArtifactSource(artifactsourceName, artifactsourceUrl);

//		sources.addFirst(newSource);
		sources.addLast(newSource);
	}

}
