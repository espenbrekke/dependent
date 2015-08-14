package no.dependent;

import sun.misc.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;

public interface ResourceFile {
	public void setChanged(boolean changed);
    public boolean isChanged();
	public InputStream getStream();

}
