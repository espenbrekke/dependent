package no.dependent_implementation;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
	private DependentRepository[] repositories={};
    
	private ArtifactResult resolveArtifact(
			Artifact artifact) throws Exception {

		for(DependentRepository repository:repositories){
			try{
				ArtifactResult result=repository.resolveArtifact(artifact);
				if((result!=null) && (!result.isMissing()))
					return result;
			} catch (Exception e){
				e.printStackTrace();
			}
		}

		throw new Exception("unable to resolve artifact: "+artifact.getArtifactId());
	}

    private ArtifactDescriptorResult getArtifactDescriptor(
			Artifact artifact) throws Exception {

		for(DependentRepository repository:repositories){
			try{
				ArtifactResult result=repository.resolveArtifact(artifact);
				if((result!=null) && (!result.isMissing())){
					ArtifactDescriptorResult artifactDescriptor=repository.getArtifactDescriptor(artifact);
					if(artifactDescriptor!=null )
						return artifactDescriptor;
				}
			} catch (Exception e){
				e.printStackTrace();
			}
		}
		throw new Exception("unable to describe artifact: "+artifact.getArtifactId());
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

	public List<Artifact> getDirectDependencies(Artifact artifact) throws Exception {
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
/*
	public void setLocalRepo(String localRepo) {
		session = Booter.newRepositorySystemSession( system, localRepo );
	}
*/
	public void addLocalStore(String localStore){
		DependentRepository[] newArray=Arrays.copyOf(repositories,repositories.length+1) ;
		newArray[newArray.length-1]=new DependentRepository(new File(localStore), null);
		repositories=newArray;
	}

	public void addSource(String artifactsourceUrl, String localStore) {
		DependentRepository[] newArray=Arrays.copyOf(repositories,repositories.length+1) ;
		newArray[newArray.length-1]=new DependentRepository(new File(localStore), artifactsourceUrl);
	}

}
