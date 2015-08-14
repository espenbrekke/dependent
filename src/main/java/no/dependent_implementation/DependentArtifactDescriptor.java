package no.dependent_implementation;

public class DependentArtifactDescriptor {
	private final String groupid;
	private final String artifactid;
	private final String version;
	private final String className;
	
	
	public DependentArtifactDescriptor(String stringRepresentation){
		try{
			String[] tokenized=stringRepresentation.split(":", 4);
			groupid=tokenized[0];
			artifactid=tokenized[1];
			version=tokenized[2];
			if(tokenized.length>3){
				className=tokenized[3];
			} else {
				className="";
			}
		} catch(Exception e) {
			throw new IllegalArgumentException("Parse error: "+stringRepresentation);
		}
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

}
