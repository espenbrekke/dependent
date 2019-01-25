package no.dependent_implementation;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import no.dependent.DependentLoader;
import no.dependent.DependentLoaderVisitor;
import no.dependent.DependentLoaderGraph;
import no.dependent.OutputBouble;
import no.dependent.utils.Artifact;
import no.dependent_implementation.feature.DependentFeatureManager;
import no.dependent_implementation.feature.FeatureOrder;

public class DependentLoaderGraphImplementation implements DependentLoaderGraph{
	private static AtomicInteger _idCounter=new AtomicInteger(0);

	public static int getNewLoaderId(){
		return _idCounter.getAndIncrement();
	}

	private Map<String,String> unified=new HashMap<String,String>();
	
	private static DependentLoaderGraphImplementation theGraph=null;
	
	private final Exposure exposed=new Exposure(this);
    private final ExtraDependencies extraDependencies=new ExtraDependencies(this);
    private final ClassLoader parentLoader;

	private DependentLoaderVisitor[] visitors={};

	void setLoader(DependentLoaderImplementation loader){
		synchronized (loaderMap){
			this.loaderMap.put(loader.getArtifact(), loader);
		}
	}


	public void addLoaderVisitor(DependentLoaderVisitor visitor){
		DependentLoaderVisitor[] newVisitors=new DependentLoaderVisitor[visitors.length+1];
		int i=0;
		for(DependentLoaderVisitor v:visitors){
			if(v==visitor) return;
			newVisitors[i]=visitors[i];
			i++;
		}
		newVisitors[i]=visitor;
		visitors=newVisitors;
		List<DependentLoaderImplementation> impls=null;
		synchronized (loaderMap) {
			impls=new ArrayList<DependentLoaderImplementation>(loaderMap.values());
		}
		for(DependentLoader loader:impls){
			//System.out.println(loader.getArtifact());
			visitor.visitLoader(loader);
		}
	}

	private void visitLoader(DependentLoaderImplementation theLoader){
		for(DependentLoaderVisitor v:visitors){
			try {
				v.visitLoader(theLoader);
			} catch (Throwable e){
				OutputBouble.reportError(e);
			}
		}
	}

	public static DependentLoaderGraphImplementation create(
            DependentRepositoryManager dependencyManager, DependentFeatureManager featureManager, ClassLoader parentLoader ) {
		theGraph=new DependentLoaderGraphImplementation(dependencyManager, featureManager, parentLoader);
		return theGraph;
	}

	public static DependentLoaderGraphImplementation get(ClassLoader parentLoader){
		if(theGraph==null){
			theGraph= DependentLoaderGraphImplementation.create(new DependentRepositoryManager(), new DependentFeatureManager(), parentLoader);
		}
		return theGraph;
	}

	final public DependentRepositoryManager mavenRepositoryManager;
	final public DependentFeatureManager featureManager;

    private final Map<String, DependentLoaderImplementation> loaderMap=new HashMap<String, DependentLoaderImplementation>();

    private static class Wildcards{
        final String what;
        final DependentLoaderImplementation loader;
        final File[] where;
        Wildcards(String what, DependentLoaderImplementation loader, File[] where){
            this.what=what;
            this.loader=loader;
            this.where=where;
        }
    }

    private final List<Wildcards> overridingLoaders= new ArrayList();

	DependentLoaderGraphImplementation(DependentRepositoryManager mavenRepositoryManager, DependentFeatureManager featureManager,  ClassLoader parentLoader){
		this.mavenRepositoryManager=mavenRepositoryManager;
		this.featureManager=featureManager;
        this.parentLoader=parentLoader;
	}
	
	public DependentLoaderImplementation nameArtifact(String artifactId, URL[] jar, Object... dependsOn){
		return nameArtifact(new Artifact(artifactId), jar, dependsOn);
	}

    @Override
    public void nameClassLoader(String artifactId, ClassLoader loader, boolean recursive) {

    }

