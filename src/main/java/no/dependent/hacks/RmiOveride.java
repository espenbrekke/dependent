package no.dependent.hacks;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.rmi.server.RMIClassLoader;
import java.rmi.server.RMIClassLoaderSpi;

public class RmiOveride extends RMIClassLoaderSpi {
	public static void setLoader(ClassLoader loader) throws Exception {
		Field provider=RMIClassLoader.class.getDeclaredField("provider");
		provider.setAccessible(true);
		
		Field modifiersField = Field.class.getDeclaredField("modifiers");
		modifiersField.setAccessible(true);
	    modifiersField.setInt(provider, provider.getModifiers() & ~Modifier.FINAL);
		
		RMIClassLoaderSpi replaced=(RMIClassLoaderSpi) provider.get(null);
				
		provider.set(null, new RmiOveride(loader,replaced));
	}
	
	private final ClassLoader loader;
	private final RMIClassLoaderSpi replaced;
	private RmiOveride(ClassLoader loader, RMIClassLoaderSpi replaced){
		this.loader=loader;
		this.replaced=replaced;
	}
	
	@Override
	public Class<?> loadClass(String codebase, String name,
			ClassLoader defaultLoader) throws MalformedURLException,
			ClassNotFoundException {
		return replaced.loadClass(codebase,name,loader);
	}

	@Override
	public Class<?> loadProxyClass(String codebase, String[] interfaces,
			ClassLoader defaultLoader) throws MalformedURLException,
			ClassNotFoundException {
		
		return replaced.loadProxyClass(codebase,interfaces,loader);
	}

	@Override
	public ClassLoader getClassLoader(String codebase)
			throws MalformedURLException {
		return loader;
	}

	@Override
	public String getClassAnnotation(Class<?> cl) {
		return replaced.getClassAnnotation(cl);
	}

}
