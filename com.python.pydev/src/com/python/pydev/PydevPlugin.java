package com.python.pydev;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.python.pydev.core.REF;

import com.python.pydev.license.ClientEncryption;
import com.python.pydev.util.EnvGetter;
import com.python.pydev.util.PydevExtensionNotifier;

/**
 * The main plugin class to be used in the desktop.
 */
public class PydevPlugin extends AbstractUIPlugin {

	//The shared instance.
	private static PydevPlugin plugin;
	private PydevExtensionNotifier notifier;
	private boolean validated;
	
	/**
	 * The constructor.
	 */
	public PydevPlugin() {
		plugin = this;
	}

	/**
	 * This method is called upon plug-in activation
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		
		checkValid();
	}

    public boolean isValidated(){
        return validated;
    }
    
	public String checkValidStr() {
	    String result = loadLicense();
	    if( !validated ) {
	        if(notifier == null){
	            notifier = new PydevExtensionNotifier();
	            notifier.setValidated( false );
	            notifier.start();
	        }
	    }else{
	        notifier.setValidated(true);
	    }
        return result;
        
    }
	public boolean checkValid() {
        checkValidStr();
	    return validated;
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		plugin = null;
	}

	/**
	 * Returns the shared instance.
	 */
	public static PydevPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path.
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return AbstractUIPlugin.imageDescriptorFromPlugin("com.python.pydev", path);
	}
	
	
	public void saveLicense( String data ) {	
		Bundle bundle = Platform.getBundle("com.python.pydev");
		IPath path = Platform.getStateLocation( bundle );		
    	path = path.addTrailingSeparator();
    	path = path.append("license");
    	try {
			FileOutputStream file = new FileOutputStream(path.toFile());
			file.write( data.getBytes() );
			file.close();
		} catch (FileNotFoundException e) {		
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}   			
    }
	
	private String loadLicense() {
    	Bundle bundle = Platform.getBundle("com.python.pydev");
    	IPath path = Platform.getStateLocation( bundle );		
    	path = path.addTrailingSeparator();
    	path = path.append("license");
    	try {
            File f = path.toFile();
            if(!f.exists()){
                throw new FileNotFoundException("File not found.");
            }
            
            String encLicense = REF.getFileContents(f);
			if( isLicenseValid(encLicense) ) {
				validated = true;
			} else {
				validated = false;
			}
		} catch (FileNotFoundException e) {
			validated = false;
			String ret = "The license file: "+path.toOSString()+" was not found.";
            return ret;
            
		} catch (Exception e) {
		    validated = false;
            return e.getMessage();
		}
        return null;
	}

    private boolean isLicenseValid(String encLicense) {
        //already decrypted
        String license = ClientEncryption.getInstance().decrypt(encLicense);
        try {
            Properties properties = new Properties();
            properties.load(new ByteArrayInputStream(license.getBytes()));
            
            String eMail = properties.getProperty("e-mail");
            String name  = properties.getProperty("name");
            String time = properties.getProperty("time");
            if(!getPreferenceStore().getString(PydevExtensionInitializer.USER_EMAIL).equals(eMail)){
                throw new RuntimeException("The e-mail specified is different from the e-mail in the license.");
            }
            getPreferenceStore().setValue(PydevExtensionInitializer.USER_NAME, name);
            getPreferenceStore().setValue(PydevExtensionInitializer.LIC_TIME, time);
            Properties envVariables = EnvGetter.getEnvVariables();
            
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        
        //if it got here, everything is ok...
        return true;
    }
	
}
