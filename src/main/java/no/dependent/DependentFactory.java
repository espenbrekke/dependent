package no.dependent;


import no.dependent.hacks.PathRewritingClassLoader;
import no.dependent_implementation.DependentFactoryImplementation;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.*;

public abstract class DependentFactory {
    public abstract ResourceFile resourceFile(String resourceId);
    public abstract DependentLoaderGraph getGraph();

    public abstract void executeScript(String[] script,String[] mainParams);

    public abstract Class mainClass();

    private static DependentFactory factoryImpl=null;
    public static DependentFactory get(){
        if(factoryImpl==null) factoryImpl=new DependentFactoryImplementation();
        return factoryImpl;
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
