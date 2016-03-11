package no.dependent_implementation.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import no.dependent_implementation.manual.ManualRepositorySystemFactory;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * A helper to boot the repository system and a repository system session.
 */
public class Booter
{

    static public PrintStream logFile;
    static public ProxyOutputStream proxy = new ProxyOutputStream();
    static public File logPlacement = new File(".");
    static {
        try {
            logFile = new PrintStream(proxy);
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    public static void setLogFilePlacement(File placement) {
        logPlacement = placement;
        try {
            proxy.setTarget(new FileOutputStream(new File(logPlacement, "dependent.log")));

        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static RepositorySystem newRepositorySystem()
    {
    	RepositorySystem system=ManualRepositorySystemFactory.newRepositorySystem();
        return system;
        // return org.eclipse.aether.examples.guice.GuiceRepositorySystemFactory.newRepositorySystem();
        // return org.eclipse.aether.examples.plexus.PlexusRepositorySystemFactory.newRepositorySystem();
    }

    public static DefaultRepositorySystemSession newRepositorySystemSession( RepositorySystem system, String localRepoDir )
    {
        	DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        	session.setConfigProperty("aether.artifactResolver.snapshotNormalization", true);
    //    	PrintStream logFile=new PrintStream("dependent.log");
        
        	LocalRepository localRepo = new LocalRepository( localRepoDir );
        	session.setLocalRepositoryManager( system.newLocalRepositoryManager( session, localRepo ) );
			session.setTransferListener( new ConsoleTransferListener(logFile) );
			session.setRepositoryListener( new ConsoleRepositoryListener(logFile) );

        return session;
    }
/*
    public static RemoteRepository newCentralRepository()
    {
    	RemoteRepository.Builder builder=new RemoteRepository.Builder( "central", "default", "http://kamino.local.dbwatch.com:8081/nexus/content/groups/public/" );
    	return builder.build();
    }
    public static RemoteRepository newLocalRepository()
    {
    	return new RemoteRepository.Builder( "local_maven", "default", "file:./server/resources/drivers" ).build();
//        return new FileReocalRepository("/home/espen/.m2/repository");
    }
*/
	public static RemoteRepository newRepository(String artifactsourceName,
			String artifactsourceUrl) {
		return new RemoteRepository.Builder( artifactsourceName
					, "default"
					, artifactsourceUrl ).build();

	}

}