    public String[] getCompileClasspath(String ... artifactIds) {
        List<String> list = new LinkedList<String>();
        Set<Artifact> cutoff = new HashSet<Artifact>();

        for(String artifactId : artifactIds) {
			DependentLoaderImplementation depLoaderImpl = enshureJarLoaded(artifactId);
            getCompileClasspath(list, cutoff, depLoaderImpl);
        }

        return list.toArray(new String[list.size()]);
    }



    private void getCompileClasspath(List<String> list, Set<Artifact> cutoff, DependentLoaderImplementation depLoader) {
        if(!cutoff.contains(depLoader.artifact)) {
            cutoff.add(depLoader.artifact);
            DependentLoaderImplementation depLoaderImpl = enshureJarLoaded(depLoader.artifact);
            for (URL url : depLoader.getURLs()) {
                try {
                    String path = Paths.get(url.toURI()).toFile().getAbsolutePath();
                    if (!list.contains(path)) {
                        list.add(path);
                    }
                } catch (Exception e) {}
            }
            for (DependentLoaderImplementation dep : depLoaderImpl.dependencies) {
                getCompileClasspath(list, cutoff, dep);
            }
        }
    }

	public DependentLoaderImplementation nameArtifact(Artifact artifactId, URL[] jar, Object... dependsOn){
		DependentLoaderImplementation theLoader=null;
		if(jar==null) theLoader= new DependentLoaderImplementation(artifactId, exposed,parentLoader);
		else theLoader= new DependentLoaderImplementation(artifactId,jar,exposed,parentLoader);
		
		List<DependentLoaderImplementation> dependencies=new ArrayList<DependentLoaderImplementation>();
		
		for (Object dependency : dependsOn) {
			if(dependency instanceof DependentLoaderImplementation){
				dependencies.add((DependentLoaderImplementation)dependency);
			} else if(dependency instanceof String){
				DependentLoaderImplementation candidate=enshureDependencyJarLoaded(artifactId, new Artifact((String)dependency));
				if(candidate!=null)
					dependencies.add((DependentLoaderImplementation)dependency);
			} else if(dependency instanceof Artifact){
				DependentLoaderImplementation candidate=enshureDependencyJarLoaded(artifactId, (Artifact)dependency);
				if(candidate!=null)
					dependencies.add((DependentLoaderImplementation)dependency);
			} 
		}
		theLoader.setDependencies(dependencies.toArray(new DependentLoaderImplementation[dependencies.size()]));
		
		if(artifactId!=null){
			assert(theLoader!=null);
			setLoader(theLoader);
			assert(loaderMap.get(toString(artifactId))!=null);
		}
		visitLoader(theLoader);
		return theLoader;
	}

	public DependentLoaderImplementation enshureJarLoaded(String artifactId){
		return enshureJarLoaded(new Artifact(artifactId));
	}

	private DependentLoaderImplementation enshureDependencyJarLoaded(Artifact dependent, String dependency){
		return enshureJarLoaded(unify(dependent,new Artifact(dependency)));
    }
	
	private DependentLoaderImplementation enshureDependencyJarLoaded(Artifact dependent, Artifact dependency){
			return enshureJarLoaded(unify(dependent,dependency));
	}

	@Override
	public DependentLoaderImplementation findOverride(String artifactId){
		Artifact artifact=new Artifact(artifactId);
		return findOverride(artifact);
	}
    public DependentLoaderImplementation findOverride(Artifact artifactId){
        if(overridingLoaders.size()==0) return null;
        String asString=artifactId.toString();
        for (Wildcards wild: overridingLoaders) {
            if(asString.startsWith(wild.what))
				return wild.loader;
        }

        return null;
    }
    private File findOverrideFile(Artifact artifactId){
        if(overridingLoaders.size()==0) return null;
        String asString=artifactId.toString();
        for (Wildcards wild: overridingLoaders) {

            if(wild.where.length>0 && asString.startsWith(wild.what)){
				return wild.where[0];
			}
        }

        return null;
    }

