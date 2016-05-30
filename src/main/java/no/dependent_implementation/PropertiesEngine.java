package no.dependent_implementation;

import no.dependent.OutputBouble;

import static java.util.regex.Pattern.compile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PropertiesEngine {
	private static final Pattern PATTERN = compile("\\$\\{(.+?)\\}");
	private Properties props=new Properties();
    {
        props.setProperty("win_app_data", getAppData());
    }

	public String replaceProperties(String line) {
		return replace(line);
	}
	
    private String replace(String source) {
        if (source == null)
            return null;
        Matcher m = PATTERN.matcher(source);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String var = m.group(1);
            String value = getProperty(var);
            String replacement = (value != null) ? replace(value) : "";
            m.appendReplacement(sb, fixBackslashForRegex(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }
    
    private static String fixBackslashForRegex(String text) {
        return text.replace("\\", "\\\\");
    }

	public void loadProperties(String propertiesFileName) {
		try {
			InputStream propertiesStream=new FileInputStream(propertiesFileName);
			props.load(propertiesStream);
			propertiesStream.close();
		} catch (IOException e) {
            OutputBouble.reportError("Error loading properties from file: " + propertiesFileName, e);
		}
	}

    private String getProperty(String key){
        if(props.containsKey(key)){
            return props.getProperty(key);
        } else{
            Properties sysp=System.getProperties();
            if(sysp.containsKey(key)){
                return sysp.getProperty(key);
            }
            return key;
        }
    }

    private static String getAppData() {
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            String drive = System.getenv("SystemDrive");

            File tmpFile = new File(drive + File.separator + "ProgramData");
            if (tmpFile.isDirectory()) {
                return tmpFile.getAbsolutePath();
            }
            tmpFile = new File(drive + File.separator + "Documents and Settings" + File.separator + "All Users" + File.separator + "Application Data");
            if (tmpFile.isDirectory()) {
                return tmpFile.getAbsolutePath();
            }
        }
        return "";
    }
}
