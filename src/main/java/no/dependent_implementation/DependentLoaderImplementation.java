package no.dependent_implementation;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import no.dependent.DependentLoader;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

class DependentLoaderImplementation extends DependentLoader {
    public String tag="";

    public void tag(String tag){
        this.tag=tag;
    }

    public String getTag(){
        return tag;
    }

    public boolean inQuarantine=false;
	
	public final Artifact artifact;
    public String getArtifact(){
        return artifact.toString();
    }

    @Override
    public void extractTo(File targetRoot){
        Set<String> resources=getEntries();
        for(String resource:resources){
            try{
                InputStream inputStream=this.getResourceAsStream(resource);
                File targetFile=new File(targetRoot,resource);
                targetFile.getParentFile().mkdirs();
                java.io.FileOutputStream outputStream = new java.io.FileOutputStream(targetFile);
                while (inputStream.available() > 0) {  // write contents of 'is' to 'fos'
                    outputStream.write(inputStream.read());
                }
                outputStream.close();
                inputStream.close();
            } catch (Throwable e){
                DependentMainImplementation.reportError(e);
            }

        }
    }


    private static class AddStringVisitor extends SimpleFileVisitor<Path> {
        private final Path fromPath;
        private final Set<String> addTo;

        public AddStringVisitor(File fromDir, Set<String> addTo){
            fromPath=fromDir.toPath();
            this.addTo=addTo;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            String targetPath = fromPath.relativize(file).toString();
            addTo.add(targetPath);
            return FileVisitResult.CONTINUE;
        }
    }

    private Set<String> entries=null;
    public Set<String> getEntries(){
        if(entries==null){
            HashSet<String> newEntries=new HashSet<String>();

            for(URL url:this.getURLs()){
                File asFile=new File(url.getFile());
                if(asFile.isDirectory()){
                    try {
                        Files.walkFileTree(asFile.toPath(), new AddStringVisitor(asFile, newEntries));
                    } catch (Exception e){
                        DependentMainImplementation.reportError(e);
                    }
                } else if(asFile.exists() && asFile.getName().endsWith(".jar")){
                    ZipFile zipFile = null;

                    try {
                        zipFile = new ZipFile(asFile);
                        Enumeration<? extends ZipEntry> e = zipFile.entries();
                        while (e.hasMoreElements()) {
                            ZipEntry entry = e.nextElement();
                            String entryName = entry.getName();
                            if(!entry.isDirectory())  newEntries.add(entryName);
                        }
                    }
                    catch (IOException ioe) {
                    }
                    finally {
                        try {
                            if (zipFile!=null) {
                                zipFile.close();
                            }
                        }
                        catch (IOException ioe) {
                        }
                    }
                }
            }

            entries=newEntries;
        }
        return entries;
    }

	private final  Exposure exposure;
	
	protected ClassLoader proxyFor = null;
	protected ClassLoader parent = null;
    protected ClassLoader system = null;


    public boolean isInQuarantine(){
        return inQuarantine;
    }


    protected DependentLoaderImplementation[] dependencies=new DependentLoaderImplementation[0];

    protected String[] libraryPaths = new String[0];

    protected final DependentLoaderGraphImplementation graph;

    public DependentLoaderGraphImplementation getGraph(){
        return graph;
    }

    @Override
    public DependentLoader replaceInGraph(URL ... urls){
        DependentLoaderImplementation newLoader=new DependentLoaderImplementation(artifact, urls,exposure,parent);
        newLoader.dependencies=dependencies;
        graph.setLoader(newLoader);
        return newLoader;
    }

    public void addLibraryPath(String path){
    	String[] newLibraryPaths=Arrays.copyOf(libraryPaths, libraryPaths.length+1);
    	newLibraryPaths[newLibraryPaths.length-1]=path;
    	libraryPaths=newLibraryPaths;
    }

    @Override
    public void addDependency(String toLoader) {
        DependentLoaderImplementation other=graph.enshureJarLoaded(toLoader);
        if(other!=null){
            addDependency(other);
        }
    }

    public void addDependency(DependentLoader toLoader) {
        DependentLoaderImplementation toLoaderImplementation=(DependentLoaderImplementation)toLoader;

        Artifact check= toLoaderImplementation.artifact;
        for (DependentLoaderImplementation existing : dependencies) {
            if(existing.artifact.equals(check)) return;
        }

        DependentLoaderImplementation[] newDependencies=Arrays.copyOf(dependencies,dependencies.length+1);
        newDependencies[newDependencies.length-1]=toLoaderImplementation;
        dependencies=newDependencies;
    }
    
