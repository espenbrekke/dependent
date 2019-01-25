package no.dependent.utils;

import java.util.Objects;

public class Artifact {

    public final String group;
    public final String id;
    public final String version;
    public final String filetype;

    public Artifact(String artifactId){
        String[] split=artifactId.split(":");
        if(split.length==3){
            group=split[0];
            id=split[1];
            filetype="jar";
            version=split[2];
        } else if(split.length==4){
            group=split[0];
            id=split[1];
            filetype=split[2];
            version=split[3];
        } else {
            throw new IllegalArgumentException("Unable to parse artifactid: "+artifactId);
        }
    }
    public Artifact(String group, String id, String version,String filetype){
        this.group=group;
        this.id=id;
        this.version=version;
        this.filetype=filetype;
    }

    public String asString(){
        return group+":"+id+":"+version;
    }

    public Artifact setVersion(String newVersion){
        return new Artifact(group,id,newVersion,filetype);
    }
    public Artifact setFileType(String newFileType){
        return new Artifact(group,id,version,newFileType);
    }

    @Override
    public String toString() {
        return group+":"+id+":"+version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Artifact artifact = (Artifact) o;
        return Objects.equals(group, artifact.group) &&
                Objects.equals(id, artifact.id) &&
                Objects.equals(version, artifact.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, id, version);
    }
}
