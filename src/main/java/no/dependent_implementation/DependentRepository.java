package no.dependent_implementation;

import no.dependent_implementation.utils.Booter;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DependentRepository {
    final public File root;
    final public RemoteRepository remoteRepository;
    final public RepositorySystemSession session;

    private RepositorySystem system = Booter.newRepositorySystem();

    public DependentRepository(File root, String artifactsourceUrl){
        this.root=root;
        session= Booter.newRepositorySystemSession(system, root.toString());

        if(artifactsourceUrl==null){
            remoteRepository=null;
        }
        else {
            remoteRepository= Booter.newRepository("remote-maven", artifactsourceUrl);
        }

    }

    public ArtifactResult resolveArtifact(Artifact artifact) throws ArtifactResolutionException {
        ArtifactRequest artifactRequest = new ArtifactRequest();
        artifactRequest.setArtifact( artifact );

        ArtifactResult artifactResult=null;
        if(remoteRepository!=null){
            List<RemoteRepository> listOfSingleSource=new ArrayList<RemoteRepository>();
            listOfSingleSource.add(remoteRepository);
            artifactRequest.setRepositories(listOfSingleSource);
        }
        artifactResult = system.resolveArtifact(session, artifactRequest);
        return artifactResult;
    }

    public ArtifactDescriptorResult getArtifactDescriptor(
            Artifact artifact) throws ArtifactDescriptorException {
        ArtifactDescriptorRequest descriptorRequest = new ArtifactDescriptorRequest();
        descriptorRequest.setArtifact( artifact );

        if(remoteRepository!=null){
            descriptorRequest.addRepository(remoteRepository);
        }

        ArtifactDescriptorResult descriptorResult = system.readArtifactDescriptor( session, descriptorRequest );
        return descriptorResult;
    }

    /*    private ArtifactDescriptorResult getArtifactDescriptor(
			Artifact artifact) throws ArtifactDescriptorException {
		ArtifactDescriptorRequest descriptorRequest = new ArtifactDescriptorRequest();
		descriptorRequest.setArtifact( artifact );

        for (ArtifactSource source : sources) {
        	descriptorRequest.addRepository(source.repo);
		}

		ArtifactDescriptorResult descriptorResult = system.readArtifactDescriptor( session, descriptorRequest );
		return descriptorResult;
	}
	*/

/*
    private ArtifactResult searchLocal(ArtifactRequest artifactRequest){
        ArtifactResult result=new ArtifactResult(artifactRequest);
        Artifact artifact=artifactRequest.getArtifact();
        String artifactDirString=artifact.getGroupId().replace('.','/')+"/"+artifact.getArtifactId()+'/'+artifact.getVersion();
        File artifactDir=new File(root, artifactDirString);
        if(artifactDir.exists() && artifactDir.isDirectory()){
            if("jar".equals(artifact.getExtension())){

            }
        }
        return result;
    }*/
}