    public DependentLoaderImplementation addJarFileDependency(String artifact,URL jarFile) {

        Artifact checkedArtifact=new DefaultArtifact(artifact);
        for (DependentLoaderImplementation existing : dependencies) {
            if(existing.artifact.equals(checkedArtifact)) return null;
        }

        DependentLoaderImplementation toLoader=new DependentLoaderImplementation(checkedArtifact, jarFile, exposure, parent);
        DependentLoaderImplementation[] newDependencies=Arrays.copyOf(dependencies,dependencies.length+1);
        newDependencies[newDependencies.length-1]=toLoader;
        dependencies=newDependencies;
        return toLoader;
    }

    private String findLibrary(String path, String libName) {
		File p = new File(path,libName+".dll");
		if(p.exists()){
			return p.getAbsolutePath();
		} 
		p = new File(path, libName+ ".so");
		if(p.exists()){
			return p.getAbsolutePath();
		} 
		
		File[] list = new File(path).listFiles();
		for(File dir : list) {
			if(dir.isDirectory()) {
				try {
					String foundFile = findLibrary(dir.getCanonicalPath(), libName);
					if(foundFile != null) {
						return foundFile;
					}
				} catch (IOException e) {
				}
			}
		}
		return null;
    }
    
    @Override
    protected String findLibrary(String libName) {
    	for (String path : libraryPaths) {
    		String foundFile = findLibrary(path, libName);
    		if(foundFile != null) {
    			return foundFile;
    		}
    	}
    	return super.findLibrary(libName);
    }
    

    public DependentLoaderImplementation quarantine(){
    	inQuarantine=true;
    	return this;
    }
    
	static  boolean bDebugHuntOutput=false;
    
    /**
     * The cache of ResourceEntry for classes and resources we have loaded,
     * keyed by resource name.
     */
    protected Map<String, ResourceEntry> resourceEntries = new HashMap<String, ResourceEntry>();
    
    public String toString(){
        URL[] utls=getURLs();
    	return "DependentLoader("+artifact.toString()+ " at "+ (utls.length>0?utls[0]:" nowere")+")"+tag ;
    }
    
    /**
     * The list of not found resources.
     */
    protected Map<String, String> notFoundResources =
        new LinkedHashMap<String, String>() {
        private static final long serialVersionUID = 1L;
        @Override
        protected boolean removeEldestEntry(
                Map.Entry<String, String> eldest) {
            return size() > 1000;
        }
    };


    private abstract class Hunter<T>{
    	public T value=null;
		public abstract boolean hunt(String prey, ClassLoader field, boolean isDependent) throws ClassNotFoundException;
    }
    
    private void hunt(String prey, Hunter hunter){
    	if(proxyFor!=null){
        	try {
    		if(hunter.hunt(prey,proxyFor,false)) return;
        	} catch (ClassNotFoundException e) {
				//ignore
			}
    	}
    	
        // (1) Delegate to our parent
        if (parent != null){
        	try {
				if(hunter.hunt(prey,parent,false)) return;
			} catch (ClassNotFoundException e) {
				//ignore
			}
        }
        // (1) Delegate to exposure
        {
        	String callerClassName="";
        	StackTraceElement[] stackTrace=Thread.currentThread().getStackTrace();
        	for (int i=1; i < stackTrace.length; i++) {
				String candidateClassName=stackTrace[i].getClassName();
        		if(!candidateClassName.startsWith("no.dependent") && !candidateClassName.startsWith("java")){
        			callerClassName=candidateClassName;
        			break;
        		}
			}
        	
        	List<ClassLoader> exposedLoaders=exposure.getLoadersExposedToPackage(callerClassName);
        	for (ClassLoader exposedLoader : exposedLoaders) {
            	try {
    				if(hunter.hunt(prey,exposedLoader,true)) return;
    			} catch (ClassNotFoundException e) {
    				//ignore
    			}
			}
        }
        
        clearHunt(); 
        //Hunt in dependencies
        if(huntNoParent(prey,hunter)) return;


    }
    
