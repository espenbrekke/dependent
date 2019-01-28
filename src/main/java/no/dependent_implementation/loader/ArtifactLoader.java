package no.dependent_implementation.loader;

import no.dependent_implementation.feature.FeaturePart;
import sun.misc.Resource;

import java.io.IOException;

public class ArtifactLoader extends PackageAwareLoaderPart {
    private final ClassLoader[] loaderEnvironment;

    public ArtifactLoader(FeaturePart loadFrom, ClassLoader[] loaderEnvironment){
        super(loadFrom);
        this.loaderEnvironment=loaderEnvironment;
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
    @Override
    protected Class<?> findClass(final String name)
            throws ClassNotFoundException
    {
        throw new ClassNotFoundException(name);
    }

    public synchronized Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException {
        // (1) Check our previously loaded local class cache
        Class<?> clazz=findLoadedClass0(name);
        if (clazz != null) return clazz;

        Class definedNow=super.loadClass(name,resolve);
        if(definedNow!=null) return definedNow;

        for(ClassLoader envLoader:loaderEnvironment){
            if(envLoader instanceof ArtifactLoader){
                definedNow=((ArtifactLoader) envLoader).superLoadClass(name, resolve);
            } else {
                definedNow=envLoader.loadClass(name);
            }
            if(definedNow!=null){
                rememberClass(name,definedNow);
                return definedNow;
            }
        }

        throw new ClassNotFoundException(name);
    }

    private Class<?> superLoadClass(String name, boolean resolve)
            throws ClassNotFoundException {
        return super.loadClass(name,resolve);
    }
}
