package no.dependent_implementation;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import no.dependent.*;
import no.dependent_implementation.feature.Feature;

class DependentLoaderGraphImplementation implements DependentLoaderGraph{
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

    private final Map<String, DependentLoaderImplementation> loaderMap=new HashMap<String, DependentLoaderImplementation>();

    void setLoader(DependentLoaderImplementation loader){
		synchronized (loaderMap){
			this.loaderMap.put(loader.getArtifact(), loader);
		}
	}

	public Map<String, String> debugLoadingClassSegments=new HashMap<>();
	public String[] debugLoadingArtifacts=new String[0];

	public void debugLoading(String afact, String classSegment){
        debugLoadingClassSegments.put(afact,classSegment);
        debugLoadingArtifacts=Arrays.copyOf(debugLoadingArtifacts,debugLoadingArtifacts.length+1);
        debugLoadingArtifacts[debugLoadingArtifacts.length-1]=afact;
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
            FeatureManager dependencyManager, ClassLoader parentLoader ) {
		theGraph=new DependentLoaderGraphImplementation(dependencyManager, parentLoader);
		return theGraph;
	}

	public static DependentLoaderGraphImplementation get(ClassLoader parentLoader){
		if(theGraph==null){
			theGraph= DependentLoaderGraphImplementation.create(new FeatureManager(), parentLoader);
		}
		return theGraph;
	}

	final FeatureManager featureManager;


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

	DependentLoaderGraphImplementation(FeatureManager featureManager, ClassLoader parentLoader){
		this.featureManager=featureManager;
        this.parentLoader=parentLoader;
	}

	public DependentLoaderImplementation enshureJarLoaded(String artifactId){
		return getLoader(new Artifact(artifactId));
	}

	private DependentLoaderImplementation enshureDependencyJarLoaded(Artifact dependent, String dependency){
		return getLoader(unify(dependent,new Artifact(dependency)));
    }
	
	private DependentLoaderImplementation enshureDependencyJarLoaded(Artifact dependent, Artifact dependency){
			return getLoader(unify(dependent,dependency));
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


    public DependentLoaderImplementation getLoader(Artifact _artifactId){
		Artifact artifactId=unify(_artifactId);

        if("dependent".equals(artifactId.getArtifactId()) && "no.dbwatch".equals(artifactId.getGroupId())){
            return null;
        };

        DependentLoaderImplementation theLoader=findOverride(artifactId);
        if(theLoader!=null){
			visitLoader(theLoader);
			return theLoader;
		}

		if(loaderMap.containsKey(toString(artifactId))) return loaderMap.get(toString(artifactId));
		// Fetch jar and dependencies.
		OutputBouble bouble=OutputBouble.push();

		try {
		    Feature feature=featureManager.getFeature(artifactId);
		    if(feature != null){
                theLoader=new DependentLoaderImplementation(artifactId, exposed, parentLoader, featureManager.getFeature(artifactId), this);

                if(debugLoadingArtifacts.length>0){
                    String afString=theLoader.artifact.toString();
                    for (int i = 0; i < debugLoadingArtifacts.length; i++) {
                        if(afString.startsWith(debugLoadingArtifacts[i])){
                            theLoader.debugLoading(debugLoadingClassSegments.get(debugLoadingArtifacts[i]));
                        }
                    }
                }
                setLoader(theLoader);

                //extraDependencies.loaderAdded();

                Artifact[] dependencies=feature.getDirectDependencies(theLoader.artifact);

                DependentLoaderImplementation[] actualDependencies=new DependentLoaderImplementation[ dependencies.length ];
                int i=0;
                for (Artifact dependencyId : dependencies) {
                    DependentLoaderImplementation loader=getLoader(dependencyId);
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

                visitLoader(theLoader);
                return theLoader;
            } else {
                feature=featureManager.getFeature(artifactId);
		        return null;
            }
		} catch (Exception e) {
			OutputBouble.reportError(e);
			return theLoader;
		} finally {
			bouble.pop();
			if(bouble.isError) bouble.writeToParent();
		}
	}



	private Artifact unify(Artifact artifact){
		for (Entry<String,String> unification : unified.entrySet()) {
			String groupFilter=unification.getKey();
			String version=unification.getValue();
			String artifactId=artifact.toString();

			if(artifactId.startsWith(groupFilter) && (!"".equals(version))){
				Artifact retVal=new Artifact(
						artifact.getGroupId(),
						artifact.getArtifactId(),
						version,
                        artifact.getClassName());
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
				version=dependent.getVersion();
			}
			if(artifactId.startsWith(groupFilter) && (!"".equals(version))){
				Artifact retVal=new Artifact(
						dependency.getGroupId(),
						dependency.getArtifactId(),
						version,
                        dependency.getClassName());
				return retVal;
			}
		}
		
		return dependency;
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

	private File getAsFile(Artifact artifactId) {
		File overrideFile=findOverrideFile(artifactId);
		if(overrideFile!=null && overrideFile.exists()) return overrideFile;

/*		Result<File> localFileName=featureManager.getLocalFile(artifactId);
		if(localFileName.success()) return localFileName.val;
*/
		return null;
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
    public void registerUnpackedJar(String what, String ... where){
        DependentLoaderImplementation loader= DependentLoaderImplementation.createClassFileLoader(new Artifact(what), where, exposed, parentLoader);
		File[] files=new File[where.length];
		int i=0;
		for(String directory:where){
			files[i]=new File(directory);
			i++;
		}
		setLoader(loader);
        overridingLoaders.add(new Wildcards(what, loader, files));
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
}
