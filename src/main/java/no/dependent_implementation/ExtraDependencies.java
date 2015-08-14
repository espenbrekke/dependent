package no.dependent_implementation;

import java.util.ArrayList;import java.util.List;
import java.util.LinkedList;

/**
 * Created by espen on 8/14/14.
 */
class ExtraDependencies {
    private List<ExtraDependency> theExtraDependencies=new ArrayList<ExtraDependency>();

    private final DependentLoaderGraphImplementation loaderGraph;
    public ExtraDependencies(DependentLoaderGraphImplementation loaderGraph){
        this.loaderGraph=loaderGraph;
    }

    public void add(String from, String to) {
        theExtraDependencies.add(new ExtraDependency(from, to));
    }

    public void loaderAdded(DependentLoaderImplementation theLoader) {
        String artifactId=theLoader.artifact.toString();

        for (ExtraDependency extraDependency : theExtraDependencies) {
            extraDependency.loaderAdded(theLoader);
        }
    }

    private class ExtraDependency{
        final String from;
        private List<DependentLoaderImplementation> fromLoaders=new LinkedList<DependentLoaderImplementation>();
        final String to;
        private DependentLoaderImplementation toLoader=null;
        public ExtraDependency(String from, String to) {
            this.from=from;
            this.to=to;
        }

        public void loaderAdded(DependentLoaderImplementation theLoader) {
            String artifactId=theLoader.artifact.toString();
            boolean matches=false;

            if(artifactId.startsWith(from)){
                fromLoaders.add(theLoader);
                matches=true;
            }
            if(toLoader==null && artifactId.startsWith(to)){
                toLoader=theLoader;
                matches=true;
            }

            if(matches && toLoader!=null){
                for (DependentLoaderImplementation fromLoader : fromLoaders) {
                    fromLoader.addDependency(toLoader);
                }
            }
        }
    }
}
