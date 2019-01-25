package no.dependent_implementation.feature;

import no.dependent.DependentLoader;
import no.dependent.utils.Artifact;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class FeatureRepository {
    private final FeatureRepositoryPolicy policy;

    public FeatureRepository(String pPlacement){
        policy=new FeatureRepositoryPolicy(pPlacement);
    }
    public DependentLoader getArtifact(String id) {
        return null;
    }

    private Map<Artifact,ArrayList<FeatureListing>> _featureListings=null;
    public Map<Artifact,ArrayList<FeatureListing>> listFeatures(){
        if(_featureListings==null){
            _featureListings=new HashMap<Artifact,ArrayList<FeatureListing>>();
        }
        ArrayList<FeatureListing> result=new ArrayList<>();

        if("".equals(policy.layout)){
            File base=new File(policy.directory);
            for(File child:base.listFiles()){
                if(child.isDirectory()){
                    String _id=child.getName();
                    int lastDot=_id.lastIndexOf('.');
                    String group=_id.substring(0,lastDot);
                    String artifactId=_id.substring(lastDot+1);
                    Artifact key=new Artifact(group,artifactId,"","");

                    for(File featureFile:child.listFiles()){
                        if(featureFile.getName().endsWith(".zip")){
                            try{
                                String name=featureFile.getName();
                                name=name.substring(0,name.length()-".zip".length());
                                String[] split=name.split("_");
                                String branch="";
                                String version="";
                                if(split.length==2){
                                    version=split[1];
                                } else if(split.length==3){
                                    branch=split[1];
                                    version=split[2];
                                }
                                ArrayList<FeatureListing> addTo=_featureListings.get(key);
                                if(addTo==null){
                                    addTo=new ArrayList<>();
                                    _featureListings.put(key,addTo);
                                }

                                addTo.add(new FeatureListing(new Artifact(group,artifactId, version, "zip"), this, featureFile.toPath().toUri().toURL()));
                            } catch (Throwable t){

                            }
                        }
                    }
                }
            }
        }
        for(ArrayList<FeatureListing> versions: _featureListings.values()){
            versions.sort(Comparator.comparing(v->Long.parseLong(v.id.version)));
        }

        return _featureListings;
    }

    public ArrayList<FeatureListing> getAllFeatureVersions(Artifact featureId){
        Artifact key=featureId.setVersion("").setFileType("");
        return listFeatures().get(key);
    }

    public FeatureListing getFeature(Artifact featureId) {
        ArrayList<FeatureListing> versions=getAllFeatureVersions(featureId);
        if(versions==null) return null;
        if("latest".equals(featureId.version)){
            versions.get(0);
        } else {
            featureId.setVersion("");
            for(FeatureListing listing:versions){
                if(listing.id==featureId) return listing;
            }
        }
        return null;
    }

}
