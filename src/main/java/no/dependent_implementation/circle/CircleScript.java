package no.dependent_implementation.circle;

import no.dependent.DependentMain;
import no.dependent.OutputBouble;
import no.dependent.utils.Artifact;
import no.dependent_implementation.PropertiesEngine;
import no.dependent_implementation.ToRun;
import no.dependent_implementation.utils.Booter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public final class CircleScript {
    public static void applyScript(Circle applyTo, List<String> configFileContent, String[] mainArgs){
        try {
            PropertiesEngine props=applyTo.props;
            LinkedList<String> lines = new LinkedList<>();
            lines.addAll(configFileContent);

            Boolean runElse=false;
            AtomicReference<String[]> p=new AtomicReference<>();

            while (!lines.isEmpty()) {
                String _currentLine=lines.removeFirst();
                String sPrintCurrentLine =  props.replaceProperties(_currentLine);
                String sCurrentLine = _currentLine;

                try {
                    if(_currentLine.trim().startsWith("else")){
                        if(runElse){
                            _currentLine=_currentLine.replaceFirst("\\s*else\\s*","");
                            sPrintCurrentLine = "else "+_currentLine;
                            sCurrentLine = _currentLine;
                            runElse=false;
                        } else {
                            sPrintCurrentLine = "--"+_currentLine;
                            sCurrentLine = "";
                        }
                    }

                    if(_currentLine.trim().startsWith("if(") & _currentLine.contains(")")){
                        String[] ifAndRest=_currentLine.split("\\)",2);
                        String _insideIf=ifAndRest[0].replaceFirst("\\s*if\\(","");
                        String insideIf=props.replaceProperties(_insideIf);
                        if("".equals(insideIf)){
                            sCurrentLine="";
                            sPrintCurrentLine="--"+ifAndRest[0] + "== \"\")"+ifAndRest[1];
                            runElse=true;
                        } else {
                            sCurrentLine=ifAndRest[1].replaceFirst("\\s*","");
                            sPrintCurrentLine=ifAndRest[0]+ "== "+insideIf+")"+ifAndRest[1];
                            runElse=false;
                        }
                    }

                    sCurrentLine = props.replaceProperties(sCurrentLine);

                    System.out.println(sPrintCurrentLine);
                    if (checkFor("localstore", sCurrentLine, p)) {
                        String repoPath = get(p, 0);
                        String repoName = get(p, 1);
                        String groupFilter = get(p, 2);
                        applyTo.mavenRepositoryManager.addLocalStore(repoPath, repoName, groupFilter);
                    } else if (checkFor("featurestore", sCurrentLine, p)) {
                        applyTo.featureManager.addFeatureStore(get(p, 0));

                    } else if (checkFor("include", sCurrentLine, p)) {
                        lines.addAll(0, readFileLines(get(p, 0)));
                    } else if (checkFor("stop", sCurrentLine, p)) {
                        break;
                    } else if (checkFor("export", sCurrentLine, p, 2)) {
                        String value = applyJvmKeywords(get(p,1));
                        System.getProperties().setProperty(get(p, 0), (" " + value).replaceFirst("\\s+", ""));
                    } else if (checkFor("run", sCurrentLine, p)) {
                        String[] methodAndParams = p.get();
                        if (methodAndParams.length > 0) {
                            String[] artifactSlashMethod = methodAndParams[0].split("/", 2);

                            ToRun run = new ToRun(
                                    artifactSlashMethod[1],
                                    applyTo.loaderGraph.enshureJarLoaded(artifactSlashMethod[0]));
                            run.addMainArgs(mainArgs);
                            if (methodAndParams.length > 1) {
                                String[] args = Arrays.copyOfRange(methodAndParams, 1, methodAndParams.length);
                                run.addMainArgs(args);
                            }
                            run.start();
                        }
                    } else if (checkFor("loadproperties", sCurrentLine, p)) {
                        props.loadProperties(get(p,0));
                    } else if (checkFor("expose", sCurrentLine, p)) {
                        String[] whatToPackage =p.get();

                        if (whatToPackage.length == 1) {
                            applyTo.loaderGraph.expose(whatToPackage[0], "");
                        } else if (whatToPackage.length == 2) {
                            applyTo.loaderGraph.expose(whatToPackage[0], whatToPackage[1]);
                        }
                    } else if (checkFor("dump", sCurrentLine, p)) {
                        applyTo.loaderGraph.logGraph(get(p,0));
                    } else if (checkFor("orderFeatureAt", sCurrentLine, p)) {
                        String[] idFilterWhere = p.get();
                        if(idFilterWhere.length==3){
                            applyTo.loaderGraph.orderFeature(idFilterWhere[0],idFilterWhere[1],idFilterWhere[2]);
                        }
                    } else if (checkFor("orderFeatureDependency", sCurrentLine, p,2)) {
                        applyTo.loaderGraph.orderFeatureDependency(get(p,0),get(p,1));
                    } else if (checkFor("orderFeatureInclude", sCurrentLine, p,2)) {
                        applyTo.loaderGraph.orderFeatureInclude(get(p,0),get(p,1));
                    } else if (checkFor("orderFeatureVersion", sCurrentLine, p)) {
                        applyTo.loaderGraph.orderFeatureVersion(get(p,0));
                    } else if (checkFor("exportFeatures", sCurrentLine, p)) {
                        applyTo.loaderGraph.exportFeatures(p.get());
                    } else if (checkFor("dependency", sCurrentLine, p,2)) {
                        applyTo.loaderGraph.registerDependency(get(p,0),get(p,1));
                    } else if (checkFor("unify", sCurrentLine, p,2)) {
                        applyTo.loaderGraph.unifyGroupVersion(get(p,0),get(p,1));
                    } else if (checkFor("start", sCurrentLine, p)) {
                        Circle.create(applyTo,new Artifact(get(p,0)), get(p,1) );
                    }
                } catch (Throwable t) {
                    t.printStackTrace(OutputBouble.logFile);
                }
            }
        } finally {
        }
    }
    private static boolean checkFor(String command, String line, AtomicReference<String[]> parameters,int requiredLength){
        if(checkFor(command,line,parameters)){
            return parameters.get().length==requiredLength;
        } else return false;
    }
    private static boolean checkFor(String command, String line, AtomicReference<String[]> parameters){
        if((line+" ").startsWith(command+" ")){
            String whitoutCommand = line.replaceFirst(command+"\\s+", "").trim();
            String[] split=whitoutCommand.split("\\s+");
            parameters.set(split);
            return true;
        }
        return false;
    }

    private static String get(AtomicReference<String[]> strings, int index) {
        if (index < strings.get().length) {
            return strings.get()[index];
        } else return "";
    }
    private static String applyJvmKeywords(String what) {
        if (what.contains("currentdir()")) {
            Path currentRelativePath = Paths.get("");
            String s = currentRelativePath.toAbsolutePath().toString();
            return what.replace("currentdir()", s);
        }
        return what;
    }

    private static List<String> readFileLines(String fileName) {
        File file = new File(fileName);
        List<String> retVal = new LinkedList<>();
        if (!file.exists()) return retVal;
        BufferedReader br = null;
        try {
            FileReader reader = new FileReader(file);
            br = new BufferedReader(reader);

            String sCurrentLine = null;
            while ((sCurrentLine = br.readLine()) != null) {
                retVal.add(sCurrentLine);
            }
        } catch (Exception e) {

        }
        return retVal;
    }
}