    public DependentLoaderImplementation[] getLoaded(String filter){
        Artifact asArtifact=new Artifact(filter);
        DependentLoaderImplementation theLoader=findOverride(asArtifact);
        if(theLoader!=null){
            DependentLoaderImplementation[] retVal={theLoader};
            return retVal;
        }

        if(loaderMap.containsKey(filter)){
            DependentLoaderImplementation[] retVal={loaderMap.get(filter)};
            return retVal;
        }

        ArrayList<DependentLoaderImplementation> result=new ArrayList();
        for(String key:loaderMap.keySet()){
            if(key.startsWith(filter)){
                result.add(loaderMap.get(key));
            }
        };
        return Arrays.copyOf(result.toArray(),result.size(), DependentLoaderImplementation[].class );
    }


    public DependentLoaderImplementation enshureJarLoaded(Artifact _artifactId){
		Artifact artifactId=unify(_artifactId);

        if("dependent".equals(artifactId.id) && "no.dbwatch".equals(artifactId.group)){
            return null;
        }
        DependentLoaderImplementation theLoader=findOverride(artifactId);
        if(theLoader!=null){
			visitLoader(theLoader);
			return theLoader;
		}
		if(loaderMap.containsKey(toString(artifactId))) return loaderMap.get(toString(artifactId));
		// Fetch jar and dependencies.
		OutputBouble bouble=OutputBouble.push();

		try {
			Result<File> localFileName=mavenRepositoryManager.getLocalFile(artifactId);
			
			if(localFileName.success()) theLoader=new DependentLoaderImplementation(artifactId, localFileName.val.getAbsoluteFile().toURI().toURL(), exposed, parentLoader);
			else  theLoader=new DependentLoaderImplementation(artifactId, exposed, parentLoader);
			setLoader(theLoader);
			
			List<Artifact> dependencies=mavenRepositoryManager.getDirectDependencies(artifactId);

			DependentLoaderImplementation[] actualDependencies=new DependentLoaderImplementation[dependencies.size()+theLoader.dependencies.length];
			int i=0;
			for(DependentLoaderImplementation dependencyFromConf:theLoader.dependencies){
				actualDependencies[i++]=dependencyFromConf;
			}
			for (Artifact dependencyId : dependencies) {
				
				DependentLoaderImplementation loader=enshureDependencyJarLoaded(artifactId,dependencyId);
				if(loader==null){
                    OutputBouble.logFile.println(artifactId.toString() + ":");
                    OutputBouble.logFile.println("\t Missing dependency "+dependencyId.toString());
                    OutputBouble.logFile.println("\t Ignoring");
				} else {
                    actualDependencies[i++] = loader;
                }
			}
            if(actualDependencies.length>i){
                actualDependencies=Arrays.copyOf(actualDependencies,i);
            }

			theLoader.setDependencies(actualDependencies);

            extraDependencies.loaderAdded(theLoader);

			visitLoader(theLoader);
			return theLoader;
		} catch (Exception e) {
			OutputBouble.reportError(e);
			return theLoader;
		} finally {
			bouble.pop();
			if(bouble.isError) bouble.writeToParent();
		}
		//return null;
	}

	private DependentLoaderImplementation rawLoadFromMaven(Artifact artifactId) throws Exception{
        DependentLoaderImplementation theLoader=null;
        Result<File> localFileName=mavenRepositoryManager.getLocalFile(artifactId);

        if(localFileName.success()) theLoader=new DependentLoaderImplementation(artifactId, localFileName.val.getAbsoluteFile().toURI().toURL(), exposed, parentLoader);
        else  theLoader=new DependentLoaderImplementation(artifactId, exposed, parentLoader);
        setLoader(theLoader);

        List<Artifact> dependencies=mavenRepositoryManager.getDirectDependencies(artifactId);

        DependentLoaderImplementation[] actualDependencies=new DependentLoaderImplementation[dependencies.size()+theLoader.dependencies.length];
        int i=0;
        for(DependentLoaderImplementation dependencyFromConf:theLoader.dependencies){
            actualDependencies[i++]=dependencyFromConf;
        }
        for (Artifact dependencyId : dependencies) {

            DependentLoaderImplementation loader=enshureDependencyJarLoaded(artifactId,dependencyId);
            if(loader==null){
                OutputBouble.logFile.println(artifactId.toString() + ":");
                OutputBouble.logFile.println("\t Missing dependency "+dependencyId.toString());
                OutputBouble.logFile.println("\t Ignoring");
            } else {
                actualDependencies[i++] = loader;
            }
        }
        if(actualDependencies.length>i){
            actualDependencies=Arrays.copyOf(actualDependencies,i);
        }

        theLoader.setDependencies(actualDependencies);

        extraDependencies.loaderAdded(theLoader);

        visitLoader(theLoader);
        return theLoader;
    }


