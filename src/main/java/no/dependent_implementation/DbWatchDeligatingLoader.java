package no.dependent_implementation;

import java.io.IOException;
import java.io.InputStream;
import java.lang.Class;
import java.lang.ClassLoader;
import java.lang.ClassNotFoundException;
import java.lang.Override;
import java.lang.String;
import java.net.URL;
import java.util.Enumeration;

import sun.reflect.Reflection;

public class DbWatchDeligatingLoader extends ClassLoader {
	
	final ClassLoader fallbackLoader;
	public DbWatchDeligatingLoader(ClassLoader fallbackLoader) {
		this.fallbackLoader=fallbackLoader;
	}
	public DbWatchDeligatingLoader() {
		this(null);
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		ClassLoader callers=DbWatchDeligatingLoader.getCallerClassLoader();
		try {
			if(callers!=null) return callers.loadClass(name);
		} catch (ClassNotFoundException e){}
		
		if(fallbackLoader!=null) return fallbackLoader.loadClass(name);
		
		throw new ClassNotFoundException(name);
	}

	@Override
	public URL getResource(String name) {
		ClassLoader callers=DbWatchDeligatingLoader.getCallerClassLoader();
		URL resource=null;
		if(callers!=null) resource=callers.getResource(name); 
		if(resource==null && fallbackLoader!=null) resource=fallbackLoader.getResource(name); 
		return resource;

	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		ClassLoader callers=DbWatchDeligatingLoader.getCallerClassLoader();
		if(callers!=null) return callers.getResources(name);
		throw new IOException("Unable to aquire caller classloader");
	}

	@Override
	public InputStream getResourceAsStream(String name) {
		ClassLoader callers=DbWatchDeligatingLoader.getCallerClassLoader();
		InputStream stream=null;
		if(callers!=null) stream= callers.getResourceAsStream(name);
		if(stream==null && fallbackLoader!=null) stream=fallbackLoader.getResourceAsStream(name); 
		return stream;
	}

    static ClassLoader getCallerClassLoader() {
        // NOTE use of more generic Reflection.getCallerClass()
        Class caller = Reflection.getCallerClass(3);
        // This can be null if the VM is requesting it
        if (caller == null) {
            return DbWatchDeligatingLoader.class.getClassLoader();
        }

        ClassLoader candidate=caller.getClassLoader();
        if(candidate!=null) return candidate;
        
        return DbWatchDeligatingLoader.class.getClassLoader();
    }
	
}
