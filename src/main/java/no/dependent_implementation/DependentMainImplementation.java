package no.dependent_implementation;

import no.dependent.DependentFactory;
import no.dependent.DependentMain;
import no.dependent_implementation.circle.Circle;
import no.dependent_implementation.circle.CircleScript;
import no.dependent_implementation.feature.DependentFeatureManager;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class DependentMainImplementation {
    private static DependentLoaderGraphImplementation loaderGraph = (DependentLoaderGraphImplementation) DependentFactory.get().getGraph();// DependentLoaderGraphImplementation.create(dependencyManager, DependentMain.class.getClassLoader());
    private static DependentRepositoryManager mavenRepositoryManager = loaderGraph.mavenRepositoryManager;
    private static DependentFeatureManager featureManager=loaderGraph.featureManager;

    public static void main(String[] args) {
        String configFileName = "dependent.conf";
        for (int i = 0; i < args.length; i++) {
            String string = args[i];
            if ("-config".equals(string) || "-c".equals(string)) {
                if (i + 1 < args.length) {
                    configFileName = args[i + 1];
                }
            }
        }


//		loaderGraph.nameClassLoader("no.dbwatch:dependent:jar:11.1.4", DependentMain.class.getClassLoader(), true);
        Thread.currentThread().setContextClassLoader(new DbWatchDeligatingLoader(DependentMain.class.getClassLoader()));

        Circle outerCircle=new Circle("outer", null);
        CircleScript.applyScript(outerCircle, readFileLines(configFileName), args);
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

    private static PrintStream printStream(ByteArrayOutputStream buffer, String targetFileName, PrintStream fallback) {
        try {
            File targetFile = new File(targetFileName).getAbsoluteFile();
            targetFile.getParentFile().mkdirs();
            FileOutputStream fos = new FileOutputStream(targetFile);
            buffer.flush();
            buffer.writeTo(fos);
            fos.flush();
            return new PrintStream(fos);
        } catch (Exception e) {

        }
        try {
            buffer.flush();
            buffer.writeTo(fallback);
            fallback.flush();
            return fallback;
        } catch (Exception e) {

        }
        return fallback;
    }

    private static String applyJvmKeywords(String what) {
        if (what.contains("currentdir()")) {
            Path currentRelativePath = Paths.get("");
            String s = currentRelativePath.toAbsolutePath().toString();
            return what.replace("currentdir()", s);
        }
        return what;
    }

    private static String get(String[] strings, int index) {
        if (index < strings.length) {
            return strings[index];
        } else return "";
    }

    private static String[] getFlags(String[] strings) {
        ArrayList<String> flags = new ArrayList<>();
        for (String check : strings) {
            if (check.startsWith("-")) flags.add(check.toLowerCase());
        }
        return flags.toArray(new String[flags.size()]);
    }



}
