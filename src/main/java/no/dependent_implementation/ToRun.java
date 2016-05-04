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
    private String[] mainParams={};
    public final DependentLoaderImplementation loader;
    public final String method;

    public void addMainArgs(String[] newParams){
        String[] newArray=new String[mainParams.length+newParams.length];
        for (int i = 0; i < mainParams.length; i++) {
            newArray[i]=mainParams[i];
        }
        for (int i = mainParams.length; i < newArray.length; i++) {
            newArray[i]=newParams[i-mainParams.length];
        }
        mainParams=newArray;
    }

    public void run() {
        try {
            int lastDotIndex=method.lastIndexOf(".");
            String methodClass = method.substring(0,lastDotIndex);
            String methodName = method.substring(lastDotIndex+1);

            Class<?> classToRun=loader.loadClass(methodClass);
            Method methodToRun=classToRun.getMethod(methodName, String[].class);
            methodToRun.invoke(null, (Object)mainParams);

        } catch (Throwable e) {
            OutputBouble.reportError(e);
        }
    }

}