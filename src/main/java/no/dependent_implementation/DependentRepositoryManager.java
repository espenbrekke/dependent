package no.dependent_implementation;

import java.io.File;
import java.util.*;

import no.dependent_implementation.utils.Booter;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
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
		Exception ex=null;

		for(DependentRepository repository:repositories){
			try{
				if(repository.canResolve(artifact)){
					ArtifactResult result=repository.resolveArtifact(artifact);

					if((result!=null) && (!result.isMissing()))
						return result;
				}
			} catch (Exception e){
				ex=e;
			}
		}

		throw new Exception("unable to resolve artifact: "+artifact.getArtifactId(), ex);
	}

    private ArtifactDescriptorResult getArtifactDescriptor(
			Artifact artifact) throws Exception {
		Exception ex=null;

		for(DependentRepository repository:repositories){
			try{
				if(repository.canResolve(artifact)) {
					ArtifactResult result = repository.resolveArtifact(artifact);
					if ((result != null) && (!result.isMissing())) {
						ArtifactDescriptorResult artifactDescriptor = repository.getArtifactDescriptor(artifact);
						if (artifactDescriptor != null)
							return artifactDescriptor;
					}
				}
			} catch (Exception e){
				ex=e;
			}
		}
		throw new Exception("unable to describe artifact: "+artifact.getArtifactId(), ex);
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
        	if(isRuntime(dependency) && !dependency.isOptional())
        		retVal.add(dependency.getArtifact());
		}
        return retVal;
	}

	private boolean isRuntime(Dependency dependency) {
		if(dependency.getArtifact().getClassifier().contains("test")) return false;

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
	public void addLocalStore(String localStore, String name, String groupFilter){
		DependentRepository[] newArray=Arrays.copyOf(repositories,repositories.length+1) ;
		newArray[newArray.length-1]=new DependentRepository(new File(localStore), null, name, groupFilter, new String[0]);
		repositories=newArray;
	}

	public void addSource(String artifactsourceUrl, String name, String localStore, String groupFilter, String[] definition) {
		DependentRepository[] newArray=Arrays.copyOf(repositories,repositories.length+1) ;
		newArray[newArray.length-1]=new DependentRepository(new File(localStore), artifactsourceUrl, name, groupFilter, definition);
		repositories=newArray;
	}


	public String[] listRepositories(){
		LinkedList<String> namedRepos=new LinkedList<String>();

		String[] retVal={};
		for (int i = 0; i < repositories.length; i++) {
			if(!"".equals(repositories[i].name)) namedRepos.add(repositories[i].name);
		}
		return namedRepos.toArray(retVal);
	}
	public String[] listArtifacts(String repository){
		HashSet<String> artifacts=new HashSet();

		for (int i = 0; i < repositories.length; i++) {
			if(repository.equals(repositories[i].name)) {
				for(String artifact:repositories[i].listArtifacts()){
					artifacts.add(artifact);
				}
			}
		}
		String[] retVal=new String[artifacts.size()];
		return artifacts.toArray(retVal);
	}

	public void copy(String fromRepo,String toRepo,String filter) throws ArtifactResolutionException{
		DependentRepository copyTo=null;
		List<DependentRepository> copyFrom=new LinkedList();
		for (int i = 0; i < repositories.length; i++) {
			if(fromRepo.equals(repositories[i].name)) {
				copyFrom.add(repositories[i]);
			}
			if(toRepo.equals(repositories[i].name)) {
				copyTo=repositories[i];
			}
		}

		if(copyTo==null){
			DependentMainImplementation.reportError(new IllegalArgumentException("Unknown repository: "+toRepo));
		}
		if(copyFrom.size()==0){
			DependentMainImplementation.reportError(new IllegalArgumentException("Unknown repository: "+fromRepo));
		}
		if(copyTo==null || copyFrom.size()==0){
			return;
		}

		for(DependentRepository fromThis:copyFrom){
			String[] fromArtifacts=fromThis.listArtifacts();
			for(String singleArtifact:fromArtifacts){
				Artifact artifact=new DefaultArtifact(singleArtifact);
				if(singleArtifact.startsWith(filter)){
					copyTo.resolveArtifactFrom(artifact, fromThis);
				}
			}
		}

	}

}
