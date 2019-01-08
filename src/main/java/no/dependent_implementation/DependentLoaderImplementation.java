package no.dependent_implementation;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import no.dependent.*;
import no.dependent_implementation.feature.Feature;

class DependentLoaderImplementation extends DependentLoader {
    private int loaderId=DependentLoaderGraphImplementation.getNewLoaderId();

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

    private Map<String,DependentLoaderImplementation> configuredChildren=null;
    private Map<String,DependentLoaderConfiguration> configs=new HashMap();


    private final Feature feature;
    private final  Exposure exposure;

    protected ClassLoader proxyFor = null;
    protected ClassLoader parent = null;
    protected ClassLoader system = null;

    private Throwable huntError=null;

    private String debugLoadingClassSegment=null;
    public void debugLoading(String classSegment){
        debugLoadingClassSegment=classSegment;
    }

    /* The context to be used when loading classes and resources */
    private final AccessControlContext acc;

    @Override
    public void extractTo(File targetRoot){
        String[] resources=getEntries();
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
                OutputBouble.reportError(e);
            }

        }
    }

    @Override
    public DependentLoaderConfiguration[] getConfigurations(){
        Collection<DependentLoaderConfiguration> values=configs.values();
        return values.toArray(new DependentLoaderConfiguration[values.size()]);
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

    public static DependentLoaderImplementation createClassFileLoader(Artifact what,String[] where,Exposure exposed,ClassLoader parentLoader){
        return null;
    }

    public String[] getEntries(){
        return feature.getEntries(artifact);
    }

    public boolean isInQuarantine(){
        return inQuarantine;
    }

    protected DependentLoaderImplementation[] dependencies=new DependentLoaderImplementation[0];

    protected String[] libraryPaths = new String[0];

    protected final DependentLoaderGraphImplementation graph;

    public DependentLoaderGraphImplementation getGraph(){
        return graph;
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
    protected Map<String, Class<?>> classCache = new ConcurrentHashMap<>();
    
    public String toString(){
    	return "DependentLoader("+artifact.toString()+")";
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
        public String name="";
        public Hunter name(String name){
            this.name=name;
            return this;
        }
		public abstract boolean hunt(String prey, ClassLoader field, boolean isDependent, T[] container) throws ClassNotFoundException;
    }
    
    private <T> boolean  hunt(String prey, Hunter<T> hunter, T[] container){
        if(proxyFor!=null){
        	try {
    		if(hunter.hunt(prey,proxyFor,false, container)) return true;
        	} catch (Throwable e) {
				//ignore
			}
    	}

        // (1) Delegate to our parent
        if (parent != null){
        	try {
				if(hunter.hunt(prey,parent,false, container)) return true;
			} catch (Throwable e) {
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
    				if(hunter.hunt(prey,exposedLoader,true, container)) return true;
    			} catch (Throwable e) {
    				//ignore
    			}
			}
        }

        //Hunt in dependencies
        if(huntNoParent(prey,hunter, container, new BitSet(512))) return true;

        return false;
    }
    
    private boolean huntNoParent(String prey, Hunter hunter, Object[] container, BitSet cuttof){
    	if(cuttof.get(loaderId)){
    		return false;
    	}
        cuttof.set(loaderId);
    	
    	if(proxyFor!=null){
        	try {
        		if(hunter.hunt(prey,proxyFor,false, container)){
        			return true;
        		}
        	} catch (Throwable e) {
				//ignore
			}
    	}
    	
        // then Check this
        try {
			if(hunter.hunt(prey,this,true, container)){
				return true;
			}
		} catch (Throwable e) {
			//Ignore
		}
        
        // (2) Search dependencies
        for (DependentLoaderImplementation dependentLoader : dependencies) {
       		if(dependentLoader.huntNoParent(prey,hunter, container,cuttof)){
				return true;
			};
		}
        return false;
    }

    public DependentLoaderImplementation(Artifact artifact, Exposure exposure, ClassLoader parentLoader, Feature feature, DependentLoaderGraphImplementation graphImplementation) {
        super(parentLoader);
        this.artifact=feature.resolveVersion(artifact);
        this.parent = parentLoader;
        this.exposure=exposure;
        this.graph=exposure.getGraph();
        this.feature=feature;
        this.acc = AccessController.getContext();
        system = getSystemClassLoader();
        readConfigurations();
        addJarDeclaredDependencies();
	}

    public void close() {
        try {
            Class clazz = java.net.URLClassLoader.class;
            java.lang.reflect.Field ucp = clazz.getDeclaredField("ucp");
            ucp.setAccessible(true);
            Object sun_misc_URLClassPath = ucp.get(this);
            java.lang.reflect.Field loaders =
                    sun_misc_URLClassPath.getClass().getDeclaredField("loaders");
            loaders.setAccessible(true);
            Object java_util_Collection = loaders.get(sun_misc_URLClassPath);
            for (Object sun_misc_URLClassPath_JarLoader :
                    ((java.util.Collection) java_util_Collection).toArray()) {
                try {
                    java.lang.reflect.Field loader =
                            sun_misc_URLClassPath_JarLoader.getClass().getDeclaredField("jar");
                    loader.setAccessible(true);
                    Object java_util_jar_JarFile =
                            loader.get(sun_misc_URLClassPath_JarLoader);
                    ((java.util.jar.JarFile) java_util_jar_JarFile).close();
                } catch (Throwable t) {
                    // if we got this far, this is probably not a JAR loader so skip it
                }
            }
        } catch (Throwable t) {
            // probably not a SUN VM
        }
        return;
    }

    private void readConfigurations(){
        InputStream conf=null;
        try{
            conf=this.getResourceAsStream("dependent.conf");
            if(conf!=null) configs=parseConfig(conf);
        } finally {
            try{
                if(conf!=null) conf.close();
            } catch (Throwable t){

            }
        }
    }

    public static Map<String,DependentLoaderConfiguration> parseConfig(InputStream conf) {
        int currentConf=0;
        Map<String,DependentLoaderConfiguration> result=new HashMap<String,DependentLoaderConfiguration>();
        if(conf != null){
            try{
                BufferedReader ds = new BufferedReader(new InputStreamReader(conf, "UTF-8"));
                String line;
                while ((line = ds.readLine()) != null) // read a line, assign to c, then compare the new value of c to null
                {
                    String sCurrentLine= DependentMainImplementation.props.replaceProperties(line);

                    String[] lineSplit=sCurrentLine.split("=", 2);
                    if(lineSplit.length==2){
                        int lastDot=lineSplit[0].lastIndexOf('.');
                        String configName="";
                        String property="";
                        if(lastDot==-1){
                            property=lineSplit[0];
                        } else {
                            configName=lineSplit[0].substring(0,lastDot);
                            property=lineSplit[0].substring(lastDot+1);
                        }
                        if("_".equals(configName)){
                            currentConf++;
                            configName=Integer.toString(currentConf);
                        } else if("|".equals(configName)){
                            configName=Integer.toString(currentConf);
                        }

                        String value=lineSplit[1];

                        DependentLoaderConfiguration changeThis=result.get(configName);
                        if(changeThis==null)changeThis=new DependentLoaderConfiguration(configName);
                        result.put(configName, changeThis.add(property,value.trim().replace("\"","")));
                    }
                }
            } catch (Exception e){

            }
        }
        return result;
    }

    private void addJarDeclaredDependencies(){
        DependentLoaderConfiguration defaultConf=configs.get("");
        if(defaultConf!=null){
            for(String dependency:defaultConf.get("dependency")){
                addDependency(dependency);
            }
        }
    }

	private static URL[] wrap( URL jarUrl){
    	URL[] retVal={jarUrl};
    	return retVal;
    }

    @Override
    public URL findResource(final String name) {

        URL[] container=new URL[1];

        if(hunt(name, findResourceHunter, container)){
            return container[0];
        } else {
            return null;
        }
    }
    private URL superFindResource(final String name) {
    	return super.findResource(name);
    }


    private Hunter<URL> findResourceHunter=new Hunter<URL>() {
    	@Override
    	public boolean hunt(String name,ClassLoader loader, boolean isDependent, URL[] container) throws ClassNotFoundException {
    		if(!isDependent){
    			return false;
    		}

            container[0]=((DependentLoaderImplementation)loader).superFindResource(name);
        	if(container[0]!=null) return true;

    		return false;
    	}
    }.name("findResourceHunter");

    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
        Vector<URL> result = new Vector<URL>();

        Vector<URL>[] container=new Vector[1];
        container[0]=result;
        hunt(name, findResourcesHunter, container);
        return result.elements();
    }

    public Enumeration<URL> superFindResources(String name) throws IOException {
        return super.findResources(name);
    }
    
    private Hunter<Vector<URL>> findResourcesHunter=new Hunter<Vector<URL>>() {   	
    	@Override
    	public boolean hunt(String name,ClassLoader loader, boolean isDependent, Vector<URL>[] container) throws ClassNotFoundException {
    		if(!isDependent){
    			return false;
    		} 
    		
    		Enumeration<URL> otherResourcePaths;
			try {
				otherResourcePaths = ((DependentLoaderImplementation)loader).superFindResources(name);
	        	while (otherResourcePaths.hasMoreElements()) {
                    container[0].addElement(otherResourcePaths.nextElement());
	        	}
			} catch (IOException e) {
				//ignore
                return false;
			}

    		return false;
    	}
    }.name("findResourcesHunter");
    

    @Override
    public URL getResource(String name) {
        // Search this jar
        URL[] container=new URL[1];
        if(hunt(name, resourceHunter, container)){
            return container[0];
        } else {
            return null;
        }
    }

    private URL superGetResource(String name) {
    	return super.getResource(name);
    }

    private Hunter<URL> resourceHunter=new Hunter<URL>() {   	
    	@Override
    	public boolean hunt(String name,ClassLoader loader, boolean isDependent, URL[] container) throws ClassNotFoundException {
    		if(!isDependent){
                container[0]=loader.getResource(name);
    			return container[0]!=null;
    		}
            container[0]=((DependentLoaderImplementation)loader).superGetResource(name);
    		return container[0]!=null;
    	}
    }.name("resourceHunter");
    

    
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
        InputStream[] container=new InputStream[1];
       	if(hunt(name, resourceAsStreamHunter,container)){
            return container[0];
        } else {
            return null;
        }
    }
    
    private InputStream superGetResourceAsStream(String name){
        try{
            return feature.getResourceAsStream(artifact, name);
        } catch (IOException e){
            return null;
        }
    }

    private Hunter<InputStream> resourceAsStreamHunter=new Hunter<InputStream>() {   	
		@Override
		public boolean hunt(String name,ClassLoader loader, boolean isDependent, InputStream[] container) throws ClassNotFoundException {
			
			if(!isDependent){
        		container[0]=loader.getResourceAsStream(name);
        		return container[0]!=null;
        	}
            container[0]=((DependentLoaderImplementation)loader).superGetResourceAsStream(name);
    		return container[0]!=null;
		}
    }.name("resourceAsStreamHunter");

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
        // (1) Check our previously loaded local class cache
        if(debugLoadingClassSegment!=null){
            if(name.contains(debugLoadingClassSegment)){
                DependentMain.breakPoint("DependentLoaderImplementation.loadClass("+name+","+resolve+")");
            }
        }

        Class<?> clazz=findLoadedClass0(name);
        if (clazz != null) return clazz;


        Class<?>[] container=new Class[1];
        if(hunt(name, loadingHunter, container)){
            if (container[0]!=null && resolve)
                resolveClass(container[0]);

            rememberClass(name, container[0]);
            return container[0];
        };
        var e=new ClassNotFoundException(name+" Not visible from "+toString(), huntError);
        huntError=null;
        throw e;
    }

    private Hunter<Class<?>> loadingHunter=new Hunter<Class<?>>() {   	
		@Override
		public boolean hunt(String name,ClassLoader loader, boolean isDependent, Class<?>[] container) throws ClassNotFoundException {
			if(!isDependent){
                container[0] = Class.forName(name, false, loader);
        		return container[0]!=null;
        	}
            container[0]=((DependentLoaderImplementation) loader).shallowLoadClass(name, false);
    		return container[0]!=null;
		}
    }.name("loadingHunter");
    
	private synchronized Class<?> shallowLoadClass(String name, boolean resolve){
        if(debugLoadingClassSegment!=null){
            if(name.contains(debugLoadingClassSegment)){
                DependentMain.breakPoint("DependentLoaderImplementation.shallowLoadClass("+name+","+resolve+")");
            }
        }
    	Class<?> clazz = null;
    	try {
    		// (2) Check our previously loaded class cache
    		clazz = findLoadedClass(name);
    		if (clazz != null) return (clazz);

    		// (3) Load from this
    		try {
				clazz = findClassInFeature(name);
				if (clazz != null){
					resolveClass(clazz);    
					return (clazz);
				}
			} catch (Throwable e) {
				//Ignore
			}
    		
    		return null;
    	} finally {
			if (clazz!=null && resolve)
				resolveClass(clazz);  
    	}
    }

    /**
     * Finds and loads the class with the specified name from the URL search
     * path. Any URLs referring to JAR files are loaded and opened as needed
     * until the class is found.
     *
     * @param name the name of the class
     * @return the resulting class
     * @exception ClassNotFoundException if the class could not be found,
     *            or if the loader is closed.
     * @exception NullPointerException if {@code name} is {@code null}.
     */
    protected Class<?> findClassInFeature(final String name)
            throws ClassNotFoundException
    {
        if(debugLoadingClassSegment!=null){
            if(name.contains(debugLoadingClassSegment)){
                DependentMain.breakPoint("DependentLoaderImplementation.findClassInFeature("+name+")");
            }
        }
        Throwable[] gotErr={null};
        final Class<?> result;
        try {
            result = AccessController.doPrivileged(
                    new PrivilegedExceptionAction<>() {
                        public Class<?> run() throws ClassNotFoundException {
                            try {
                            String path = name.replace('.', '/').concat(".class");
                            var byteArray=feature.getResourceAsByteArray(artifact, path);
                                if (byteArray != null) {
                                    return defineClass(name, byteArray, 0,byteArray.length);
                                } else {
                                    return null;
                                }
                            } catch (Throwable e) {
                                gotErr[0]=e;
                                return null;
                            }
                        }
                    }, acc);
        } catch (java.security.PrivilegedActionException pae) {
            throw (ClassNotFoundException) pae.getException();
        }
        if (result == null) {
            this.huntError=gotErr[0];
            throw new ClassNotFoundException(name, gotErr[0]);
        }
        return result;
    }

    /**
     * Finds the class with the given name if it has previously been
     * loaded and cached by this class loader, and return the Class object.
     * If this class has not been cached, return <code>null</code>.
     *
     * @param name Name of the resource to return
     */
    protected Class<?> findLoadedClass0(String name) {
        Class<?> cachedClass = classCache.get(name);
        if (cachedClass != null) {
            return cachedClass;
        }
        return (null);
    }

    protected void rememberClass(String name, Class<?> rememberThis){
        classCache.put(name, rememberThis);
    }


	void setDependencies(DependentLoaderImplementation[] actualDependencies) {
		this.dependencies=actualDependencies;
	}

    @Override
    public DependentLoader getConfigured(String configName) {
        DependentLoaderConfiguration config=configs.get(configName);
        if(config==null) return null;

        synchronized (configs) {
            if(configuredChildren==null) configuredChildren=new HashMap();

            DependentLoaderImplementation configuredLoader=configuredChildren.get(configName);
            DependentLoaderImplementation other=configuredLoader;
            if(other==null)other=new DependentLoaderImplementation(artifact, exposure,parent, feature, graph);
            other.setDependencies(this.dependencies);
            for(String dependency:config.get("dependency")){
                DependentLoaderImplementation depLoder=graph.getLoader(new Artifact(dependency));
                if(depLoder!=null){
                    other.addDependency(depLoder);
                }
            }
            if(configuredLoader==null) configuredChildren.put(configName, other);
            return other;
        }
    }
}
