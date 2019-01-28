package no.dependent_implementation.feature;

import no.dependent.utils.Artifact;

import java.util.*;

public class DependentFeatureManager {
    private List<FeatureRepository> repositoryList=new ArrayList<>();

    public void addFeatureStore(String featureStore){
        repositoryList.add(new FeatureRepository(featureStore));
    }
    public FeatureLoader getFeature(Artifact featureId){
        FeatureListing getThis=getFeatureListing(featureId);
        if(getThis==null)return null;
        return new FeatureLoader(getThis);
    }

    private FeatureListing getFeatureListing(Artifact featureId){
        LinkedList<FeatureListing> candidates=new LinkedList<>();

        for(FeatureRepository repo:repositoryList){
            FeatureListing gotIt=repo.getFeature(featureId);
            if(gotIt!=null){
                candidates.add(gotIt);
            }
        }

        candidates.sort(Comparator.comparing(v->v.id.version));

        if(candidates.isEmpty()) {
            return null;
        } else {
            return candidates.getFirst();
        }
    }
}
