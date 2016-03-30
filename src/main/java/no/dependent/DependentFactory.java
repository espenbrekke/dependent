package no.dependent;


import no.dependent.hacks.PathRewritingClassLoader;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.*;

public abstract class DependentFactory {
    public abstract ResourceFile resourceFile(String resourceId);
    public abstract DependentLoaderGraph getGraph();
    public abstract DependentTracker getTracker();

    public abstract RemoteRepositories remoteRepositories(String ... urls);

    public abstract void executeScript(String[] script,String[] mainParams);

    public abstract Class mainClass();

    private static DependentFactory factoryImpl=null;
    public static DependentFactory get(){
        if(factoryImpl!=null) return factoryImpl;
        try{
            Class<DependentFactory> factoryClass=(Class<DependentFactory> )dependentClassLoader().loadClass("no.dependent_implementation.DependentFactoryImplementation");
            Constructor<DependentFactory> constructor=factoryClass.getConstructor(ClassLoader.class);
            factoryImpl=constructor.newInstance(DependentFactory.class.getClassLoader());
            return factoryImpl;
        }
        catch(Throwable e){
            System.err.println("Fatal error:");
            e.printStackTrace();
            return null;
        }
    }

    private static ClassLoader _dependentClassLoader=null;
    private static ClassLoader dependentClassLoader(){
        if(_dependentClassLoader!=null) return _dependentClassLoader;
        Class mainImplementationClass=null;
        try{
            DependentFactory.class.getClassLoader().loadClass("no.dependent_implementation.DependentFactoryImplementation");
            _dependentClassLoader= DependentFactory.class.getClassLoader();
            return _dependentClassLoader;
        }
        catch(ClassNotFoundException e) {
        }

        if(DependentFactory.class.getClassLoader() instanceof URLClassLoader){
            _dependentClassLoader=new PathRewritingClassLoader("private",(URLClassLoader)DependentFactory.class.getClassLoader());
            return _dependentClassLoader;
        }

/*        try{
            ClassLoader thisLoader=DependentFactory.class.getClassLoader();

            File jarFile=getJarFile(DependentFactory.class);
            System.out.println(jarFile);
            File containingDir=jarFile.getParentFile();
            System.out.println(containingDir);

            String implementationName=jarFile.getName().replace(".jar","-implementation.jar");
            System.out.println(implementationName);
            File implementationJarFile=new File(containingDir,implementationName);
            System.out.println(implementationJarFile);

            URL[] dependent={implementationJarFile.toURI().toURL()};
//            _dependentClassLoader=new URLClassLoader(dependent);
            _dependentClassLoader=new URLClassLoader(dependent,DependentFactory.class.getClassLoader());
            return _dependentClassLoader;
        } catch(Throwable ee){
            System.err.println("Fatal error:");
            ee.printStackTrace();
            return null;
        }
        */
        return null;
    }

    private static File getJarFile(Class aclass) {
        URL url;
        String extURL;
        try {
            url = aclass.getProtectionDomain().getCodeSource().getLocation();
            // url is in one of two forms
            //        ./build/classes/   NetBeans test
            //        jardir/JarName.jar  froma jar
        } catch (SecurityException ex) {
            url = aclass.getResource(aclass.getSimpleName() + ".class");
            // url is in one of two forms, both ending "/com/physpics/tools/ui/PropNode.class"
            //          file:/U:/Fred/java/Tools/UI/build/classes
            //          jar:file:/U:/Fred/java/Tools/UI/dist/UI.jar!
        }

        // convert to external form
        extURL = url.toExternalForm();

        // prune for various cases
        if (extURL.endsWith(".jar")) {  // from getCodeSource
//            extURL = extURL.substring(0, extURL.lastIndexOf("/"));
        } else {  // from getResource
            String suffix = "/"+(aclass.getName()).replace(".", "/")+".class";
            extURL = extURL.replace(suffix, "");
            if (extURL.startsWith("jar:") && extURL.endsWith(".jar!"))
                extURL = extURL.substring(4, extURL.length()-1);
        }

        // convert back to url
        try {
            url = new URL(extURL);
        } catch (MalformedURLException mux) {
            // leave url unchanged; probably does not happen
        }

        // convert url to File
        try {
            return new File(url.toURI());
        } catch(URISyntaxException ex) {
            return new File(url.getPath());
        }
    }



}
