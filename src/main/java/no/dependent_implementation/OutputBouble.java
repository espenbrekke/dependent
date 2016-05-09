package no.dependent_implementation;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class OutputBouble {
    private static final ThreadLocal<OutputBouble> theBouble = new ThreadLocal<OutputBouble>() ;

    public static int numberOfFErrors=0;
    static public PrintStream logFile;

    private static void increaseErrorCount(){
        numberOfFErrors=numberOfFErrors+1;
        log3("*** numberOfFErrors = "+numberOfFErrors);
    }

    private static PrintStream getLog(){
        OutputBouble bouble=theBouble.get();
        if(bouble==null) return logFile;
        return bouble.boubleStream;
    }

    public static void log2(String what){
        if(DependentMainImplementation.logLevel >=2){
            logFile.println(what);
        }
    }

    public static void log3(String what){
        if(DependentMainImplementation.logLevel >=2){
            logFile.println(what);
        }
    }

    public static void reportError(String preMessage, Throwable e){
        report(preMessage, e, true);
    }

    public static void reportError(Throwable e){
        report(e, true);
    }

    public static void report(String preMessage, Throwable e, boolean isError){
        write(preMessage, isError);
        report(e, isError);
    }

    private static void report(Throwable e, Boolean isError){
        Throwable cause = null;
        Throwable result = e;

        while(null != (cause = result.getCause())  && (result != cause) ) {
            result = cause;
        }

        if(DependentMainImplementation.logLevel >=4){
            write(e, isError);
        } else {
            if(e!=result){
                write(e.getMessage(), isError);
                write(result.getMessage(), isError);
            } else {
                write(e.getMessage(),isError);
            }
        }

    }

    private static void write(Throwable error, Boolean isError){
        OutputBouble bouble=theBouble.get();
        if(bouble==null){
            error.printStackTrace(logFile);
            if(isError) increaseErrorCount() ;
        } else {
            error.printStackTrace(bouble.boubleStream);
            if(isError) bouble.isError=true;
        }
    }

    private static void write(String message, Boolean isError){
        OutputBouble bouble=theBouble.get();
        if(bouble==null){
            logFile.println(message);
            if(isError) increaseErrorCount();
        } else {
            bouble.boubleStream.println(message);
            if(isError) bouble.isError=true;
        }
    }

    private OutputBouble outerBouble=theBouble.get();

    private ByteArrayOutputStream _boubleStream=new ByteArrayOutputStream();
    private PrintStream boubleStream=new PrintStream(_boubleStream);
    public Boolean isError=false;

    public static OutputBouble push(){
        OutputBouble newBouble=new OutputBouble();
        return newBouble;
    }

    public void pop(){
        theBouble.set(outerBouble);
    }

    public void writeToParent(){
        try{
            if(outerBouble==null){
                logFile.write(_boubleStream.toByteArray());
                if(isError) increaseErrorCount();
            } else {
                outerBouble.boubleStream.write(_boubleStream.toByteArray());
                if(isError) outerBouble.isError=true;
            }
        } catch (Exception e){

        }
    }

    public void close(){
        try{
            _boubleStream.close();
            boubleStream.close();
        } catch (Throwable t){

        }
    }
}
