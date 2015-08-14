package no.dependent;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.TimeUnit;

public interface DependentTracker {
    public void trackChanges(String filter,DependentTrackerChange callback);

    public void signalChange(String whatArtifactChanged, DependentTrackerChangeDescription whatHappened);
}
