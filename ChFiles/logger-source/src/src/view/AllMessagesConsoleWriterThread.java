package src.view;

import java.util.Date;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import src.logic.ExclusiveLock;


/**
 * Class to aid in optimizing JLogger:
 * This class will prevent JLogger from updating the GUI's text area each time a new message arrives; instead, the GUI's
 * text area will only get updated every second.
 * This class stores all incoming messages in a string and only updates JLogger every second.
 * Problem: there is a race condition: Access to the mesasges field should be synchronized, otherwise some messages
 * might be lost
 * 
 * @author Brenda Medina
 */
public class AllMessagesConsoleWriterThread extends Thread{

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
     * Time to wait, in milliseconds, before updating the GUI's text area
     */
    private final long MILLISECONDS_TO_WAIT = 300;

    /**
     * JLogger whose text area we will update
     */
    private JLogger logger;

    /**
     * Variable to synchronize access to messages
     */
    private boolean access; //to prevent access to messages by multiple threads

    /**
     * Buffer between max and current memory usage; used as indication to start
     * deleting lines from text area in order to avoid buffer overflow.
     */
    private final long BYTES_AS_BUFFER = 1024 * 1024 * 5; // 5MB

    /**
     * Indicated whether the maximum number of lines to display in GUI has been reached
     */
    private boolean maxLinesReached = false;

    /**
     * Added by Apar Suri for setting format of current time stamp 
     */
    public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
    
    ExclusiveLock lock;


    /**
     * Parametrized constructor: accepts a non-null JLogger for which it will update its text area.
     * @param aLogger
     */
    public AllMessagesConsoleWriterThread(JLogger aLogger){
        super();
        messages = "";
        numberOfNewLineChars = 0;
        logger = aLogger;
        lock = new ExclusiveLock();
    }

    @Override
    public void run() {
        while(true) {
            //wait one second
            try{
                //logger.appendToTextArea(System.getProperty("line.separator") + getCurrentDateTime() + " - Sleeping..", 1);
                Thread.sleep(MILLISECONDS_TO_WAIT);
                //logger.appendToTextArea(System.getProperty("line.separator") + getCurrentDateTime() +  " - Awake", 1);

            } catch(Exception e) {
                System.out.println(e.toString());
            }


            if ( !messages.equals("") ) {
                // Check to see if we're running out of memory and if so, start
                // deleting lines from GUI. Does not affect the writing of files
                // to disk.
                if (!maxLinesReached) {
                    Runtime r = Runtime.getRuntime();
                    long totalMemory = r.totalMemory();
                    long maxMemory = r.maxMemory();
                    //long freeMemory = r.freeMemory();
                    //System.out.println("\nTotal memory: \t"+ totalMemory);
                    //System.out.println("Free memory: \t"+ freeMemory);
                    //System.out.println("Max memory: \t"+ maxMemory);
                    //System.out.println("Buffer memory: \t"+ BYTES_AS_BUFFER);
                    if (totalMemory + BYTES_AS_BUFFER > maxMemory)
                    {
                        logger.timeToStartDeletingThemLines();
                        System.out.println("Close to buffer overflow. Will delete lines from GUI. This has no effect on the lines that are written to file.");
                        System.gc(); // Suggest garbage collection; will not happen instantly, but usally in time to avoid buffer overflow
                        maxLinesReached = true;
                    }
                } else {
                    logger.timeToStartDeletingThemLines();
                    maxLinesReached = false;
                }
                String newMessages = "";
                int numOfNewLines = -1;
                try {
                    lock.lock();
                    {
                        newMessages = messages;
                        numOfNewLines = numberOfNewLineChars;

                        messages = "";
                        numberOfNewLineChars = 0;
                    }
                    lock.unlock();
                } catch (Exception e) {
                    
                }
                if (newMessages != "" && numOfNewLines != -1)
                    logger.appendToTextArea(newMessages, numOfNewLines);
            }
        }
    }

    /**
     * Add a new message to the messages field
     * @param newMsg the new message
     */
    public void addMessage(String newMsg){
        try {
            lock.lock();
            {
                messages += System.getProperty("line.separator") + newMsg;
                numberOfNewLineChars++;
            }
            lock.unlock();
        }
        catch (Exception e) {
            //Exception which trying to lock
        }
    }

    /*
     * Added by Apar Suri for getting current time stamp for debugging purposes
     */
    public static String getCurrentDateTime() {
         Calendar cal = Calendar.getInstance();
    SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
    return sdf.format(cal.getTime());

    }
}