	private Artifact unify(Artifact artifact){
		for (Entry<String,String> unification : unified.entrySet()) {
			String groupFilter=unification.getKey();
			String version=unification.getValue();
			String artifactId=artifact.toString();

			if(artifactId.startsWith(groupFilter) && (!"".equals(version))){
				Artifact retVal=artifact.setVersion(version);
				return retVal;
			}
		}
		return artifact;
	}

	private Artifact unify(Artifact dependent, Artifact dependency){
		for (Entry<String,String> unification : unified.entrySet()) {
			String groupFilter=unification.getKey();
			String version=unification.getValue();
			String artifactId=dependency.toString();

			if("".equals(version) ){
				version=dependent.version;
			}
			if(artifactId.startsWith(groupFilter) && (!"".equals(version))){
				Artifact retVal=dependency.setVersion(version);
				return retVal;
			}
		}
		
		return dependency;
	}
	
	
	public void getJar(String artifactId){
		getJar(new Artifact(artifactId));
	}
	public void getJar(Artifact artifactId){
		OutputBouble bouble=OutputBouble.push();
		// Fetch jar and dependencies.
		try {
			Result<File> localFileName=mavenRepositoryManager.getLocalFile(artifactId);
			List<Artifact> dependencies=mavenRepositoryManager.getDirectDependencies(artifactId);
			
			int i=0;
			for (Artifact dependencyId : dependencies) {
				if(!loaderMap.containsKey(toString(dependencyId))){
					getJar(dependencyId);
				}
			}
		} catch (Exception e) {
			OutputBouble.reportError(e);
		}finally {
			bouble.pop();
			if(bouble.isError) bouble.writeToParent();
		}
	}
		
	public void logGraph(String logFile) {
		PrintWriter writer;
		try {
			writer = new PrintWriter(logFile, "UTF-8");
			writer.println("digraph loaders {");
			for (Entry<String, DependentLoaderImplementation> inMap : loaderMap.entrySet()) {
				DependentLoaderImplementation loader= inMap.getValue();
				for (DependentLoaderImplementation depedent : loader.dependencies) {
					if(depedent!=null)
						writer.println("\""+loader.artifact+"\" -> "+"\""+depedent.artifact+"\"");
					else 
						writer.println("\""+loader.artifact+"\" -> null");
				}
			}
			writer.println("}");
			writer.close();
		} catch (Exception e) {
			OutputBouble.reportError(e);
		}
	}
	
	private String toString(Artifact artifactId){
		return artifactId.toString();
//		return artifactId.getGroupId()+":"+artifactId.getArtifactId()+":"+artifactId.getClassifier()+":"+artifactId.getBaseVersion();
	}

	public void connect(String connectThis, String toThis) {
		List<DependentLoaderImplementation>  connectLoaders=new ArrayList<DependentLoaderImplementation>();
		List<DependentLoaderImplementation>  toLoaders=new ArrayList<DependentLoaderImplementation>();
		
		for (String stringKey : loaderMap.keySet()) {
			if(stringKey.startsWith(connectThis)){
				connectLoaders.add(loaderMap.get(stringKey));
			}
			if(stringKey.startsWith(toThis)){
				toLoaders.add(loaderMap.get(stringKey));
			}
		}
		
		for (DependentLoaderImplementation connectLoader : connectLoaders) {
			DependentLoaderImplementation[] newDependents=new DependentLoaderImplementation[ connectLoader.dependencies.length+toLoaders.size()];
			
			int i=0;
			for (DependentLoaderImplementation oldDependency : connectLoader.dependencies) {
				newDependents[i++]=oldDependency;
			}
			for (DependentLoaderImplementation toLoader : toLoaders) {
				newDependents[i++]=toLoader;
			}
			connectLoader.dependencies=newDependents;
		}
	}

