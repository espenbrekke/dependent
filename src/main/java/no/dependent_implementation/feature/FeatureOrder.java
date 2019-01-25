package no.dependent_implementation.feature;

import no.dependent.DependentLoader;
import no.dependent.OutputBouble;
import no.dependent_implementation.DependentLoaderGraphImplementation;
import no.dependent_implementation.DependentLoaderImplementation;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class FeatureOrder {
    public final String id;
    public final String shortId;
    public final String filter;
    public final boolean positiveFilter;
    public final String exportTo;
    public Set<String> dependencies=new HashSet<String>();
    public Set<String> includeFiles=new HashSet<>();

    public void include(String fileName){
        includeFiles.add(fileName);
    }

    public FeatureOrder(String id, String filter,String exportTo){
        this.id=id;
        if(filter.startsWith("!")){
            this.filter=filter.substring(1);
            this.positiveFilter=false;
        } else {
            this.filter=filter;
            this.positiveFilter=true;
        }
        this.exportTo=exportTo;
        int lastDot=id.lastIndexOf('.');
        shortId=id.substring(lastDot+1);
    }

    public void addDependency(String dependsOn){
        dependencies.add(dependsOn);
    }

    public Set<DependentLoaderImplementation> loaders = new HashSet<DependentLoaderImplementation>();
    public void addIfMatches(DependentLoaderImplementation loader){
        if(positiveFilter == (loader.artifact.group.startsWith(filter))){
            loaders.add(loader);
        }
    }


    public String checkReuseOld(String ordredFeatureVersion) throws IOException {
        ArrayList<String> newOne=createIndex();
        File baseDir=new File(exportTo);
        baseDir.mkdirs();

        for(File old:baseDir.listFiles()){
            ArrayList<String> oldOne=readOldIndex(old);
            if(newOne.equals(oldOne)){
                int _index=old.getName().lastIndexOf('_');
                int dotIndex=old.getName().lastIndexOf('.');
                String oldVersion=old.getName().substring(_index+1,dotIndex);

                return oldVersion;
            }
        }
        return null;
    }

    public void create(String ordredFeatureVersion, Map<String,String> reuseOldMap) throws IOException {
        File baseDir=new File(exportTo);
        baseDir.mkdirs();

        File targetFile=new File(baseDir,shortId+"_"+ordredFeatureVersion+".zip");

        ArrayList<String> newOne=createIndex();

        FileOutputStream os=new FileOutputStream(targetFile, false);
        ZipOutputStream out = new ZipOutputStream(os);

        writeToZip(newOne,"feature.index", out);

        ArrayList<String> featureDelcaration=createFeatureDeclaration(ordredFeatureVersion,reuseOldMap);
        writeToZip(featureDelcaration,"feature.declaration", out);

        for(DependentLoaderImplementation loader:loaders){
            String group=loader.artifact.group;
            String artifactId=loader.artifact.id;
            String version=loader.artifact.version;
            String id=group+":"+artifactId+":"+version;

            String artifactBaseDir=loader.artifact.group+"."+loader.artifact.id+"."+loader.artifact.version;

            Set<String> resources=loader.getEntries();
            for(String resource:resources){
                InputStream inputStream=loader.getResourceAsStream(resource);
                String targetZipEntry=artifactBaseDir+"/"+resource;

                writeToZip(inputStream, targetZipEntry, out);
            }
        }

        for(String includeFile:includeFiles){
            File copyFrom=new File(includeFile);
            InputStream copyFromStream=new FileInputStream(copyFrom);

            writeToZip(copyFromStream, includeFile, out);

            copyFromStream.close();
        }

        out.close();
        os.close();
    }

    private void writeToZip(ArrayList<String> input,String targetZipEntry,ZipOutputStream out) throws IOException{
//        System.out.println(targetZipEntry);
        ZipEntry e = new ZipEntry(targetZipEntry);
        out.putNextEntry(e);

        String sep="";
        for(String line:input){
            byte[] lineBuff=(sep+line).getBytes();
            sep="\n";
            out.write(lineBuff);
        }

        out.closeEntry();
    }

    private void writeToZip(InputStream inputStream,String targetZipEntry,ZipOutputStream out) throws IOException{
//        System.out.println(targetZipEntry);
        ZipEntry e = new ZipEntry(targetZipEntry);
        out.putNextEntry(e);

        byte[] buffer=new byte[1024];
        boolean readOn=true;
        while(readOn){
            int readCount=inputStream.read(buffer);
            if(readCount!=-1){
                out.write(buffer,0,readCount);
            } else {
                readOn=false;
            }
        }

        out.closeEntry();
    }

    private ArrayList<String> readOldIndex(File zFile) throws IOException{
        if(!zFile.exists()) return null;
        InputStream indexStream=null;
        try{
            ZipFile zipFile = new ZipFile(zFile);
            ZipEntry indexEntry=zipFile.getEntry("feature.index");

            indexStream=zipFile.getInputStream(indexEntry);

            if(indexStream!=null){
                BufferedReader readIt=new BufferedReader(new InputStreamReader(indexStream));
                ArrayList<String> oldOne=new ArrayList<String>(readIt.lines().collect(Collectors.toList()));
                indexStream.close();
                return oldOne;
            }

        } catch (IOException e){
            if(indexStream!=null) try{indexStream.close();}catch (IOException ee){}
        }
        return null;
    }

    private ArrayList<String> _index=null;

    private ArrayList<String> createIndex() throws IOException {
        if(_index!=null) return _index;
        List<DependentLoaderImplementation> sortedLoaders = new ArrayList<DependentLoaderImplementation>(loaders);
        sortedLoaders.sort(Comparator.comparing(dependentLoaderImplementation -> dependentLoaderImplementation.getArtifact()));

        ArrayList<String> newOne=new ArrayList<String>();
        newOne.add("[");

        String indexSeperator="";
        for(DependentLoaderImplementation loader:sortedLoaders){
            String group=loader.artifact.group;
            String artifactId=loader.artifact.id;
            String version=loader.artifact.version;
            String id=group+":"+artifactId+":"+version;

            String artifactBaseDir=loader.artifact.group+"."+loader.artifact.id+"."+loader.artifact.version;

            newOne.add(indexSeperator+"{");
            newOne.add("    \"base\":\""+artifactBaseDir+"\",");
            newOne.add("    \"id\":\""+id+"\",");
            newOne.add("    \"dependencies\":[");
            String seperator="";
            for(DependentLoaderImplementation dep:loader.dependencies){
                String dependencyId=dep.getArtifact();
                newOne.add("        "+seperator+"\""+dependencyId+"\"");
                seperator=",";
            }
            newOne.add("    ]");
            newOne.add("}");
            indexSeperator=",";
        }

        newOne.add("]");

        _index=newOne;
        return _index;
    }

    private ArrayList<String> createFeatureDeclaration(String ordredFeatureVersion, Map<String,String> reuseOldMap){
        ArrayList<String> result=new ArrayList<String>();

        List<String> sortedDependencies=new ArrayList<String>(dependencies);
        sortedDependencies.sort(Comparator.comparing(v -> v));

        result.add("{");
        result.add("    \"id\":\""+id+"\",");
        result.add("    \"version\":\""+ordredFeatureVersion+"\",");
        result.add("    \"dependencies\":[");
        String seperator="";
        for(String dep:sortedDependencies){
            String version=reuseOldMap.get(dep);
            if(version==null) version=ordredFeatureVersion;
            result.add("        "+seperator+"\""+dep+":"+version+"\"");
            seperator=",";
        }
        result.add("    ]");
        result.add("}");

        return result;
    }
}
