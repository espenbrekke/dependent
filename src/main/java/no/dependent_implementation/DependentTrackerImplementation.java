package no.dependent_implementation;

import no.dependent.DependentFactory;
import no.dependent.DependentTrackerChange;
import no.dependent.DependentTrackerChangeDescription;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.TimeUnit;

class DependentTrackerImplementation implements no.dependent.DependentTracker {
    private static DependentTrackerImplementation theTracker=null;
    private WatchService watcherService=null;
    private Map<Path,WatchKey> watchedDirs=new HashMap();

    private Thread whatchTread=new Thread(){
        public void run() {
            final Set<TrackedWatcher> changedWatchers=new HashSet();
            while(true){
                try{
                    WatchKey key=watcherService.poll(1, TimeUnit.SECONDS);

                    if(key==null){
                        if(changedWatchers.size()>0) {
                            for (TrackedWatcher watcher : changedWatchers) {
                                try {
                                    DependentTrackerImplementation.get().signalChange(watcher, DependentTrackerChangeDescription.changed);
                                } catch (Throwable e) {
                                    e.printStackTrace();
                                }
                            }

                            changedWatchers.clear();
                        }
                    }else {
                        key.pollEvents();
                        TrackedWatcher watcher=getWatcher(key);
                        updateMap(watcher);

                        if(watcher!=null) changedWatchers.add(watcher);
                        key.reset();
                    }

                } catch (Throwable e){
                    e.printStackTrace();
                }
            }
        }
    };

    synchronized WatchService getWatchService(){
        if(watcherService==null){
            try{
                watcherService = FileSystems.getDefault().newWatchService();
                whatchTread.start();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        return watcherService;
    }

    private TrackedCallback[] trackers=new TrackedCallback[0];
    private Map<WatchKey, TrackedWatcher> whachers=new HashMap();


    public static DependentTrackerImplementation get(){
        if(theTracker==null){
            theTracker=new DependentTrackerImplementation();
        }
        return theTracker;
    }

    private void whatch(Path toWatch, String artifactId){
        System.out.println("whatching "+toWatch.toString());
        synchronized (this){
            TrackedWatcher watcher=whachers.get(toWatch);
            if(watcher==null){
                watcher=new TrackedWatcher(toWatch,watchRecursive(toWatch));

                for(WatchKey key:watcher.watchKeys){
                    whachers.put(key,watcher);
                }
            }
            watcher.addArtifact(artifactId);
        }
    }

    public void trackChanges(String filter,DependentTrackerChange callback){
        synchronized (this){
            trackers=Arrays.copyOf(trackers,trackers.length+1);
            trackers[trackers.length-1]=new TrackedCallback(filter,callback);
        }

        DependentLoaderImplementation[] loadedAndMatching=((DependentLoaderGraphImplementation) DependentFactory.get().getGraph()).getLoaded(filter);
        for(DependentLoaderImplementation l:loadedAndMatching) {
            URL[] urls=l.getURLs();
            for(URL url:urls){
                try {
                    Path path = Paths.get(url.toURI());
                    File asFile = path.toFile();
                    System.out.println(asFile.toString());
                    if (asFile.isDirectory()) {
                        whatch(path, filter);
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    };

    private void remove(TrackedCallback tracking){
        synchronized (this){
            int found=0;
            for (TrackedCallback t:trackers) {
                if(t==tracking) found++;
            }
            TrackedCallback[] newArray=new TrackedCallback[trackers.length-found];
            int to=0;
            for (TrackedCallback t:trackers) {
                if(t!=tracking){
                    newArray[to]=t;
                    to++;
                }
            }
            trackers=newArray;
        }
    };

    private TrackedWatcher getWatcher(WatchKey whatChanged){
        synchronized (this){
            return whachers.get(whatChanged);
        }
    }

    private void updateMap(TrackedWatcher watcher){
        if(watcher == null) return;
        synchronized (this) {
            watcher.watchKeys.clear();
            watcher.watchKeys.addAll(watchRecursive(watcher.directory));
        }
    }

    public void signalChange(TrackedWatcher watcher, DependentTrackerChangeDescription whatHappened) {
        for(String artifact: watcher.artifacts){
             signalChange(artifact,whatHappened);
        }
    }
    public void signalChange(String whatArtifactChanged, DependentTrackerChangeDescription whatHappened){
         TrackedCallback[] tTrackers=trackers;
        for (TrackedCallback t:tTrackers){
            DependentTrackerChange callback=t.callback.get();
            if(callback==null){
                remove(t);
            } else
            if(whatArtifactChanged.startsWith(t.filter) || t.filter.startsWith(whatArtifactChanged)){
                callback.artifactChanged(whatArtifactChanged,whatHappened);
            }
        }
    }

    private static class TrackedCallback{
        final String filter;
        final WeakReference<DependentTrackerChange> callback;

        TrackedCallback(String filter, DependentTrackerChange callback){
            this.filter=filter;
            this.callback=new WeakReference<DependentTrackerChange>(callback);
        }
    }

    private static class TrackedWatcher{

//        final Path directory;
        final Path directory;
        final List<WatchKey> watchKeys;
        final Set<String> artifacts=new HashSet();

        TrackedWatcher(Path directory, List<WatchKey> watchKeys){
            this.directory=directory;
            this.watchKeys=watchKeys;
        }

        synchronized TrackedWatcher addArtifact(String artifact){
            artifacts.add(artifact);
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TrackedWatcher that = (TrackedWatcher) o;

            if (directory != null ? !directory.equals(that.directory) : that.directory != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return directory != null ? directory.hashCode() : 0;
        }
    }

    private List<WatchKey> watchRecursive(Path directory) {
        final List<WatchKey> watchKeys=new ArrayList();
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                        throws IOException {
                    if (attrs.isDirectory()) {
                        if(watchedDirs.containsKey(dir)){
                            watchKeys.add(watchedDirs.get(dir));
                        } else {

                            System.out.println("Adding watch "+dir);
                            WatchKey newKey=dir.register(DependentTrackerImplementation.get().getWatchService(),
                                    StandardWatchEventKinds.ENTRY_CREATE,
                                    StandardWatchEventKinds.ENTRY_DELETE,
                                    StandardWatchEventKinds.ENTRY_MODIFY);
                            if(newKey!=null){
                                watchedDirs.put(dir, newKey);
                                watchKeys.add(newKey);
                            }
                        }


                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception e){
            e.printStackTrace();
        }
        return watchKeys;
    };


}
