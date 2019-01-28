package no.dependent_implementation.circle;

import no.dependent.utils.Artifact;
import no.dependent_implementation.feature.DependencyResolutionExeption;
import no.dependent_implementation.feature.FeatureLoader;
import no.dependent_implementation.feature.DependentFeatureManager;
import no.dependent_implementation.DependentLoaderGraphImplementation;
import no.dependent_implementation.DependentRepositoryManager;
import no.dependent_implementation.PropertiesEngine;

import java.util.*;

public class Circle {
    final Circle outerCircle;
    final String name;
    final PropertiesEngine props;

    final public DependentLoaderGraphImplementation loaderGraph;

    public Circle(String name, Circle outerCircle){
        this.outerCircle=outerCircle;
        this.name=name;

        if(outerCircle!=null){
            mavenRepositoryManager=outerCircle.mavenRepositoryManager;
            featureManager=outerCircle.featureManager;
            props=new PropertiesEngine(outerCircle.props);
        } else {
            mavenRepositoryManager=new DependentRepositoryManager();
            featureManager= new DependentFeatureManager();
            props=new PropertiesEngine();
        }

        loaderGraph= DependentLoaderGraphImplementation.create(mavenRepositoryManager, featureManager, circleLoader() );
    }

    final public DependentRepositoryManager mavenRepositoryManager;
    final public DependentFeatureManager featureManager;

    public ClassLoader circleLoader(){
        if(outerCircle!=null) return outerCircle.circleLoader();
        return System.class.getClassLoader();
    }

    public static void create(Circle outerCircle, Artifact feature, String entrypoint) throws Exception{
        Circle newCircle=new Circle(entrypoint, outerCircle);
        FeatureLoader definingFeature=newCircle.loadDefiningFeatures(feature);

    }

    private FeatureLoader[] definingFeatures=null;

    private FeatureLoader loadDefiningFeatures(Artifact feature) throws Exception{
        if(definingFeatures!=null) throw new IllegalAccessException("defining features may only be written once");
        ArrayList<FeatureLoader> output=new ArrayList<FeatureLoader>();
        Map<Artifact, FeatureLoader> definingFeaturesMap=new HashMap<Artifact, FeatureLoader>();
        FeatureLoader foundIt=featureManager.getFeature(feature);
        LinkedList<Artifact> dependencies=new LinkedList<>();
        if(foundIt!=null){
            output.add(foundIt);
            definingFeaturesMap.put(feature,foundIt);
            dependencies.addAll(foundIt.getDependencies());

            while(!dependencies.isEmpty()){
                Artifact lookFor=dependencies.pop();
                if(definingFeaturesMap.get(lookFor)==null){
                    FeatureLoader dependency=featureManager.getFeature(lookFor);
                    if(dependency==null) throw new DependencyResolutionExeption("In "+name+": Unable to resolve feature dependency: "+dependency);

                    output.add(dependency);
                    definingFeaturesMap.put(lookFor,dependency);
                    dependencies.addAll(dependency.getDependencies());
                }
            }

            definingFeatures=output.toArray(new FeatureLoader[output.size()]);
            return foundIt;
        } else return null;
    };
}
