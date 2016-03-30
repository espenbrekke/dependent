package no.dependent_implementation;

import no.dependent.*;
import no.dependent_implementation.utils.Booter;

import java.io.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by espen on 11/26/14.
 */
class DependentMainImplementation {
    private static DependentLoaderGraphImplementation loaderGraph=(DependentLoaderGraphImplementation)DependentFactory.get().getGraph();// DependentLoaderGraphImplementation.create(dependencyManager, DependentMain.class.getClassLoader());
    private static DependentRepositoryManager dependencyManager=loaderGraph.dependencyManager;

    public static void main(String[] args) {
        String configFileName="dependent.conf";
        for (int i = 0; i < args.length; i++) {
            String string = args[i];
            if("-config".equals(string) || "-c".equals(string) ){
                if(i+1 < args.length){
                    configFileName=args[i+1];
                }
            }
        }


//		loaderGraph.nameClassLoader("no.dbwatch:dependent:jar:11.1.4", DependentMain.class.getClassLoader(), true);
        Thread.currentThread().setContextClassLoader(new DbWatchDeligatingLoader(DependentMain.class.getClassLoader()));

        List<String> configFileContent=readFileLines(configFileName);
        executeScript(configFileContent, args);
        //	loaderGraph.logGraph();
    }

    public static void executeScript(List<String> script, String[] args){
        List<ToRun> methodsToRun=readConfig(script);

        for (ToRun methodToRun : methodsToRun) {
            methodToRun.mainParams=args;
            methodToRun.run();
        }
    };


    private static List<String> readFileLines(String fileName){
        File file=new File(fileName);
        List<String> retVal=new LinkedList<>();
        if(!file.exists()) return retVal;
        BufferedReader br = null;
        try {
            FileReader reader = new FileReader(file);
            br = new BufferedReader(reader);

            String sCurrentLine = null;
            while ((sCurrentLine=br.readLine())!= null){
                retVal.add(sCurrentLine);
            }
        } catch (Exception e ){

        }
        return retVal;
    }

    static PropertiesEngine props=new PropertiesEngine();