    private String didntFind="";
    private void setDidntFind(String didntFind){
    	this.didntFind=didntFind;
    }
    private boolean checkDidntFind(String prey){
    	return didntFind.equals(prey);
    }
        
    private void clearHunt(){
    	if(checkDidntFind("clearing")){
    		return ;
    	}
        setDidntFind("clearing");
        for (DependentLoaderImplementation dependentLoader : dependencies) {
       		dependentLoader.clearHunt();
		}
    }
    
    private boolean huntNoParent(String prey, Hunter hunter){
    	if(didntFind.equals(prey)){
    		return false;
    	}
    	
    	if(proxyFor!=null){
        	try {
        		if(hunter.hunt(prey,proxyFor,false)){
        			setDidntFind("");
        			return true;
        		}
        	} catch (ClassNotFoundException e) {
				//ignore
			}
    	}
    	
    	setDidntFind(prey);
        // then Check this
        try {
			if(hunter.hunt(prey,this,true)){
				setDidntFind("");
				return true;
			}
		} catch (ClassNotFoundException e) {
			//Ignore
		}
        
        // (2) Search dependencies
        for (DependentLoaderImplementation dependentLoader : dependencies) {
       		if(dependentLoader.huntNoParent(prey,hunter)){
       			setDidntFind("");;
				return true;
			};
		}
        setDidntFind(prey);
        return false;
    }

    static DependentLoaderImplementation createClassFileLoader(String artifact, String[] directories, Exposure exposure, ClassLoader parentLoader) {
        File[] files=new File[directories.length];
        int i=0;
        for(String directory:directories){
            files[i]=new File(directory);
            i++;
        }
        return createClassFileLoader(artifact, files, exposure, parentLoader);
    }
    static DependentLoaderImplementation createClassFileLoader(String artifact, File[] directories, Exposure exposure, ClassLoader parentLoader){
        try {
            URL[] urls=new URL[directories.length];
            int i=0;
            for(File file:directories){
                urls[i]=file.toURI().toURL();
                i++;
            }
            Artifact af=null;
            try{
                String[] split=artifact.split(":",3);
                if(split.length==3)  af=new DefaultArtifact(artifact);
                else if(split.length==2)  af=new DefaultArtifact(split[0],split[1],"jar","?");
                else if(split.length==1)  af=new DefaultArtifact(split[0],"unknown","jar","?");
                else af=new DefaultArtifact("no.dbwatch.unnamed:unnamed_artifact:1");
            } catch (Exception e){
                //   e.printStackTrace();
            }
            DependentLoaderImplementation loader = new DependentLoaderImplementation(af,urls , exposure, parentLoader);

            return loader;
        } catch (Exception e){
            //e.printStackTrace();
            return null;
        }
    }


    public DependentLoaderImplementation(Artifact artifact, URL[] jarUrls, Exposure exposure, ClassLoader parentLoader) {
        super(jarUrls, parentLoader);
        this.artifact=artifact;
        this.parent = parentLoader;
        this.exposure=exposure;
        this.graph=exposure.getGraph();
        system = getSystemClassLoader();
        addJarDeclaredDependencies();
	}
    
	public DependentLoaderImplementation(Artifact artifact, URL jarUrl, Exposure exposure, ClassLoader parentLoader) {
        super(wrap(jarUrl), parentLoader);
        this.artifact=artifact;
        this.parent = parentLoader;
        this.exposure=exposure;
        this.graph=exposure.getGraph();
        system = getSystemClassLoader();
        addJarDeclaredDependencies();
	}

    public DependentLoaderImplementation(Artifact artifact, Exposure exposure, ClassLoader parentLoader) {
    	super(new URL[0], parentLoader);
    	this.artifact=artifact;
    	this.parent = parentLoader;
        this.exposure=exposure;
        this.graph=exposure.getGraph();
    	system = getSystemClassLoader();
        addJarDeclaredDependencies();
	}

	public DependentLoaderImplementation(Artifact artifactId, ClassLoader delegate, Exposure exposure, ClassLoader parentLoader) {
    	super(new URL[0], parentLoader);
           	
    	this.proxyFor=delegate;
    	this.artifact=artifactId;
    	this.parent = parentLoader;
        this.exposure=exposure;
        this.graph=exposure.getGraph();
    	system = getSystemClassLoader();
        addJarDeclaredDependencies();
	}

