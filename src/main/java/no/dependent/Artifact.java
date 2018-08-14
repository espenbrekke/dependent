package no.dependent;

public class Artifact {
    private final int _hashcode;
	private final String groupid;
	private final String artifactid;
	private final String version;
	private final String className;
	public final String _featurePathPrefix;
	
	public Artifact(String stringRepresentation){
		try{
			String[] tokenized=stringRepresentation.split(":", 4);
            this.groupid=tokenized[0];
            if(tokenized.length>1) this.artifactid=tokenized[1]; else this.artifactid="";
            if(tokenized.length>2) this.version=tokenized[2]; else this.version="";
            if(tokenized.length>3) this.className=tokenized[3]; else this.className="jar";

            this._featurePathPrefix=groupid+"/"+artifactid+"-"+version+"/";
		} catch(Exception e) {
			throw new IllegalArgumentException("Parse error: "+stringRepresentation);
		}
        this._hashcode=_hCode();
	}

    public Artifact(String groupid, String artifactid, String version, String className){
        this.groupid=groupid;
        this.artifactid=artifactid;
        this.version=version;
        this.className=className;
        this._featurePathPrefix=groupid+"/"+artifactid+"-"+version+"/";
        this._hashcode=_hCode();
    }

    private int _hCode(){
	    int t=0;
        t=t*31+groupid.hashCode();
        t=t*31+artifactid.hashCode();
        t=t*31+version.hashCode();
        t=t*31+className.hashCode();
        return t;
    }
	
	public String getGroupId(){
		return groupid;
	}
	public String getArtifactId(){
		return artifactid;
	}
	public String getVersion(){
		return version;
	}
	public String getClassName(){
		return className;
	}

	public Artifact setVersion(String newVersion){
	    return new Artifact(groupid, artifactid, newVersion, className);
    }

    @Override
    public String toString() {
        return groupid+":"+artifactid+":"+version+":"+className;
    }

    @Override
    public int hashCode() {
        return _hashcode;
    }

    @Override
    public boolean equals(Object obj) {
	    try{
	        Artifact o=(Artifact)obj;
            if(o._hashcode!=this._hashcode) return false;
            if(!o.groupid.equals(groupid)) return false;
            if(!o.artifactid.equals(artifactid)) return false;
            if(!o.version.equals(version)) return false;
            if(!o.className.equals(className)) return false;
            return true;
        } catch (Throwable t){
	        return false;
        }
    }
}
