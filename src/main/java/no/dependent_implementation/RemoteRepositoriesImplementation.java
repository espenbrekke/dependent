package no.dependent_implementation;

import java.util.LinkedList;
import java.util.List;

import no.dependent_implementation.utils.Booter;
import org.eclipse.aether.repository.RemoteRepository;
import no.dependent.RemoteRepositories;
import no.dependent.DependentDownloader;

public class RemoteRepositoriesImplementation implements RemoteRepositories{
	final List<RemoteRepository> repos;
	
	public RemoteRepositoriesImplementation(String ... urls){
		repos=new LinkedList<RemoteRepository>();
		for (String repoUrl : urls) {
			repos.add(Booter.newRepository(repoUrl.substring(0, 10) + "..", repoUrl,new String[0]));
		}
	}
	
	public DependentDownloader local(String targetRepo){
		return new DependentDownloaderImplementation(this, targetRepo);
	}
	

}