    private void addJarDeclaredDependencies(){
        InputStream conf=this.getResourceAsStream("dependent.conf");
        if(conf != null){
            try{
                BufferedReader ds = new BufferedReader(new InputStreamReader(conf, "UTF-8"));
                String line;
                while ((line = ds.readLine()) != null) // read a line, assign to c, then compare the new value of c to null
                {
                    String sCurrentLine=DependentMainImplementation.props.replaceProperties(line);
                    if(sCurrentLine.startsWith("dependency")){
                        String withoutDependency=sCurrentLine.replaceFirst("dependency", "").replaceFirst("\\s+", "");
                        addDependency(withoutDependency);
                    }
                }
            } catch (Exception e){

            }
        }
    }

	private static URL[] wrap( URL jarUrl){
    	URL[] retVal={jarUrl};
    	return retVal;
    }

    @Override
    public URL findResource(final String name) {
    	assert(debugPoint(name));
    	
        findResourceHunter.value=null;
        hunt(name, findResourceHunter);
        URL url = findResourceHunter.value;
        findResourceHunter.value=null;
        return url;
    }
    private URL superFindResource(final String name) {
    	return super.findResource(name);
    }


    private Hunter<URL> findResourceHunter=new Hunter<URL>() {   	
    	@Override
    	public boolean hunt(String name,ClassLoader loader, boolean isDependent) throws ClassNotFoundException {			
    		if(!isDependent){
    			return false;
    		}
    		
    		Enumeration<URL> otherResourcePaths;
    		value=((DependentLoaderImplementation)loader).superFindResource(name);
        	if(value!=null) return true;

    		return false;
    	}
    };
    
    

    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
        Vector<URL> result = new Vector<URL>();
        