	public File getAsFile(String resourceId) {
		return getAsFile(new Artifact(resourceId));
	}

	public File getAsFile(Artifact artifactId) {
		File overrideFile=findOverrideFile(artifactId);
		if(overrideFile!=null && overrideFile.exists()) return overrideFile;

		Result<File> localFileName=mavenRepositoryManager.getLocalFile(artifactId);
		if(localFileName.success()) return localFileName.val;

		return null;
	}

	public DependentLoaderImplementation cloneLoader(DependentLoader loader, String aditionalDependency){
        DependentLoaderImplementation otherAsImplementation=(DependentLoaderImplementation)loader;
       
		DependentLoaderImplementation newLoader=new DependentLoaderImplementation(otherAsImplementation.artifact,loader.getURLs(), exposed, otherAsImplementation.parent);
		newLoader.dependencies = otherAsImplementation.dependencies;
		
		if(!"".equals(aditionalDependency)){
			DependentLoaderImplementation dependency=enshureDependencyJarLoaded(otherAsImplementation.artifact, aditionalDependency);
			ArrayList<DependentLoaderImplementation> depList = new ArrayList<DependentLoaderImplementation>();
			for (DependentLoaderImplementation dependentLoaderImplementation : newLoader.dependencies) {
				depList.add(dependentLoaderImplementation);
			}
			depList.add(dependency);
			newLoader.setDependencies(depList.toArray(new DependentLoaderImplementation[depList.size()]));
		}
		visitLoader(newLoader);
		return newLoader;
	}
	
	public DependentLoaderImplementation cloneLoader(DependentLoader loader){
        DependentLoaderImplementation otherAsImplementation=(DependentLoaderImplementation)loader;
		DependentLoaderImplementation newLoader=new DependentLoaderImplementation(otherAsImplementation.artifact,loader.getURLs(), exposed, otherAsImplementation.parent);
		newLoader.setDependencies(otherAsImplementation.dependencies);

		visitLoader(newLoader);
		return newLoader;
	}

	public void expose(String what, String toPackage) {
		System.out.println("exposing " + what + " to " + toPackage);
        exposed.expose(new Artifact(what), toPackage);
	}

    public void registerDependency(String from, String to) {
        System.out.println("dependency " + from + " " + to);
        extraDependencies.add(from, to);
    }

	@Override
    public void registerUnpackedJar(String what, String ... where){}

	public void reloadJar(String artifactId){
		loaderMap.remove(artifactId);
		enshureJarLoaded(artifactId);
	}

	public void unifyGroupVersion(String group,String version){
		unified.put(group, version);
	}

    public void unpack(String artifact, File destination){
        File archiveFile=getAsFile(artifact);

        if(archiveFile == null || !archiveFile.exists()) return;
        if(archiveFile.isDirectory()){
            try {
                Files.walkFileTree(archiveFile.toPath(), new CopyDirVisitor(archiveFile,destination));
            } catch (Exception e){
				OutputBouble.reportError(e);
            }

        } else if(archiveFile.getName().endsWith("jar")) {
            destination.mkdirs();
            try{
            java.util.jar.JarFile jar = new java.util.jar.JarFile(archiveFile);
            java.util.Enumeration jarEnum=jar.entries();
            while ( jarEnum.hasMoreElements()){
                java.util.jar.JarEntry file = (java.util.jar.JarEntry) jarEnum.nextElement();
                java.io.File f = new java.io.File(destination + java.io.File.separator + file.getName());
                if (file.isDirectory()) { // if its a directory, create it
                    f.mkdir();
                    continue;
                }
                java.io.InputStream is = jar.getInputStream(file); // get the input stream
                java.io.FileOutputStream fos = new java.io.FileOutputStream(f);
                while (is.available() > 0) {  // write contents of 'is' to 'fos'
                    fos.write(is.read());
                }
                fos.close();
                is.close();
            }
            jar.close();
            } catch (Exception e){

            }
        }
    }

