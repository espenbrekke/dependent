package no.dependent_implementation;

import java.io.*;
import java.net.URI;

import no.dependent.DependentFactory;
import no.dependent.ResourceFile;
import sun.misc.IOUtils;

public class ResourceFileImplementation implements ResourceFile {
	final String resourceId;
    final boolean isFile;

	private boolean changed=true;


    public boolean isChanged(){
        return changed;
    }

	public ResourceFileImplementation(String resourceId){
		this.resourceId=resourceId;
        this.isFile=resourceId.toLowerCase().startsWith("file:/");
	}

	public String toString() {
		return "ResourceFile(resourceId: " + resourceId + ", isFile: " + isFile + ")"; 
	}
	
    protected String data(){
        try {
            byte[] result = IOUtils.readFully(getStream(),Integer.MAX_VALUE, true);
            return new String(result);
        } catch (Exception e){
            return e.toString();
        }
    }

	public boolean changed(){
		return changed;
	}
	
	public void setChanged(boolean changed){
		this.changed=changed;
	}
	
	public InputStream getStream(){
		File theFile;
        if(isFile) {
            try {
                theFile = new File(new URI(resourceId));
            } catch (Exception e){
                return null;
            }
        } else {
            theFile= ((DependentLoaderGraphImplementation) DependentFactory.get().getGraph()).getAsFile(resourceId);
        }

		if(theFile==null)return null;
		
		try {
			return new FileInputStream(theFile);
		} catch (FileNotFoundException e) {
			OutputBouble.reportError(e);
			//e.printStackTrace();
		}
		return null;
	}
}
