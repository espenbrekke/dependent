package no.dependent_implementation;

import no.dependent_implementation.utils.Booter;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class DependentRepository {
    final String name;
    final public File root;
    final public RemoteRepository remoteRepository;
    final public RepositorySystemSession session;

    private RepositorySystem system = Booter.newRepositorySystem();

    public DependentRepository(File root, String artifactsourceUrl,String name){
        this.root=root;
        session= Booter.newRepositorySystemSession(system, root.toString());

        if(artifactsourceUrl==null){
            remoteRepository=null;
        }
        else {
            remoteRepository= Booter.newRepository("remote-maven", artifactsourceUrl);
        }
        this.name=name;
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

    public String[] listArtifacts(){
        List<String> found= listArtifacts("", "", "", root);
        String[] retVal=new String[found.size()];
        return found.toArray(retVal);
    }
    private List<String> listArtifacts(String group,String prevPrev, String prev,File cursor){
        if(!"".equals(group)) group=group+".";
        group=group+prevPrev;

        List<String> retval=new LinkedList<String>();

        if(cursor.exists()){
            if(cursor.isDirectory()){
                File[] contains=cursor.listFiles();
                for(File sub:contains){
                    List<String> found=listArtifacts(group, prev,sub.getName(),sub);
                    retval.addAll(found);
                }
            } else {
                String fileName=cursor.getName();
                String artifactName=prevPrev+"-"+prev;
                String artifactType=fileName.replaceFirst(artifactName+".","");
                if(fileName.startsWith(artifactName)){
                    retval.add(group+":"+prevPrev+":"+artifactType+":"+prev);
                }
            }
        }

        return retval;
    }

}
