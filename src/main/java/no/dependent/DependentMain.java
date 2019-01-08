package no.dependent;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class DependentMain {
	public static void main(String[] args) {
        Class<?> mainImplementationClass=DependentFactory.get().mainClass();
        try {
            Class<?>[] mainParamDef = {String[].class};
            Method mainImpl = mainImplementationClass.getMethod("main", mainParamDef);
            Object[] argsWrapped={args};
            mainImpl.setAccessible(true);
            mainImpl.invoke(null,argsWrapped);
        } catch(Exception e){
            System.err.println("Fatal error:");
            e.printStackTrace();
        }
	}

    public static boolean executeScript(String script){
        Class<?> mainImplementationClass=DependentFactory.get().mainClass();
        try {
            Class<?>[] mainParamDef = {String.class};
            Method executeImpl = mainImplementationClass.getMethod("executeScript", mainParamDef);
            Object[] argsWrapped={script};
            executeImpl.setAccessible(true);
            return (boolean)executeImpl.invoke(null,argsWrapped);
        } catch(Exception e){
            System.err.println("Fatal error:");
            e.printStackTrace();
        }
        return false;
    }

    public static void breakPoint(String name){
	    String breakHere=name;
    }
}
