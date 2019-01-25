package no.dependent_implementation;

import no.dependent.*;
import no.dependent_implementation.circle.Circle;
import no.dependent_implementation.circle.CircleScript;

import java.util.ArrayList;
import java.util.List;

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

    public void executeScript(String[] script,String[] mainParams){
        ArrayList<String> scriptAsList=new ArrayList<String>();
        for (int i = 0; i < script.length; i++) {
            scriptAsList.add(script[i]);
        }
        Circle executeCircle=new Circle("executecircle", null);
        CircleScript.applyScript(executeCircle, scriptAsList,mainParams);
    };
}