    private static List<ToRun> readConfig(List<String> configFileContent){
        PrintStream sysOut=System.out;
        ByteArrayOutputStream tmpSysOut=new ByteArrayOutputStream();
        System.setOut(new PrintStream(tmpSysOut));

        PrintStream sysErr=System.err;
        ByteArrayOutputStream tmpSysErr=new ByteArrayOutputStream();
        System.setErr(new PrintStream(tmpSysOut));


        List<ToRun> toRun=new ArrayList<ToRun>() ;

        BufferedReader br = null;
        String sCurrentLine = null;
        try
        {
/*            File file=new File(configFileName);
            if(!file.exists()) return toRun;
            FileReader reader= new FileReader(file);
            br = new BufferedReader(reader);*/

            LinkedList<String> lines=new LinkedList<>();
            lines.addAll(configFileContent);


            while (!lines.isEmpty())
            {
                sCurrentLine=props.replaceProperties(lines.removeFirst());

                try{
                    System.out.println(sCurrentLine);
                    if(sCurrentLine.startsWith("artifactsource")){
                        String artifactsource=sCurrentLine.replaceFirst("artifactsource\\s+", "").replaceAll("\\s+", " ");
                        String[] artifactsourceParts=artifactsource.split("\\s+", 2);
                        String artifactsourceUrl="";
                        String repoPath="";
                        if(artifactsourceParts.length>1){
                            artifactsourceUrl=artifactsourceParts[0];
                            repoPath=artifactsourceParts[1];
                            dependencyManager.addSource(repoPath,artifactsourceUrl);
                        }
                    } else if(sCurrentLine.startsWith("localstore")){
                        String wothoutLocalstore=sCurrentLine.replaceFirst("localstore\\s+", "");
                        String[] fileThenName = wothoutLocalstore.split("\\s+", 2);
                        if(fileThenName.length==2){
                            dependencyManager.addLocalStore(fileThenName[0],fileThenName[1]);
                        } else {
                            dependencyManager.addLocalStore(fileThenName[0],fileThenName[1]);
                        }

                    } else if(sCurrentLine.startsWith("include")){
                        String withoutInclude=sCurrentLine.replaceFirst("include", "").replaceFirst("\\s+", "");
                        lines.addAll(0,readFileLines(withoutInclude));
                    } else if(sCurrentLine.startsWith("stop")){
                        break;
                    } else if(sCurrentLine.startsWith("export")){
                        String withoutExport=sCurrentLine.replaceFirst("export", "").replaceFirst("\\s+", "");
                        String[] nameValue = withoutExport.split("\\s",2);
                        if(nameValue.length==2){
                            System.getProperties().setProperty(nameValue[0], (" "+nameValue[1]).replaceFirst("\\s+", ""));
                        }
                    } else if(sCurrentLine.startsWith("redirect")){
                        String withoutRedirect=sCurrentLine.replaceFirst("redirect", "").replaceFirst("\\s+", "");
                        String[] streamNameValue = withoutRedirect.split("\\s",2);
                        if(streamNameValue.length==2){
                            if("stdout".equals(streamNameValue[0].toLowerCase()) && tmpSysOut!=null){
                                System.setOut(printStream(tmpSysOut, streamNameValue[1],sysOut));
                                tmpSysOut=null;
                            } else if("stderr".equals(streamNameValue[0].toLowerCase()) && tmpSysErr!=null){
                                System.setErr(printStream(tmpSysErr, streamNameValue[1],sysErr));
                                tmpSysErr=null;
                            } else if("dependent-log".equals(streamNameValue[0].toLowerCase()) && tmpSysErr!=null){
                                Booter.setLogFilePlacement(new File(streamNameValue[1]));
                            }
                        }
                    } else
                    if(sCurrentLine.startsWith("mainloader")) {
                        String artifact = sCurrentLine.replaceFirst("mainloader", "").replaceAll("\\s+", "");
                        loaderGraph.nameClassLoader(artifact, DependentMain.class.getClassLoader(), true);
                    } else
                    if(sCurrentLine.startsWith("import")){
                        String toImport=sCurrentLine.replaceFirst("import", "").replaceAll("\\s+", "");
                        loaderGraph.enshureJarLoaded(toImport);
                    } else
                    if(sCurrentLine.startsWith("get")){
                        String toGet=sCurrentLine.replaceFirst("get", "").replaceAll("\\s+", "");
                        loaderGraph.getJar(toGet);
                    } else
                    if(sCurrentLine.startsWith("loadartifact")){
                        String toLoad=sCurrentLine.replaceFirst("loadartifact", "").replaceAll("\\s+", "");
                        loaderGraph.enshureJarLoaded(toLoad);
                    } else
                    if(sCurrentLine.startsWith("run")){
                        String theEssential=sCurrentLine.replaceFirst("run", "").replaceAll("\\s+", "");
                        String[] artifactSlashMethod=theEssential.split("/",2);
                        System.out.println(theEssential);
                        System.out.println(artifactSlashMethod[0]);
                        System.out.println(artifactSlashMethod[1]);
                        ToRun run=new ToRun(
                                artifactSlashMethod[1],
                                loaderGraph.enshureJarLoaded(artifactSlashMethod[0]));
                        toRun.add(run);
                    } else if(sCurrentLine.startsWith("loadproperties")){
                        String propertiesFileName=sCurrentLine.replaceFirst("loadproperties", "").replaceAll("\\s+", "");
                        props.loadProperties(propertiesFileName);
                    } else if(sCurrentLine.startsWith("expose")){
                        String withoutExpose=sCurrentLine.replaceFirst("expose", "").replaceFirst("\\s+", "");
                        String[] whatToPackage = withoutExpose.split("\\s+to\\s+",2);

                        if(whatToPackage.length==1){
                            loaderGraph.expose(whatToPackage[0],"");
                        } else if(whatToPackage.length==2){
                            loaderGraph.expose(whatToPackage[0],whatToPackage[1]);
                        }
                    } else if(sCurrentLine.startsWith("dump")){
                        String dumpFileName=sCurrentLine.replaceFirst("dump", "").replaceFirst("\\s+", "");
                        loaderGraph.logGraph(dumpFileName);
                    } else if(sCurrentLine.startsWith("dependency")){
                        String withoutDependency=sCurrentLine.replaceFirst("dependency", "").replaceFirst("\\s+", "");
                        String[] fromTo = withoutDependency.split("\\s",2);
                        if(fromTo.length==2){
                            loaderGraph.registerDependency(fromTo[0],fromTo[1]);
                        }
                    } else
                    if(sCurrentLine.startsWith("unify")){
                        String group=sCurrentLine.replaceFirst("unify", "").replaceFirst("\\s+", "");
                        loaderGraph.unifyGroupVersion(group);
                    } else if(sCurrentLine.startsWith("unpacked")){
                        String withoutUnpacked=sCurrentLine.replaceFirst("unpacked", "").replaceFirst("\\s+", "");
                        String[] whatWhere = withoutUnpacked.split("\\s",2);
                        if(whatWhere.length==2){
                            loaderGraph.registerUnpackedJar(whatWhere[0],whatWhere[1]);
                        }
                    }
                } catch (Exception e){
                    e.printStackTrace(Booter.logFile);
                }
            }
        }
        finally
        {

            if(tmpSysOut!=null){
                System.setOut(sysOut);
            }
            if(tmpSysErr!=null){
                System.setErr(sysErr);
            }
            try
            {
                if (br != null)
                    br.close();
            } catch (IOException ex)
            {
                ex.printStackTrace(Booter.logFile);
            }
        }
        return toRun;
    }


    private static PrintStream printStream(ByteArrayOutputStream buffer, String targetFileName, PrintStream fallback){
        try {
            File targetFile = new File(targetFileName).getAbsoluteFile();
            targetFile.getParentFile().mkdirs();
            FileOutputStream fos = new FileOutputStream(targetFile);
            buffer.flush();
            buffer.writeTo(fos);
            fos.flush();
            return new PrintStream(fos);
        } catch (Exception e){

        }
        try {
            buffer.flush();
            buffer.writeTo(fallback);
            fallback.flush();
            return fallback;
        } catch (Exception e){

        }
        return fallback;
    }
}
