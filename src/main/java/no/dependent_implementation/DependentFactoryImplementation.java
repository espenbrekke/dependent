package no.dependent_implementation;

import no.dependent.DependentFactory;
import no.dependent.ResourceFile;

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
}
