package src.view;

import java.io.File;
import javax.swing.filechooser.*;
        
/**
 * This class is used to filter JFileChoosers by a file extension.  
 * It is not used by the user directly, its methods are called by the JFileChooser
 * automatically when you use it as a FileFilter.  
 * 
 * @author Robert Kanter
 */
public class ExtensionFilter extends FileFilter{
    
    /**
     * The file extension
     */
    private String extension;
    
    /**
     * A description of the extension
     */
    private String description;
                
    /**
     * The constructor creates the FileFilter based on the passed in extension.  
     * 
     * @param ex The file extension to allow
     * @param d A description of the file extension
     */
    public ExtensionFilter(String ex, String d)
    {
        extension = ex;
        description = d;
    }
                    
    /**
     * This method actually filters the files.
     * Only allows files with the extension as well as directories.  
     * 
     * @param f The File that is being checked
     * @return true if the file is acceptable, false if it is not
     */
    public boolean accept(File f) {
        
        //Accept directories too
        if (f.isDirectory()) 
        {
            return true;
        }

        String name = f.getName();
        if(name.endsWith("." + extension))
        {
            return true;
        }
        return false;
        
    }
    
    /**
     * Returns the description of the acceptable files.  
     * 
     * @return the description of the acceptable files
     */
    public String getDescription() {
        return description + " (*." + extension + ")";
    }
}