    public class CopyDirVisitor extends SimpleFileVisitor<Path> {
        private Path fromPath;
        private Path toPath;
        private StandardCopyOption copyOption = StandardCopyOption.REPLACE_EXISTING;

        public CopyDirVisitor(File fromDir,File toDir){
            fromPath=fromDir.toPath();
            toPath=toDir.toPath();
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            Path targetPath = toPath.resolve(fromPath.relativize(dir));
            if(!Files.exists(targetPath)){
                targetPath.toFile().mkdirs();
               // Files.createDirectory(targetPath);
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.copy(file, toPath.resolve(fromPath.relativize(file)), copyOption);
            return FileVisitResult.CONTINUE;
        }
    }

	public String[] listRepositories(){
		return mavenRepositoryManager.listRepositories();
	}
	public String[] listArtifacts(String repository){
		return mavenRepositoryManager.listArtifacts(repository);
	}



	public void downloadFlat(String artifact, String copyToDir){
		OutputBouble bouble=OutputBouble.push();
		try {
			Artifact theArtifact=new Artifact(artifact);

			HashSet<Artifact> downloadThese=new HashSet<Artifact>();
			downloadThese.add(theArtifact);
			Stack<Artifact> downloadDependenciesOfThese=new Stack<Artifact>();
			downloadDependenciesOfThese.add(theArtifact);

			while(!downloadDependenciesOfThese.isEmpty()){
				Artifact getDependenciesOfThis=downloadDependenciesOfThese.pop();
				List<Artifact> dependencies=mavenRepositoryManager.getDirectDependencies(getDependenciesOfThis);
				for(Artifact a:dependencies){
					if(!downloadThese.contains(a)){
						downloadThese.add(a);
						downloadDependenciesOfThese.add(a);
					}
				}
			}

			File destination=new File(copyToDir);
			destination.mkdirs();

			for(Artifact a:downloadThese){
				File theFile=getAsFile(a);
				Files.copy(theFile.toPath(), new File(destination,theFile.getName() ).toPath());
			}
		} catch (Exception e) {
			OutputBouble.reportError(e);
		} finally {
			bouble.pop();
			if(bouble.isError) bouble.writeToParent();
		}
	}

	/*
	* Feature export
	* */
	private Map<String, FeatureOrder> featureOrders=new HashMap<String,FeatureOrder>();
	private String ordredFeatureVersion="";

    public void orderFeature(String id, String filter,String exportTo){
        featureOrders.put(id,new FeatureOrder(id, filter,new File(new File(exportTo),id).getAbsolutePath() ));
    }

    public void orderFeatureInclude(String id,String fileName){
        featureOrders.get(id).include(fileName);
    }
    public void orderFeatureDependency(String id,String dependsOnId){
        featureOrders.get(id).addDependency(dependsOnId);
    }
    public void orderFeatureVersion(String version){
        ordredFeatureVersion=version;
    }
    public void exportFeatures(String[] ids) throws IOException{
        for(String id:ids){
            enshureJarLoaded(id);
        }

        for (FeatureOrder newFeature : featureOrders.values()) {
            for (Entry<String, DependentLoaderImplementation> inMap : loaderMap.entrySet()) {
                DependentLoaderImplementation loader = inMap.getValue();
                newFeature.addIfMatches(loader);

            }
        }

        Map<String,String> reuseOldMap=new HashMap<String, String>();
        for (FeatureOrder newFeature : featureOrders.values()) {
            String reuseOldVersion= newFeature.checkReuseOld(ordredFeatureVersion);
            if(reuseOldVersion!=null){
                reuseOldMap.put(newFeature.id,reuseOldVersion);
            }
        }

        for (FeatureOrder newFeature : featureOrders.values()) {
            String reuse=reuseOldMap.get(newFeature.id);
            if(reuse==null){
                newFeature.create(ordredFeatureVersion, reuseOldMap);
            } else {

            }
        }
    }
}
