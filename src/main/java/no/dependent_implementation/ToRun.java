package no.dependent_implementation;

import no.dependent.DependentLoader;

import java.lang.reflect.Method;

class ToRun{
    public ToRun(String method, DependentLoader loader){
        this.loader=(DependentLoaderImplementation)loader;
        this.method=method;

        assert method!=null;
        assert loader!=null;
    }
    public String[] mainParams;
    public final DependentLoaderImplementation loader;
    public final String method;
    public void run() {
        try {
            int lastDotIndex=method.lastIndexOf(".");
            String methodClass = method.substring(0,lastDotIndex);
            String methodName = method.substring(lastDotIndex+1);

            Class<?> classToRun=loader.loadClass(methodClass);
            Method methodToRun=classToRun.getMethod(methodName, String[].class);
            methodToRun.invoke(null, (Object)mainParams);

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

}