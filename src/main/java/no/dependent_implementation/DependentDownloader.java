package no.dependent_implementation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import no.dependent_implementation.utils.Booter;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;

public class DependentDownloader {
	private final RemoteRepositories repos;
	private final String targetRepo;
	private final RepositorySystem system = Booter.newRepositorySystem();
	
	DependentDownloader(RemoteRepositories repos, String targetRepo){
		this.repos=repos;
		this.targetRepo=targetRepo;
	}
	
	public void download(String artifact){
		try {
			Artifact theArtifact=new DefaultArtifact(artifact);
		
			RepositorySystemSession session = Booter.newRepositorySystemSession( system, targetRepo);


			DependencyFilter classpathFlter = DependencyFilterUtils.classpathFilter( JavaScopes.COMPILE );

			CollectRequest collectRequest = new CollectRequest();
			collectRequest.setRoot( new Dependency( theArtifact, JavaScopes.COMPILE ) );
                
			for (RemoteRepository repository : repos.repos) {
				collectRequest.addRepository( repository );	
			}

			DependencyRequest dependencyRequest = new DependencyRequest( collectRequest, classpathFlter );

			List<ArtifactResult> artifactResults;
			assertNotNull(session,"Session is null");
			assertNotNull(dependencyRequest,"dependencyRequest is null");
			DependencyResult resolvedDependencies=system.resolveDependencies( session, dependencyRequest );
			assertNotNull(resolvedDependencies,"resolvedDependencies is null");
			artifactResults = resolvedDependencies.getArtifactResults();
		
/*			for ( ArtifactResult artifactResult : artifactResults )
			{
				System.out.println( artifactResult.getArtifact() + " resolved to " + artifactResult.getArtifact().getFile() );
			}
			*/
		} catch (DependencyResolutionException e) {
			e.printStackTrace();
		}
	}
	
	public void downloadFlat(String artifact, String copyToDir){
		try {
			Artifact theArtifact=new DefaultArtifact(artifact);
		
			RepositorySystemSession session = Booter.newRepositorySystemSession( system, targetRepo);


			DependencyFilter classpathFlter = DependencyFilterUtils.classpathFilter( JavaScopes.COMPILE );

			CollectRequest collectRequest = new CollectRequest();
			collectRequest.setRoot( new Dependency( theArtifact, JavaScopes.COMPILE ) );
                
			for (RemoteRepository repository : repos.repos) {
				collectRequest.addRepository( repository );	
			}
			DependencyRequest dependencyRequest = new DependencyRequest( collectRequest, classpathFlter );

			List<ArtifactResult> artifactResults;
			assertNotNull(session,"Session is null");
			assertNotNull(dependencyRequest,"dependencyRequest is null");
			DependencyResult resolvedDependencies=system.resolveDependencies( session, dependencyRequest );
			assertNotNull(resolvedDependencies,"resolvedDependencies is null");
			artifactResults = resolvedDependencies.getArtifactResults();
			
			File targetDir=new File(copyToDir); 
			targetDir.mkdir();
					
			for ( ArtifactResult artifactResult : artifactResults )
			{
				File from=artifactResult.getArtifact().getFile();
				File to=new File(targetDir,from.getName());
				Files.copy(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (DependencyResolutionException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

    private ArtifactResult getArtifact(String artifact) throws IOException ,ArtifactResolutionException {
        Artifact theArtifact=new DefaultArtifact(artifact);

        RepositorySystemSession session = Booter.newRepositorySystemSession( system, targetRepo);


        DependencyFilter classpathFlter = DependencyFilterUtils.classpathFilter( JavaScopes.COMPILE );

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot( new Dependency( theArtifact, JavaScopes.COMPILE ) );

        for (RemoteRepository repository : repos.repos) {
            collectRequest.addRepository( repository );
        }
        ArtifactRequest ardifactRequest=new ArtifactRequest(theArtifact,repos.repos,"downloading");

//			DependencyRequest dependencyRequest = new DependencyRequest( collectRequest, classpathFlter );

//			List<ArtifactResult> artifactResults;
        assertNotNull(session,"Session is null");
        assertNotNull(ardifactRequest,"ardifactRequest is null");
        ArtifactResult resolvedArtifact=system.resolveArtifact(session,ardifactRequest);
        assertNotNull(resolvedArtifact,"resolvedDependencies is null");

        return resolvedArtifact;
    }
	
	public File downloadSingle(String artifact, String toDir){
        String whileDoing="";
		try {
            ArtifactResult resolvedArtifact=getArtifact(artifact);
            
			File targetDir=new File(toDir);
			targetDir.mkdirs();

			File from=resolvedArtifact.getArtifact().getFile();
            if(from==null || !from.exists()) throw new Exception("Unable to download artifact "+ artifact);
            File to=new File(targetDir,from.getName());

            whileDoing="Copying "+from.toPath()+" to "+to.toPath();
			Files.copy(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING);
            File dir = new File(from.getParent());
            for(File pomCandidate : dir.listFiles()) {
                if(pomCandidate.getName().endsWith(".pom")) {
                    File toPom=new File(targetDir,pomCandidate.getName());
                    whileDoing="Copying "+pomCandidate.toPath()+" to "+toPom.toPath();
                    Files.copy(pomCandidate.toPath(), toPom.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }

            System.out.println("Debug: " + whileDoing);
			return to;
		}
		catch (IOException e) {
            if(!"".equals(whileDoing)){
                System.out.println("While: "+whileDoing);
            }
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
    public File downloadSingle(String artifact, String toDir, String targetName){
        String whileDoing="";
        try {
            ArtifactResult resolvedArtifact=getArtifact(artifact);

            File targetDir=new File(toDir);
            targetDir.mkdirs();

            File from=resolvedArtifact.getArtifact().getFile();
            if(from==null || !from.exists()) throw new Exception("Unable to download artifact "+ artifact);
            File to=new File(targetDir,targetName);
            whileDoing="Copying "+from.toPath()+" to "+to.toPath();
            Files.copy(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING);
            File dir = new File(from.getParent());
            for(File pomCandidate : dir.listFiles()) {
                if(pomCandidate.getName().endsWith(".pom")) {
                    File toPom=new File(targetDir,pomCandidate.getName());
                    Files.copy(pomCandidate.toPath(), toPom.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }

            return to;
        }
        catch (IOException e) {
            if(!"".equals(whileDoing)){
                System.out.println("While: "+whileDoing);
            }
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
	
	private void assertNotNull(Object value,String message){
		if(value == null){
			System.out.println(message);
			throw new NullPointerException(message);
		}
	}

    private InputStream getStreamFrom(String artifact,String fileName){
        try {
            ArtifactResult resolvedArtifact = getArtifact(artifact);

            File from=resolvedArtifact.getArtifact().getFile();

            ZipFile zipFile = new ZipFile(from);
            ZipEntry theEntry=zipFile.getEntry(fileName);

            if(theEntry!=null){            	
            	return zipFile.getInputStream(theEntry);
            }

            System.out.println();
            System.out.println("Error getting "+fileName+" from "+ artifact);
            System.out.println("All entries:");

            Enumeration<? extends ZipEntry> entries= zipFile.entries();
            while(entries.hasMoreElements()){
                System.out.println(entries.nextElement().getName());
            }
            zipFile.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }


        return null;

    }
}
