package no.dependent_implementation;

import no.dependent.utils.Artifact;

import java.util.ArrayList;import java.util.List;


public class Exposure {
	private List<ExposureFilter> exposed=new ArrayList<ExposureFilter>();
	private final DependentLoaderGraphImplementation loaderGraph;
	public Exposure(DependentLoaderGraphImplementation loaderGraph){
		this.loaderGraph=loaderGraph;
	}

    public DependentLoaderGraphImplementation getGraph(){
        return loaderGraph;
    }

	private static class ExposureFilter{
		public final Artifact what;
		public final String toPackage;
		public ExposureFilter(Artifact what, String toPackage){
			this.what=what;
			this.toPackage=toPackage;
		}
		
		public ClassLoader exposedLoader=null;
	}

	public List<ClassLoader> getLoadersExposedToPackage(String packageName){
		List<ClassLoader> retVal=new ArrayList<ClassLoader>(exposed.size());
		for (ExposureFilter filter : exposed) {
			if(packageName.startsWith(filter.toPackage)){
				if(filter.exposedLoader==null){
					filter.exposedLoader=loaderGraph.enshureJarLoaded(filter.what);
				}
				if(filter.exposedLoader!=null){
					retVal.add(filter.exposedLoader);
				}
			}
		}
		return retVal;
	}
	
	public void expose(Artifact what, String toPackage) {
		exposed.add(new ExposureFilter(what, toPackage));
	}
}
