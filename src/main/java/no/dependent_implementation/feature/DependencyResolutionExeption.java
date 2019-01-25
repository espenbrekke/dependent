package no.dependent_implementation.feature;

public class DependencyResolutionExeption extends Exception{
    public DependencyResolutionExeption(String message) {
        this(message,null);
    }
    public DependencyResolutionExeption(String message,Throwable cause){
        super(message,cause);
    }
}
