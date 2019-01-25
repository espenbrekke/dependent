package no.dependent_implementation.feature;

import java.util.Arrays;

public class FeatureCoordinate {
    public String base=null;
    public String id=null;
    public String[] aditionalKeys=null;
    public String[] aditionalValues=null;
    public void set(String key,String value){
        if("base".equals(key)){
            base=value;
        } else if("id".equals(key)){
            id=key;
        } else {
            if(aditionalKeys == null){
                aditionalKeys=new String[0];
                aditionalValues=new String[0];
            }

            aditionalValues= Arrays.copyOf(aditionalValues, aditionalValues.length+1);
            aditionalValues[aditionalValues.length-1]=value;

            aditionalKeys= Arrays.copyOf(aditionalKeys, aditionalKeys.length+1);
            aditionalKeys[aditionalKeys.length-1]=value;
        }
    }
}
