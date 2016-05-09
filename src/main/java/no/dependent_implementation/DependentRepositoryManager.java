package no.dependent_implementation;

import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import no.dependent.DependentLoaderConfiguration;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactResolutionException;
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
		OutputBouble.log1("unable to resolve artifact: " + artifact.getArtifactId());
		OutputBouble.log2("looked in:");
		for(DependentRepository repo:lookedIn){
			OutputBouble.log2(repo.toString());
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

	public void copy(String fromRepo,String toRepo,String filter, Boolean includeConfigDependencies) throws ArtifactResolutionException{
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
			OutputBouble.reportError(new IllegalArgumentException("Unknown repository: " + toRepo));
		}
		if(copyFrom.size()==0){
			OutputBouble.reportError(new IllegalArgumentException("Unknown repository: " + fromRepo));
		}
		if(copyTo==null || copyFrom.size()==0){
			return;
		}

		for(DependentRepository fromThis:copyFrom){
			String[] fromArtifacts=fromThis.listArtifacts();
			for(String singleArtifact:fromArtifacts){
				Artifact artifact=new DefaultArtifact(singleArtifact);
				if(singleArtifact.startsWith(filter)){
					ArtifactResult result=copyTo.resolveArtifactFrom(artifact, fromThis);
					if(includeConfigDependencies){
						File artifactFile=result.getArtifact().getFile();
						if(artifactFile!=null && artifactFile.exists() && artifactFile.getName().endsWith("jar")){
							ZipFile zf=null;
							InputStream dependentInputStream=null;
							try{
								zf=new ZipFile(artifactFile);
								ZipEntry dependentEntry=zf.getEntry("dependent.conf");
								dependentInputStream=zf.getInputStream(dependentEntry);

								Map<String,DependentLoaderConfiguration> confs=DependentLoaderImplementation.parseConfig(dependentInputStream);
								for(DependentLoaderConfiguration conf:confs.values()){
									for(String dependency:conf.get("dependency")){
										try{
											Artifact dependencyArtifact=new DefaultArtifact(dependency);
											this.resolveArtifact(dependencyArtifact);
										} catch (Throwable t) {
											OutputBouble.reportError(t);
										}
									}
								}
							} catch (Throwable t) {
								OutputBouble.reportError(t);
							} finally{
								try{
									if(zf!=null) zf.close();
									if(dependentInputStream!=null) dependentInputStream.close();
								} catch (Throwable t) {
									OutputBouble.reportError(t);
								}
							}
						}
					}
				}
			}
		}

	}

}
