package no.dependent_implementation;

import java.io.File;
import java.util.*;

import no.dependent.Artifact;

import no.dependent.OutputBouble;
import no.dependent_implementation.feature.Feature;

public class FeatureManager {

    private List<Feature> features=new ArrayList<>();
	private Map<Artifact,Feature> artifactLocations=new HashMap<>();

	public Feature getFeature(Artifact artifact){
	    var fromMap=artifactLocations.get(artifact);
	    if(fromMap!=null) return fromMap;
	    return null;
    }

    public void addFeature(String pathToFeature){
	    File featureFile=new File(pathToFeature);
	    if(featureFile.exists()){
	        try{
                Feature newFeature=new Feature(featureFile);
                features.add(newFeature);
                newFeature.index();
                var artifacts=newFeature.getArtifacts();

                for (int j = 0; j < artifacts.length; j++) {
                    artifactLocations.put(artifacts[j],newFeature);
                    artifactLocations.put(artifacts[j].setVersion(""),newFeature);
                }
            } catch (Exception e){

            }
        } else {

        }
    }

/*	private ArtifactResult resolveArtifact(
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

		throw new Exception("Unable to describe artifact: "+artifact.getArtifactId(), ex);
	}

	public Result<File> getLocalFile(Artifact artifact){
		OutputBouble bouble=OutputBouble.push();
		try {
//			Artifact artifact = new Artifact(artifactId );
			ArtifactResult artifactResult;
			artifactResult = resolveArtifact(artifact);
	        Artifact describedArtifact=artifactResult.getArtifact();
	        return Result.res(describedArtifact.getFile());
		} catch (Exception e) {
			 return Result.error(e);
		} finally {
			bouble.pop();
			if(bouble.isError) bouble.writeToParent();
		}
	}
*/

    public Artifact[] getDirectDependencies(Artifact artifact) throws Exception {
		OutputBouble bouble=OutputBouble.push();
		try{
		    Feature featureContainingArtifact=artifactLocations.get(artifact);
		    if(featureContainingArtifact!=null){
		        return featureContainingArtifact.getDirectDependencies(artifact);
            } else return new Artifact[0];
		} finally {
			bouble.pop();
			if(bouble.isError) bouble.writeToParent();
		}
	}

/*

	public void addSource(String artifactsourceUrl, String name, String localStore, String groupFilter, String[] definition) {
		DependentRepository[] newArray=Arrays.copyOf(repositories,repositories.length+1) ;
		newArray[newArray.length-1]=new DependentRepository(new File(localStore), artifactsourceUrl, name, groupFilter, definition);
		repositories=newArray;
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
*/
}
