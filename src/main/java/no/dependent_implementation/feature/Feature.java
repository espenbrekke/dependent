package no.dependent_implementation.feature;

import no.dependent.Artifact;

import java.io.*;
import java.lang.reflect.Array;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


public class Feature {
    private final File featureFile;

    private JarFile jarFile;
    private Map<Artifact, Artifact[]> dependencies=new HashMap<Artifact, Artifact[]>();
    private Map<Artifact, String> prefixes=new HashMap<Artifact, String>();
    private Map<Artifact,Artifact> versionless=new HashMap<>();

    public Artifact[] getArtifacts(){
        var keys=dependencies.keySet();
        return keys.toArray(new Artifact[keys.size()]);
    }

    public Artifact[] getDirectDependencies(Artifact artifact) throws Exception {
        var result=dependencies.get(artifact);
        if(result!=null) return result;
        return new Artifact[0];
    }

    public Feature(File featureFile) throws IOException {
        this.featureFile=featureFile;
        jarFile=new JarFile(featureFile);
    }

    public Artifact resolveVersion(Artifact artifact){
        if(artifact.getVersion().equals("")) return versionless.get(artifact);
        return artifact;
    }

    public void index(){
        try{
            var afEntry=jarFile.getJarEntry("artifacts.txt");
            var str=jarFile.getInputStream(afEntry);
            var isReader = new InputStreamReader(str, "UTF-8"); // leave charset out for default
            var bReader = new BufferedReader(isReader);
            Artifact current=null;
            LinkedList<Artifact> _dependencies=null;
            String s="";
            while ((s = bReader.readLine()) != null) {
                if(s.startsWith("artifact:")){
                    if(current!=null){
                        dependencies.put(current, _dependencies.toArray(new Artifact[_dependencies.size()]));
                        versionless.put(current.setVersion(""),current);
                        prefixes.put(current,current.getGroupId()+"/"+current.getArtifactId()+"-"+current.getVersion()+"/");
                    }
                    current=new Artifact(s.substring("artifact:".length()));
                    _dependencies=new LinkedList<>();
                } else if(s.startsWith("->")){
                    _dependencies.add(new Artifact(s.substring("->".length())));
                }
            }
            if(current!=null){
                dependencies.put(current, _dependencies.toArray(new Artifact[_dependencies.size()]));
                versionless.put(current.setVersion(""),current);
                prefixes.put(current,current.getGroupId()+"/"+current.getArtifactId()+"-"+current.getVersion()+"/");
            }
            bReader.close();
        } catch (Exception e){

        }
    }

    /*
     * Check whether the resource URL should be returned.
     * Throw exception on failure.
     * Called internally within this file.
     */
    public static void check(URL url) throws IOException {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            URLConnection urlConnection = url.openConnection();
            Permission perm = urlConnection.getPermission();
            if (perm != null) {
                try {
                    security.checkPermission(perm);
                } catch (SecurityException se) {
                    // fallback to checkRead/checkConnect for pre 1.2
                    // security managers
                    if ((perm instanceof java.io.FilePermission) &&
                            perm.getActions().indexOf("read") != -1) {
                        security.checkRead(perm.getName());
                    } else if ((perm instanceof
                            java.net.SocketPermission) &&
                            perm.getActions().indexOf("connect") != -1) {
                        URL locUrl = url;
                        if (urlConnection instanceof JarURLConnection) {
                            locUrl = ((JarURLConnection)urlConnection).getJarFileURL();
                        }
                        security.checkConnect(locUrl.getHost(),
                                locUrl.getPort());
                    } else {
                        throw se;
                    }
                }
            }
        }
    }

    public String[] getEntries(Artifact artifact){
        var e=jarFile.entries();
        while(e.hasMoreElements()){
            var anEntry=e.nextElement();
            var name=anEntry.getName();
        }
        return new String[0];
    }

    public InputStream getResourceAsStream(Artifact artifact,String name) throws IOException{
        var prefix=prefixes.get(artifact);
        var jarEntry=jarFile.getJarEntry(prefix+name);
        if(jarEntry==null) return null;
        return jarFile.getInputStream(jarEntry);
    }

    public byte[] getResourceAsByteArray(Artifact artifact, String name) throws IOException{
        var prefix=prefixes.get(artifact);
        var jarEntry=jarFile.getJarEntry(prefix+name);
        if(jarEntry==null) return null;
        byte[] readTo=new byte[(int)jarEntry.getSize()];
        int bytesRead=0;
        var stream=jarFile.getInputStream(jarEntry);
        try{
            while(bytesRead<readTo.length){
                bytesRead=bytesRead+stream.read(readTo, bytesRead, readTo.length-bytesRead);
            }

            return readTo;
        } finally {
            if(stream!=null) stream.close();
        }
    }

}