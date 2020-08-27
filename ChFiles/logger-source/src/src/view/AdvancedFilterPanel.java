package src.view;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * This class is used to create an Advanced Filter tab on the JTabbedPane in the
 * JLogger class.  It is a subclass of FilterPanel.  The only difference from
 * the FilterPanel class is that this class has its own constructor and it 
 * overrides the filterText() method; all other methods are inherited from 
 * FilterPanel.  The Advanced Filter lets you create a filter with multple
 * terms and a logical operator.  There is no limit on the number of terms that can be in the filter, but
 * if you have too many, it will take longer to filter.  
 * 
 * @author Robert Kanter
 * @author Brenda Medina
 */
public class AdvancedFilterPanel extends src.view.FilterPanel
{
    
    /**
     * Stores the filters
     */
    private AdvancedFilterObject[] filters;

    /**
     * Stores whether the user selected the 'and' logical operator or not (in which case 'or' will be used)
     */
    private boolean usesAnd;        
    
    /**
     * The constructor sets up the filters.  It calls super() which sets up the
     * GUI.  It then sets the filters class field to the passed in AdvancedFilterObject
     * array.  Then it iterates through the already received messages and filters
     * them.  
     * 
     * @param filts An array of AdvancedFilterObjects to use as the filters
     * @param logList The ArrayList of messages that have already been received - Changed by Apar Suri, logList being an Array rather than an arraylist.
     * @param autoScroll true if the textarea should auto-scroll, false if not
     * @param and true if the logical operator 'and' will be applied to the individual terms, false if 'or' will be used.
     * @exception If there is a problem, thow an exception
     */
    public AdvancedFilterPanel(AdvancedFilterObject[] filts, String[] logList, boolean autoScroll, boolean and, JLogger aLogger) throws Exception
    {
        super();
        logger = aLogger;
        filters = filts;
        usesAnd = and;
        this.textArea.setLineWrap(aLogger.getWordWrap());
                
        //Iterate through the already received messages
        //for(Iterator it = logList.iterator(); it.hasNext(); )
        //Changed to iterating and array rather than an arraylist
        for (int it = 0; it < logList.length; ++it)
        {
            //String text = (String)it.next();

            //Call appendText to append the text to the textarea
            appendText(logList[it], autoScroll);
        }
    }

    /**
     * Parametrized contructor to create the advanced filter but with not initial input
     * @param filts An array of AdvancedFilterObjects to use as the filters
     * @param and true if the logical operator 'and' will be applied to the individual terms, false if 'or' will be used.
     */
    public AdvancedFilterPanel(AdvancedFilterObject[] filts, boolean and, JLogger aLogger){
         super();
         logger = aLogger;
        filters = filts;
        usesAnd = and;
    }
        
    /**
     * This method applies the filters to the passed in text. It checks each filter
     * from the filters array by using the filterTextHelper() method.  
     * 
     * @param text The message to check
     * @return true if the text satisfies the filters, false if not
     * @exception If there is a problem, throw an exception
     */
    @Override
    protected boolean filterText(String text) throws Exception
    {
        boolean satisfyFilter;
        //Check if the text satisfies the filters
        for(int i = 0; i < filters.length; i++)
        {
            satisfyFilter = filterTextHelper(filters[i].getFilter(), filters[i].isPositive(), text);
            if((!satisfyFilter) && usesAnd)
            {
                return false;
            }else if(satisfyFilter && (!usesAnd)){
                return true;
            }
        }
        //if using 'and' and there was no filter that did not satisfy filter
        if(usesAnd){
            return true;
        }else{ //if using 'or' and no term was satisfied
            return false;
        }
    }
       
    /**
     * This method is a helper method for the filterText() method.  If it is a 
     * positive filter, then it will return true for a match and false for not a 
     * match; if it is a negative filter, then it will return false for a match 
     * and true for not a match.  It is not case sensitive.  
     * 
     * @param myFilter The filter String to check against
     * @param myIsPositiveFilter true if its a positive filter, false if a negative filter
     * @param text The message to check
     * @return true if the text satisfies the filter, false if not
     * @throws java.lang.Exception If there is a problem, throw an exception
     */
    private boolean filterTextHelper(String myFilter, boolean myIsPositiveFilter, String text) throws Exception
    {
        return myIsPositiveFilter == text.toLowerCase().contains(myFilter.toLowerCase());
    }    
}
