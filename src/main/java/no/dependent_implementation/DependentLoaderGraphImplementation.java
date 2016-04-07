package no.dependent_implementation;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.Map.Entry;

import no.dependent.DependentLoader;
import no.dependent.DependentLoaderVisitor;
import no.dependent_implementation.utils.Booter;
import no.dependent.DependentLoaderGraph;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactDescriptorException;

class DependentLoaderGraphImplementation implements DependentLoaderGraph{
	private Set<String> unified=new HashSet<String>(); 
	
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
				DependentMainImplementation.report(e);
			}
		}
	}

	public static DependentLoaderGraphImplementation create(
			DependentRepositoryManager dependencyManager, ClassLoader parentLoader ) {
		theGraph=new DependentLoaderGraphImplementation(dependencyManager, parentLoader);
		return theGraph;
	}

	public static DependentLoaderGraphImplementation get(ClassLoader parentLoader){
		if(theGraph==null){
			theGraph= DependentLoaderGraphImplementation.create(new DependentRepositoryManager(), parentLoader);
		}
		return theGraph;
	}
	
	final  DependentRepositoryManager dependencyManager;

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

	DependentLoaderGraphImplementation(DependentRepositoryManager dependencyManager,  ClassLoader parentLoader){
		this.dependencyManager=dependencyManager;
        this.parentLoader=parentLoader;
	}
	
	public DependentLoaderImplementation nameArtifact(String artifactId, URL[] jar, Object... dependsOn){
		return nameArtifact(new DefaultArtifact(artifactId), jar, dependsOn);
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
				DependentLoaderImplementation candidate=enshureDependencyJarLoaded(artifactId, new DefaultArtifact((String)dependency));
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
	
	public void nameClassLoader(String artifactId, ClassLoader loader, boolean recursive){
		nameClassLoader(new DefaultArtifact(artifactId), loader, recursive);
	}

	public void nameClassLoader(Artifact artifactId, ClassLoader loader,boolean recursive){
		DependentLoaderImplementation theLoader=null;
		theLoader= new DependentLoaderImplementation(artifactId,loader, exposed,parentLoader);

		if(artifactId!=null){
			assert(theLoader!=null);
			setLoader(theLoader);
			assert(loaderMap.get(toString(artifactId))!=null);
		}
		if(recursive){
			try {
				List<Artifact> dependencies=dependencyManager.getDirectDependencies(artifactId);
				for (Artifact dependency : dependencies) {
					if(!loaderMap.containsKey(toString(dependency))){
						nameClassLoader(dependency, theLoader, recursive);
					}
				}
			} catch (Exception e) {
				DependentMainImplementation.report(e);
			}
		}
		visitLoader(theLoader);
	}

	public DependentLoaderImplementation enshureJarLoaded(String artifactId){
		return enshureJarLoaded(new DefaultArtifact(artifactId));
	}

	private DependentLoaderImplementation enshureDependencyJarLoaded(Artifact dependent, String dependency){
		return enshureJarLoaded(unify(dependent,new DefaultArtifact(dependency)));
    }
	
	private DependentLoaderImplementation enshureDependencyJarLoaded(Artifact dependent, Artifact dependency){
			return enshureJarLoaded(unify(dependent,dependency));
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
        DefaultArtifact asArtifact=new DefaultArtifact(filter);
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


    public DependentLoaderImplementation enshureJarLoaded(Artifact artifactId){
        if("dependent".equals(artifactId.getArtifactId()) && "no.dbwatch".equals(artifactId.getGroupId())){
            return null;
        }
        DependentLoaderImplementation theLoader=findOverride(artifactId);
        if(theLoader!=null){
			visitLoader(theLoader);
			return theLoader;
		}
		if(loaderMap.containsKey(toString(artifactId))) return loaderMap.get(toString(artifactId));
		// Fetch jar and dependencies.
		try {
			Result<File> localFileName=dependencyManager.getLocalFile(artifactId);
			
			if(localFileName.success()) theLoader=new DependentLoaderImplementation(artifactId, localFileName.val.toURI().toURL(), exposed, parentLoader);
			else  theLoader=new DependentLoaderImplementation(artifactId, exposed, parentLoader);
			setLoader(theLoader);
			
			List<Artifact> dependencies=dependencyManager.getDirectDependencies(artifactId);
			
			DependentLoaderImplementation[] actualDependencies=new DependentLoaderImplementation[dependencies.size()];
			int i=0;
			for (Artifact dependencyId : dependencies) {
				
				DependentLoaderImplementation loader=enshureDependencyJarLoaded(artifactId,dependencyId);
				if(loader==null){
                    Booter.logFile.println(artifactId.toString() + ":");
                    Booter.logFile.println("\t Missing dependency "+dependencyId.toString());
                    Booter.logFile.println("\t Ignoring");
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
			DependentMainImplementation.report(e, Booter.logFile);
			return theLoader;
		}
		//return null;
	}
	
	private Artifact unify(Artifact dependent, Artifact dependency){
		for (String groupFilter : unified) {
			if(dependent.getGroupId().startsWith(groupFilter) && dependency.getGroupId().startsWith(groupFilter)){
				Artifact retVal=new DefaultArtifact(
						dependency.getGroupId(),
						dependency.getArtifactId(),
						dependency.getClassifier(),
						dependency.getExtension(),
						// The important one
						dependent.getVersion());
				retVal.setProperties(dependency.getProperties());
				return retVal;
			}
		}
		
		return dependency;
	}
	
	
	public void getJar(String artifactId){
		getJar(new DefaultArtifact(artifactId));
	}
	public void getJar(Artifact artifactId){	
		// Fetch jar and dependencies.
		try {
			Result<File> localFileName=dependencyManager.getLocalFile(artifactId);
			List<Artifact> dependencies=dependencyManager.getDirectDependencies(artifactId);
			
			int i=0;
			for (Artifact dependencyId : dependencies) {
				if(!loaderMap.containsKey(toString(dependencyId))){
					getJar(dependencyId);
				}
			}
		} catch (Exception e) {
			DependentMainImplementation.report(e);
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
			DependentMainImplementation.report(e);
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
		Artifact artifactId=new DefaultArtifact(resourceId);

        File overrideFile=findOverrideFile(artifactId);
        if(overrideFile!=null && overrideFile.exists()) return overrideFile;

		Result<File> localFileName=dependencyManager.getLocalFile(artifactId);
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
        exposed.expose(new DefaultArtifact(what), toPackage);
	}

    public void registerDependency(String from, String to) {
        System.out.println("dependency " + from + " " + to);
        extraDependencies.add(from, to);
    }

	@Override
    public void registerUnpackedJar(String what, String ... where){
        DependentLoaderImplementation loader= DependentLoaderImplementation.createClassFileLoader(what, where, exposed, parentLoader);
		File[] files=new File[where.length];
		int i=0;
		for(String directory:where){
			files[i]=new File(directory);
			i++;
		}
		setLoader(loader);
        overridingLoaders.add(new Wildcards(what, loader, files));
    }

	public void reloadJar(String artifactId){
		loaderMap.remove(artifactId);
		enshureJarLoaded(artifactId);
	}

	public void unifyGroupVersion(String group){
		unified.add(group);
	}

    public void unpack(String artifact, File destination){
        File archiveFile=getAsFile(artifact);

        if(archiveFile == null || !archiveFile.exists()) return;
        if(archiveFile.isDirectory()){
            try {
                Files.walkFileTree(archiveFile.toPath(), new CopyDirVisitor(archiveFile,destination));
            } catch (Exception e){
				DependentMainImplementation.report(e);
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
		return dependencyManager.listRepositories();
	}
	public String[] listArtifacts(String repository){
		return dependencyManager.listArtifacts(repository);
	}

}
