package src.view;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.JTextArea;


/**
 * This class is used to create a tab on the JLogger tabbedPane.  Each FilterPanel
 * tab will automatically filter the messages based on its filter string.  It is 
 * not case sensitive and will search the entire message.  You can create 
 * "Positive" (contains) or "Negative" (does not contain) filters..
 * 
 * @author Robert Kanter
 * @author Brenda Medina
 */
public class FilterPanel extends javax.swing.JPanel {

    /**
     * The JLogger
     */
    protected JLogger logger;
    
    /**
     * The String to use to filter the messages
     */
    private String filter;
    
    /**
     * Is true if its a positive filter, false if its a negative filter
     */
    private boolean isPositiveFilter;

    /**
     * Thread to add messages to the tab's text area only every second
     */
    private FilteredMessagesConsoleWriterThread addMessageThread;
        
    
    /**
     * The constructor sets up the filter.  It sets the class fields to the
     * passed in arguments.  It also calls initComponents to setup the GUI.  
     * Lastly, it will iterate through the passed in logList and call appendText()
     * on each String in the list so that the filter will be applied to messages 
     * that have already been received.  
     * 
     * @param isPositive true if it should be a positive filter, false if negative filter
     * @param myFilter The String to use to filter the messages
     * @param logList The ArrayList of messages that have already been received - Changed by Apar Suri, logList being an Array rather than an arraylist.
     * @param autoScroll true if the textarea should auto-scroll, false if not
     * @exception If there is a problem, thow an exception
     */
    public FilterPanel(boolean isPositive, String myFilter, String[] logList, boolean autoScroll, JLogger aLogger) throws Exception
    {
        isPositiveFilter = isPositive;
        filter = myFilter.toLowerCase();
        logger = aLogger;
        
        initializeVariables();
        this.textArea.setLineWrap(aLogger.getWordWrap());
        //this.textArea.setLineWrap(logger.get);
        
        //Iterate through the already received messages
        //for(Iterator it = logList.iterator(); it.hasNext(); )
        //Changed to iterating and array rather than an arraylist
        for (int it = 0; it < logList.length; ++it)
        {
            //String text = (String)it.next();
            
            //Call appendText to append the text to the textarea
            appendText(logList[it], autoScroll);
            //Hack for already present messages so that the newline does not create issues
            if (filterText(logList[it])) {
                //this means check till you find a new message
                for (int tempIndex = it + 1; tempIndex < logList.length; ++tempIndex) 
                {
                     if (!logger.StringStartsWithNumber(logList[tempIndex])) {
                          addMessageThread.newMessage(logList[tempIndex], autoScroll);
                     }
                     else {
                         it = tempIndex;
                         break;
                     }
                }
            }
        }
    }
    
    /**
     * Default Constructor for subclasses to initialize the components
     */
    protected FilterPanel()
    {
        initializeVariables();
    }

    /**
     * Parametrized constructor to create a filter tab without initial text
     * @param isPositive true if it should be a positive filter, false if negative filter
     * @param myFilter The String to use to filter the messages
     */
    public FilterPanel(boolean isPositive, String myFilter, JLogger aLogger){
         initializeVariables();
         isPositiveFilter = isPositive;
         filter = myFilter.toLowerCase();
         logger = aLogger;
    }

    /**
     * Method to initialize the tab's components and the thread that will update tab's text area
     */
    private void initializeVariables(){
         initComponents();
         addMessageThread = new FilteredMessagesConsoleWriterThread(this);
         addMessageThread.start();
    }

    
    /**
     * This method is used for appending text to the textarea in this filter.  It
     * adds a new line when approriate and uses the filterText() method to determine
     * if it should append the passed in text.  If autoScroll is true then it will scroll
     * the textArea.  
     * 
     * @param text The message to potentially append to the textarea
     * @param autoScroll true if the textarea should auto-scroll, false if not
     * @exception If there is a problem, thow an exception
     */
    public void appendText(String text, boolean autoScroll) throws Exception
    {       
        //Ask filterText() if we should append the passed in text
        if(filterText(text))
        {
            addMessageThread.newMessage(text, autoScroll);
        }
    }

