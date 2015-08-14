package no.dependent;

public class DependentStatics {
    private static final DependentLoaderGraph graph=DependentFactory.get().getGraph();
    public static DependentLoaderGraph getGraph(){
        return graph;
    }

    private static final DependentTracker tracker=DependentFactory.get().getTracker();
    public static DependentTracker getTracker(){
        return tracker;
    }

}
