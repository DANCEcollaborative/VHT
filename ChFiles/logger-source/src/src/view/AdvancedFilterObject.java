package src.view;

/**
 * This class is used as a convinenece for the AdvancedFilterPanel.  Instead of
 * trying to syncronize two arrays (String and boolean) for each filter or trying
 * to use a TreeMap; it is simpler to just create the AdvancedFilterObject and
 * have one array.  This object simply stores a String and a boolean and provides
 * Accessor methods for them.  
 * 
 * @author Robert Kanter
 */
public class AdvancedFilterObject 
{
    /**
     * The filter string
     */
    private String filter;
    
    /**
     * Is true if is positive filter, false if negative filter
     */
    private boolean isPositive;

    
    /**
     * The constructor simply assigns the passed in arguments to the class fields.  
     * 
     * @param f The filter string
     * @param p true if positive filter, false if negative filter
     */
    public AdvancedFilterObject(String f, boolean p)
    {
        filter = f;
        isPositive = p;
    }
    
    /**
     * Accessor for the filter string
     * 
     * @return filter
     */
    public String getFilter()
    {
        return filter;
    }
    
    /**
     * Accessor for if the filter is positive or negative
     * 
     * @return isPositive
     */
    public boolean isPositive()
    {
        return isPositive;
    }
}