    /**
     * Method to appen text to the text area: does not check if text passes filter.
     * This method ahould only be invoked by the addMessageToFilterTabThread class
     * @param someText
     */
    public void appendToTextArea(String someText, boolean autoScroll, int numberOfNewLineChars){
        //assumes thread always adds a new line char at the start of every message
        if(textArea.getText().compareTo("") == 0){
            someText = someText.substring(1);
            numberOfNewLineChars--;
        }
        textArea.append(someText);

        //Should we scroll the text area
        if (autoScroll) {
            textArea.setCaretPosition(textArea.getDocument().getLength() - someText.length() + numberOfNewLineChars);
        }
    }
    
    /**
     * This method applies the filter to the passed in text.  If it is a positive 
     * filter, then it will return true for a match and false for not a match; 
     * if it is a negative filter, then it will return false for a match and
     * true for not a match.  It is not case sensitive.   
     * 
     * @param text The message to check
     * @return true if the text satisfies the filter, false if not
     * @exception If there is a problem, throw an exception
     */
    protected boolean filterText(String text) throws Exception
    {
        return text.toLowerCase().contains(filter.toLowerCase()) == isPositiveFilter;
    }
    
    
    /**
     * Accessor for the messages in the textarea.  This is used by JLogger when it 
     * wants to save the filtered messages from this FilterPanel.  
     * 
     * @return the text from the textarea
     */
    public String getText()
    {
        return textArea.getText();
    }

    /**
     * Method to be used by search
     * @return the text area for this filter tab
     */
    public JTextArea getTextArea(){
        return textArea;
    }
   
    
    /**
     * This method simply sets the text of the textArea to "" (empty string).  It is
     * used by the JLogger class when the user wants to clear the logs.  
     * 
     */
    public void clearText()
    {
        textArea.setText("");
    }
    
    
    /**
     * This method will set the Line Wrap option of the textArea to true or false.  
     * The WordWrap option is actually always set (so line wrapping doesn't cut
     * words in half), but it won't actually wrap them unless line wrap is set.  
     * 
     * @param wrap true if it should line wrap, false if not
     */
    public void setWrap(boolean wrap)
    {
        textArea.setLineWrap(wrap);
    }
        
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        scrollPane = new javax.swing.JScrollPane();
        textArea = new javax.swing.JTextArea();

        textArea.setColumns(20);
        textArea.setEditable(false);
        textArea.setFont(new java.awt.Font("Courier New", 0, 12));
        textArea.setRows(5);
        textArea.setWrapStyleWord(true);
        textArea.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                textAreaFocusGained(evt);
            }
        });
        textArea.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textAreaKeyReleased(evt);
            }
        });
        scrollPane.setViewportView(textArea);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(scrollPane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 319, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void textAreaKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textAreaKeyReleased
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            logger.searchForwardWithLastSearch();
        }
        else if(evt.isShiftDown() && evt.getKeyCode() == KeyEvent.VK_F3){
          logger.searchBackwardWithLastSearch();
        }else if (evt.getKeyCode() == KeyEvent.VK_F3) {
          logger.searchForwardWithLastSearch();
       } else if (evt.isControlDown() && evt.getKeyCode() == KeyEvent.VK_F) {
           logger.searchForwardWithLastSearch();
       }else if (evt.isControlDown() && evt.getKeyCode() == KeyEvent.VK_R) {
           logger.searchBackwardWithLastSearch();
       }
    }//GEN-LAST:event_textAreaKeyReleased

    private void textAreaFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_textAreaFocusGained
        // TODO add your handling code here:
        this.textArea.setLineWrap(this.logger.getWordWrap());
    }//GEN-LAST:event_textAreaFocusGained

    // Variables declaration - do not modify//GEN-BEGIN:variables
    protected javax.swing.JScrollPane scrollPane;
    protected javax.swing.JTextArea textArea;
    // End of variables declaration//GEN-END:variables

}
