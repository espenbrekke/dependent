package no.dependent_implementation;

import no.dependent.DependentFactory;
import no.dependent.ResourceFile;
import no.dependent.RemoteRepositories;
import no.dependent.DependentDownloader;

public class DependentFactoryImplementation extends DependentFactory {
    final ClassLoader parentLoader;
    public DependentFactoryImplementation(ClassLoader parentLoader){
        this.parentLoader=parentLoader;
    }

    public ResourceFile resourceFile(String resourceId){
        return new ResourceFileImplementation(resourceId);
    }
    public DependentLoaderGraphImplementation getGraph(){
        return DependentLoaderGraphImplementation.get(parentLoader);
    }
    public no.dependent.DependentTracker getTracker(){
        return DependentTrackerImplementation.get();
    };

    public Class mainClass(){
        return DependentMainImplementation.class;
    }

    public RemoteRepositories remoteRepositories(String ... urls){
        return new RemoteRepositoriesImplementation(urls);
    }
}