        findResourcesHunter.value=result;
        hunt(name, findResourcesHunter);
        findResourcesHunter.value=null;
        return result.elements();
    }

    public Enumeration<URL> superFindResources(String name) throws IOException {
    	return super.findResources(name);
    }
    
    private Hunter<Vector<URL>> findResourcesHunter=new Hunter<Vector<URL>>() {   	
    	@Override
    	public boolean hunt(String name,ClassLoader loader, boolean isDependent) throws ClassNotFoundException {			
    		if(!isDependent){
    			return false;
    		} 
    		
    		Enumeration<URL> otherResourcePaths;
			try {
				otherResourcePaths = ((DependentLoaderImplementation)loader).superFindResources(name);
	        	while (otherResourcePaths.hasMoreElements()) {
	        		value.addElement(otherResourcePaths.nextElement());
	        	}
			} catch (IOException e) {
				//ignore
			}

    		return false;
    	}
    };
    

    
  
    @Override
    public URL getResource(String name) {
        URL url = null;
    	assert(debugPoint(name));
        // Search this jar        
        resourceHunter.value=null;
        hunt(name, resourceHunter);
        url = resourceHunter.value;

        return url;
    }

    private URL superGetResource(String name) {
    	return super.getResource(name);
    }

    private Hunter<URL> resourceHunter=new Hunter<URL>() {   	
    	@Override
    	public boolean hunt(String name,ClassLoader loader, boolean isDependent) throws ClassNotFoundException {			
    		if(!isDependent){
    			value=loader.getResource(name);
    			return value!=null;
    		} 
    		value=((DependentLoaderImplementation)loader).superGetResource(name);
    		return value!=null;
   	
    	}
    };
    

    
    /**
     * Find the resource with the given name, and return an input stream
     * that can be used for reading it.  The search order is as described
     * for <code>getResource()</code>, after checking to see if the resource
     * data has been previously cached.  If the resource cannot be found,
     * return <code>null</code>.
     *
     * @param name Name of the resource to return an input stream for
     */
    @Override
    public synchronized InputStream getResourceAsStream(String name) {
        InputStream stream = null;
    	assert(debugPoint(name));

    	
    	 resourceAsStreamHunter.value=null;
    	hunt(name, resourceAsStreamHunter);
        stream = resourceAsStreamHunter.value;

     //   bDebugHuntOutput=false;
        return stream;
    }
    
    private InputStream superGetResourceAsStream(String name) {
    	return super.getResourceAsStream(name);
    }

    private Hunter<InputStream> resourceAsStreamHunter=new Hunter<InputStream>() {   	
		@Override
		public boolean hunt(String name,ClassLoader loader, boolean isDependent) throws ClassNotFoundException {		
			
			if(!isDependent){
        		value=loader.getResourceAsStream(name);
        		return value!=null;
        	} 
        	value=((DependentLoaderImplementation)loader).superGetResourceAsStream(name);
    		return value!=null;
        	
		}
	};

    /**
     * Load the class with the specified name.  This method searches for
     * classes in the same manner as <code>loadClass(String, boolean)</code>
     * with <code>false</code> as the second argument.
     *
     * @param name Name of the class to be loaded
     *
     * @exception ClassNotFoundException if the class was not found
     */
    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {

        return (loadClass(name, false));

    }

    
    /**
     * Load the class with the specified name, searching using the following
     * algorithm until it finds and returns the class.  If the class cannot
     * be found, returns <code>ClassNotFoundException</code>.
     * <ul>
     * <li>Call <code>findLoadedClass(String)</code> to check if the
     *     class has already been loaded.  If it has, the same
     *     <code>Class</code> object is returned.</li>
     * <li>If the <code>delegate</code> property is set to <code>true</code>,
     *     call the <code>loadClass()</code> method of the parent class
     *     loader, if any.</li>
     * <li>Call <code>findClass()</code> to find this class in our locally
     *     defined repositories.</li>
     * <li>Call the <code>loadClass()</code> method of our parent
     *     class loader, if any.</li>
     * </ul>
     * If the class was found using the above steps, and the
     * <code>resolve</code> flag is <code>true</code>, this method will then
     * call <code>resolveClass(Class)</code> on the resulting Class object.
     *
     * @param name Name of the class to be loaded
     * @param resolve If <code>true</code> then resolve the class
     *
     * @exception ClassNotFoundException if the class was not found
     */
    @Override
    public synchronized Class<?> loadClass(String name, boolean resolve)  		
    		throws ClassNotFoundException {

        Class<?> clazz = null;
              
        try {
        	loadingHunter.value=null;
        	hunt(name, loadingHunter);
        	clazz=loadingHunter.value;
        	if(clazz!=null)
        		return clazz;
    
        	throw new ClassNotFoundException(name+" Not visible from "+toString());
    
        } finally{
			if (clazz!=null && resolve)
				resolveClass(clazz);    

        }

    }

    private Hunter<Class<?>> loadingHunter=new Hunter<Class<?>>() {   	
		@Override
		public boolean hunt(String name,ClassLoader loader, boolean isDependent) throws ClassNotFoundException {			
			if(!isDependent){
        		value=Class.forName(name, false, loader);
        		return value!=null;
        	} 
        	value=((DependentLoaderImplementation)loader).shallowLoadClass(name, false);
    		return value!=null;
        	
		}
	};
    
    private boolean debugPoint(String name) {
    	if(bDebugHuntOutput){
    		System.out.println("Hunting: "+name+"  in  "+ getURLs()[0]);
    	}
    	
    	int a=0;
    	if(name.contains("ServerContext")){
    		a=1;
    	}
    	return true;
	}

    
    
	private Class<?> shallowLoadClass(String name, boolean resolve){
    	assert(debugPoint(name));
    	
    	Class<?> clazz = null;
    	try {
    		// (1) Check our previously loaded local class cache   	
    		clazz=findLoadedClass0(name);
    		if (clazz != null) return (clazz);
        

    		// (2) Check our previously loaded class cache
    		clazz = findLoadedClass(name);
    		if (clazz != null) return (clazz);

    		// (3) Load from this       	
    		try {
				clazz = super.findClass(name);
				if (clazz != null){
					resolveClass(clazz);    
					return (clazz);
				}
			} catch (ClassNotFoundException e) {
				//Ignore
			}
    		
    		return null;
    	} finally {
			if (clazz!=null && resolve)
				resolveClass(clazz);  
    	}
    }
    

    
    /**
     * Finds the class with the given name if it has previously been
     * loaded and cached by this class loader, and return the Class object.
     * If this class has not been cached, return <code>null</code>.
     *
     * @param name Name of the resource to return
     */
    protected Class<?> findLoadedClass0(String name) {
    	assert(debugPoint(name));
        ResourceEntry entry = resourceEntries.get(name);
        if (entry != null) {
            return entry.loadedClass;
        }
        return (null);  // FIXME - findLoadedResource()

    }

	void setDependencies(DependentLoaderImplementation[] actualDependencies) {
		this.dependencies=actualDependencies;
	}
}
