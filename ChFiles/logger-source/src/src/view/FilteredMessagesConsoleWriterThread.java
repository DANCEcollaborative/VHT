package src.view;

/**
 * Class to aid in optimizing JLogger:
 * This class will prevent JLogger from updating the filter-tabs' text area each time a new message arrives; instead, the
 * text area will only get updated every second.
 * This class stores all incoming messages in a string and only updates JLogger every second.
 * It assumes the text has a;ready been checked to see if it passes the filter.
 * Problem: there is a race condition: Access to the mesasges field should be synchronized, otherwise some messages
 * might be lost
 * 
 * @author Brenda Medina
 */
public class FilteredMessagesConsoleWriterThread extends Thread{

    /**
     * The filter tab it will be updating the text area for
     */
    private FilterPanel tab;

    /**
     * Variable to synchronize access to messages
     */
    private boolean access;

    /**
     * Variable to store all new messages
     */
    private String messages;

    /**
     * Field to keep track of how many new line characters the messages field has.
     * Used to correctly position caret on text area
     */
    private int numberOfNewLineChars;

    /**
     * variable to store if the autoscroll was set during the last message
     */
    private boolean autoScroll;

     /**
     * Time to wait, in milliseconds, before updating the tab's text area
     */
    private final int MILLISECONDS_TO_WAIT = 1000;

    /**
     * Parametrized constructor: accepts the filter tab object it will be updating.
     * @param aTab
     */
    public FilteredMessagesConsoleWriterThread(FilterPanel aTab){
        super();
        messages = "";
        numberOfNewLineChars = 0;
        tab = aTab;
        autoScroll = true;
    }

    @Override
    public void run(){
        while(true){
            //wait one second
            try{
                Thread.sleep(MILLISECONDS_TO_WAIT);
            }catch(Exception e){
                //do nothing just continue
            }

            //update GUI
            if(messages.compareTo("") != 0){
                tab.appendToTextArea(messages, autoScroll, numberOfNewLineChars);
                messages = "";
                numberOfNewLineChars = 0;
            }
        }       
    }

    /**
     * Method to add a message to the messages field
     * Assumes text has already been filtered, ie, it has already been verified that the text passes the filter.
     * @param newMessage the new message to add
     */
    public void newMessage(String newMessage, boolean currentAutoScroll){
        messages += System.getProperty("line.separator") + newMessage;
        autoScroll = currentAutoScroll;
        numberOfNewLineChars++;
    }
}