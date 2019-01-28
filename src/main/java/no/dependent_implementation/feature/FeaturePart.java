package no.dependent_implementation.feature;

import sun.misc.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.Enumeration;

public class FeaturePart {
    private final String filePrefix;
    private final String classPrefix;
    private final FeatureLoader loader;
    protected FeaturePart(String filePrefix, FeatureLoader loader){
        this.filePrefix=filePrefix;
        this.classPrefix=filePrefix.replace('/','.');
        this.loader=loader;
    }

    public Resource findClassResourceAsResource(String name){
        return loader.findClassResourceAsResource(classPrefix+"."+name);
    }
    public Resource findResourceAsResource(String name){
        return loader.findResourceAsResource(filePrefix+"/"+name);
    }
}
