package no.dependent_implementation;

import java.util.LinkedList;
import java.util.List;

import no.dependent_implementation.utils.Booter;
import org.eclipse.aether.repository.RemoteRepository;

public class RemoteRepositories {
	final List<RemoteRepository> repos;
	
	public RemoteRepositories(String ... urls){
		repos=new LinkedList<RemoteRepository>();
		for (String repoUrl : urls) {
			repos.add(Booter.newRepository(repoUrl.substring(0, 10) + "..", repoUrl));
		}
	}
	
	public DependentDownloader local(String targetRepo){
		return new DependentDownloader(this, targetRepo);
	}
	
	

}
