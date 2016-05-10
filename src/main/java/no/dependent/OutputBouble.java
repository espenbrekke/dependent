package no.dependent;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class OutputBouble {
    private static final ThreadLocal<OutputBouble> theBouble = new ThreadLocal<OutputBouble>() ;

    public static int numberOfFErrors=0;
    static public PrintStream logFile;
    public static int logLevel=1;

    private OutputBouble(OutputBouble outer){
        outerBouble=outer;
    }

    private static void increaseErrorCount(){
        numberOfFErrors=numberOfFErrors+1;
        log3("*** numberOfFErrors = "+numberOfFErrors);
    }

    private static PrintStream getLog(){
        OutputBouble bouble=theBouble.get();
        if(bouble==null) return logFile;
        return bouble.boubleStream;
    }

    public static void log1(String what){
        if(logLevel >=1){
            logFile.println(what);
        }
    }

    public static void log2(String what){
        if(logLevel >=2){
            logFile.println(what);
        }
    }

    public static void log3(String what){
        if(logLevel >=2){
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
        if(e==null){
            write(new IllegalArgumentException("Trying to report null-message", e),true);
        } else {
            Throwable cause = null;
            Throwable result = e;

            while(null != (cause = result.getCause())  && (result != cause) ) {
                result = cause;
            }

            String message=e.getMessage();
            String sourceMessage=result.getMessage();

            if(logLevel >=4 || message==null){
                write(e, isError);
            } else {
                if(e!=result){
                    write(message, false);
                    write(sourceMessage, isError);
                } else {
                    write(message,isError);
                }
            }
        }
    }

    private static void write(Throwable error, Boolean isError){
        if(error==null){
            new IllegalArgumentException("Trying to write null-error").printStackTrace(logFile);
            increaseErrorCount();
        }
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
        if(message==null){
            new IllegalArgumentException("Trying to write null-message").printStackTrace(logFile);
            increaseErrorCount();
        }
        OutputBouble bouble=theBouble.get();
        if(bouble==null){
            logFile.println(message);
            if(isError) increaseErrorCount();
        } else {
            bouble.boubleStream.println(message);
            if(isError) bouble.isError=true;
        }
    }

    private final OutputBouble outerBouble;

    private ByteArrayOutputStream _boubleStream=new ByteArrayOutputStream();
    private PrintStream boubleStream=new PrintStream(_boubleStream);
    public Boolean isError=false;

    public static OutputBouble push(){
        OutputBouble outer=theBouble.get();
        OutputBouble newBouble=new OutputBouble(outer);
        theBouble.set(newBouble);
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
