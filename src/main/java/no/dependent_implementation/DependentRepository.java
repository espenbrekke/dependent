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
    final public String groupFilter;

    private RepositorySystem system = Booter.newRepositorySystem();

    public boolean canResolve(Artifact artifact){
        if("".equals(groupFilter)) return true;
        return artifact.getGroupId().startsWith(groupFilter);
    }

    public DependentRepository(File root, String artifactsourceUrl,String name, String groupFilter){
        this.root=root;
        session= Booter.newRepositorySystemSession(system, root.toString());

        if(artifactsourceUrl==null){
            remoteRepository=null;
        }
        else {
            remoteRepository= Booter.newRepository("remote-maven", artifactsourceUrl);
        }
        this.name=name;
        this.groupFilter=groupFilter;
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
        LinkedList<String> retVal=new LinkedList<String>();
        File[] contains=root.listFiles();
        for(File sub:contains){
            if(sub.isDirectory()){
                List<String> found=listArtifacts("", "","",sub);
                retVal.addAll(found);
            }
        }
        return retVal.toArray(new String[retVal.size()]);
    }

    private List<String> listArtifacts(String group,String prevPrev, String prev,File cursor){


        List<String> retval=new LinkedList<String>();

        if(cursor.exists()){
            if(cursor.isDirectory()){
                if(!"".equals(group)) group=group+".";
                group=group+prevPrev;

                File[] contains=cursor.listFiles();
                for(File sub:contains){
                    List<String> found=listArtifacts(group, prev,cursor.getName(),sub);
                    retval.addAll(found);
                }
            } else {
                String fileName=cursor.getName();
                String artifactName=prevPrev+"-"+prev;
                String artifactType=fileName.replaceFirst(artifactName+".","");
                if(fileName.startsWith(artifactName) && !artifactType.contains("sha") && !artifactType.contains("pom")){
                    retval.add(group+":"+prevPrev+":"+artifactType+":"+prev);
                }
            }
        }

        return retval;
    }

}
