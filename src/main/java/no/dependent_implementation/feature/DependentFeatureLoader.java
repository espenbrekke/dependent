package no.dependent_implementation.feature;

import no.dependent.OutputBouble;
import no.dependent.utils.Artifact;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class DependentFeatureLoader extends URLClassLoader {
    public final Artifact artifact;
    public FeatureLayout featureLayout=FeatureLayout.namedDirectories;

    public DependentFeatureLoader(FeatureListing listing){
        super(wrap(listing.url));
        artifact=listing.id;
    }

    public DependentFeatureLoader(Artifact artifact, File featureFile) throws MalformedURLException {
        this(artifact, featureFile.toPath().toUri().toURL());
    }

    public DependentFeatureLoader(Artifact artifact, URL featureUrl){
        super(wrap(featureUrl));
        this.artifact=artifact;
    }

    private Set<String> entries=null;
    public Set<String> getEntries(){
        if(entries==null){
            HashSet<String> newEntries=new HashSet<String>();

            for(URL url:this.getURLs()){
                try {
                    File asFile = new File(url.toURI());
                    if (asFile.isDirectory()) {
                        try {
                            Files.walkFileTree(asFile.toPath(), new DependentFeatureLoader.AddStringVisitor(asFile, newEntries));
                        } catch (Exception e) {
                            OutputBouble.reportError(e);
                        }
                    } else if (asFile.exists() && asFile.getName().endsWith(".jar")) {
                        ZipFile zipFile = null;

                        try {
                            zipFile = new ZipFile(asFile);
                            Enumeration<? extends ZipEntry> e = zipFile.entries();
                            while (e.hasMoreElements()) {
                                ZipEntry entry = e.nextElement();
                                String entryName = entry.getName();
                                if (!entry.isDirectory()) newEntries.add(entryName);
                            }
                        } catch (IOException ioe) {
                        } finally {
                            try {
                                if (zipFile != null) {
                                    zipFile.close();
                                }
                            } catch (IOException ioe) {
                            }
                        }
                    }
                } catch (URISyntaxException e) {}
            }

            entries=newEntries;
        }
        return entries;
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

    private static URL[] wrap( URL jarUrl){
        URL[] retVal={jarUrl};
        return retVal;
    }

    public List<Artifact> getDependencies(){
        return new ArrayList<Artifact>();
    }
}
