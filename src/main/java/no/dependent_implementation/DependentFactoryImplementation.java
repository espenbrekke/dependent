package no.dependent_implementation;

import no.dependent.*;

import java.util.ArrayList;
import java.util.List;

public class DependentFactoryImplementation extends DependentFactory {
    final ClassLoader parentLoader;
    public DependentFactoryImplementation(){
        this.parentLoader=this.getClass().getClassLoader();
    }

    public ResourceFile resourceFile(String resourceId){
        return new ResourceFileImplementation(resourceId);
    }

    public DependentLoaderGraphImplementation getGraph(){
        return DependentLoaderGraphImplementation.get(parentLoader);
    }

    public Class mainClass(){
        return DependentMainImplementation.class;
    }

    public void executeScript(String[] script,String[] mainParams){
        ArrayList<String> scriptAsList=new ArrayList<String>();
        for (int i = 0; i < script.length; i++) {
            scriptAsList.add(script[i]);
        }
        DependentMainImplementation.executeScript(scriptAsList, mainParams);
    };
}
