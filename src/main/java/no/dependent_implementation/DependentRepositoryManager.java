package no.dependent_implementation;

import java.io.File;
import java.util.*;

import no.dependent.OutputBouble;

import no.dependent.utils.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactResult;

public class DependentRepositoryManager {
	private DependentRepository[] repositories={};
    
	private ArtifactResult resolveArtifact(
			Artifact artifact) throws Exception {
		Throwable ex=null;
		List<DependentRepository> lookedIn=new LinkedList();

		List<OutputBouble> errorBoubles=new LinkedList<>();
		for(DependentRepository repository:repositories){
			OutputBouble bouble=OutputBouble.push();
			errorBoubles.add(bouble);
			try{
				if(repository.canResolve(artifact)){
					lookedIn.add(repository);
					ArtifactResult result=repository.resolveArtifact(artifact);

					if(result!=null && result.isResolved()){
						for(OutputBouble b:errorBoubles){
							b.close();
						}
						return result;
					}
				}
			} catch (Throwable e){
				ex=e;
			} finally {
				bouble.pop();
			}
		}

		if(!errorBoubles.isEmpty()) errorBoubles.get(0).writeToParent();
		for(OutputBouble bouble:errorBoubles){
			bouble.close();
		}
		OutputBouble.log1("unable to resolve artifact: " + artifact.id);
		OutputBouble.log2("looked in:");
		for(DependentRepository repo:lookedIn){
			OutputBouble.log2(repo.toString());
		}

		throw new Exception("unable to resolve artifact: "+artifact.id, ex);
	}

    private ArtifactDescriptorResult getArtifactDescriptor(
			Artifact artifact) throws Exception {
		Exception ex=null;

		List<OutputBouble> errorBoubles=new LinkedList<>();
		List<DependentRepository> lookedIn=new LinkedList();

		for(DependentRepository repository:repositories){
			OutputBouble bouble=OutputBouble.push();
			errorBoubles.add(bouble);
			try{
				if(repository.canResolve(artifact)) {
					lookedIn.add(repository);

					ArtifactResult result = repository.resolveArtifact(artifact);
					System.out.println("artifact resolved " + result.toString());
					if (result != null && result.isResolved()) {
						for(OutputBouble b:errorBoubles){
							b.close();
						}
						bouble.pop();
						ArtifactDescriptorResult artifactDescriptor = repository.getArtifactDescriptor(artifact);
						if (artifactDescriptor != null){
							//System.out.println("Got artifact descriptor "+artifactDescriptor.toString());
							return artifactDescriptor;
						} else {
							//System.out.println("Got no artifact descriptor ");
						}
					}
				}
			} catch (Exception e){
				ex=e;
			} finally {
				bouble.pop();
			}
		}

		if(!errorBoubles.isEmpty()) errorBoubles.get(0).writeToParent();
		for(OutputBouble bouble:errorBoubles){
			bouble.close();
		}
		OutputBouble.log1("unable to describe artifact: " + artifact.toString());
		OutputBouble.log2("looked in:");
		for(DependentRepository repo:lookedIn){
			OutputBouble.log2(repo.toString());
		}

		throw new Exception("Unable to describe artifact: "+artifact.id, ex);
	}

	public Result<File> getLocalFile(Artifact artifact){
		OutputBouble bouble=OutputBouble.push();
		try {
//			Artifact artifact = new DefaultArtifact(artifactId );
			ArtifactResult artifactResult;
			artifactResult = resolveArtifact(artifact);
	        org.eclipse.aether.artifact.Artifact describedArtifact=artifactResult.getArtifact();
	        return Result.res(describedArtifact.getFile());
		} catch (Exception e) {
			 return Result.error(e);
		} finally {
			bouble.pop();
			if(bouble.isError) bouble.writeToParent();
		}
	}

	public List<Artifact> getDirectDependencies(Artifact artifact) throws Exception {
  //      Artifact artifact = new DefaultArtifact(artifactId );
		OutputBouble bouble=OutputBouble.push();

		try{
			ArtifactDescriptorResult descriptorResult = getArtifactDescriptor(artifact);
			List<Dependency> dependencies=descriptorResult.getDependencies();
			List<Artifact> retVal=new LinkedList<Artifact>();
			for (Dependency dependency : dependencies) {
				if(isRuntime(dependency) && !dependency.isOptional())
					retVal.add( new Artifact(dependency.getArtifact().toString()) );
			}
			return retVal;
		} finally {
			bouble.pop();
			if(bouble.isError) bouble.writeToParent();
		}
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
}
