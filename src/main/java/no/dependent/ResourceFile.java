package no.dependent;

import java.io.InputStream;

public interface ResourceFile {
	public void setChanged(boolean changed);
    public boolean isChanged();
	public InputStream getStream();

}
