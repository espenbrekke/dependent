package no.dependent_implementation.feature;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/*
*
* */

public class FeatureRepositoryPolicy {
    public final Properties properties;
    public final String directory;
    public final String layout;

    public FeatureRepositoryPolicy(String repositoryDeclaration){
        properties=defaultProperties(repositoryDeclaration);

        File policyFile=new File(properties.getProperty("policy"));
        if(policyFile.exists() && policyFile.getName().endsWith(".properties")){
            InputStream in=null;
            try{
                in=new FileInputStream(policyFile);
                properties.load(in);
            } catch (IOException e){
                throw new IllegalArgumentException("Unable to load repositorypolicy: "+policyFile.getAbsolutePath());
            }
            if(in!=null) try{in.close();} catch (IOException e){}
        }

        directory=properties.getProperty("dir");
        layout=properties.getProperty("layout");
    }

    private static Properties defaultProperties(String repositoryDeclaration){
        File file=new File(repositoryDeclaration);
        Properties r=new Properties();
        if(file.exists() && file.isDirectory()){
            r.setProperty("dir",file.getAbsolutePath());
            r.setProperty("policy",new File(file,"repository.properties").getAbsolutePath());
        } else {
            r.setProperty("dir",file.getParentFile().getAbsolutePath());
            r.setProperty("policy",file.getAbsolutePath());
        }

        r.setProperty("layout","grouped");
        return r;
    }
}
