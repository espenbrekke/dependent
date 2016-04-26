package no.dependent;

import java.util.Arrays;
import java.util.Map;




public class DependentLoaderConfiguration {
    private final String[] propertyNames;
    private final String[] values;
    public final String name;

    public DependentLoaderConfiguration add(String property, String value){
        String[] newPropertyNames=Arrays.copyOf(propertyNames,propertyNames.length+1);
        String[] newValues=Arrays.copyOf(values,values.length+1);

        newPropertyNames[newPropertyNames.length-1]=property;
        newValues[newValues.length-1]=value;

        return new DependentLoaderConfiguration(name, newPropertyNames, newValues);
    }

    public DependentLoaderConfiguration(String configName){
        this(configName, new String[0], new String[0]);
    }
    private DependentLoaderConfiguration(String configName, String[] propertyNames, String[] values){
        this.propertyNames=propertyNames;
        this.values=values;
        this.name=configName;
    }

    public String[] get(String property){
        int length=0;
        for (int i = 0; i < propertyNames.length; i++) {
            if(property.equals(propertyNames[i])){
                length++;
            }
        }

        String[] retVal=new String[length];
        int writeTo=0;
        for (int i = 0; i < propertyNames.length; i++) {
            if(property.equals(propertyNames[i])){
                retVal[writeTo++]=values[i];
            }
        }
        return retVal;
    }
}
