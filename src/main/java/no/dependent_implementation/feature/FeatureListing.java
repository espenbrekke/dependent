package no.dependent_implementation.feature;

import no.dependent.utils.Artifact;

import java.net.URL;

public class FeatureListing {
    public final Artifact id;
    public final FeatureRepository foundIn;
    public final URL url;

    public FeatureListing(Artifact id, FeatureRepository foundIn, URL url){
        this.id=id;
        this.foundIn=foundIn;
        this.url=url;
    }

}
