package src.logic;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.*;

/**
 * Class to aid in optimizing JLogger:
 * This class will write to the log files every X seconds to prevent many I/O
 * operations, as opposed to writing to the log files every time a message arrives).
 *
 * @author Brenda Medina
 * @author Deepali Mendhekar
 */
public class FileWriterThread extends Thread {

    /**
     * Time to wait, in milliseconds, before updating the GUI's text area
     */
    private final long MILLISECONDS_TO_WAIT = 1000;
    /**
     * LogManager
     */
    private LogManager logger;
    /**
     * A map of message for each message type
     */
    private Map<String, LinkedBlockingDeque<String>> messageListMap;
    /**
     * Boolean to flag is there is a log message in queue to be written to file
     */
    private boolean haveNewMessageToWrite = false;
    
    private ExclusiveLock lock;

    /**
     * Parametrized constructor
     * @param aLogger the logger that holds the files and file writers
     */
    public FileWriterThread(LogManager aLogger) {
        super();
        logger = aLogger;
        messageListMap = new HashMap();
        lock = new ExclusiveLock();
    }

    @Override
    public void run() {
        PrintStream out = null;
        String type = null;
        String messageList = "";

        while (logger.inSession()) {
            //wait
            //wait 30 seconds
            try {
                Thread.sleep(MILLISECONDS_TO_WAIT);
            } catch (Exception e) {
                //do nothing just continue
            }

            if (haveNewMessageToWrite == true) {
                for (Iterator<String> itr = messageListMap.keySet().iterator(); itr.hasNext();) {
                    type = itr.next();
                    
                    try {
                        lock.lock();
                        {
                            messageList = messageListMap.get(type).remove();
                        }
                        lock.unlock();
                    }
                    catch (Exception e) {
                        
                    }

                    out = logger.getLogFile(type);
                    if (out != null) {
                        out.print(messageList);
                    }
                    messageList = "";
                }

                haveNewMessageToWrite = false;
            }
        }
        
        if (haveNewMessageToWrite == true) {
                for (Iterator<String> itr = messageListMap.keySet().iterator(); itr.hasNext();) {
                    type = itr.next();
                    
                    try {
                        lock.lock();
                        {
                            messageList = messageListMap.get(type).remove();
                        }
                        lock.unlock();
                    }
                    catch (Exception e) {
                        System.out.println("Issues in acquiring lock when writing to file - " + e.getMessage());
                    }

                    out = logger.getLogFile(type);
                    if (out != null) {
                        out.print(messageList);
                    }
                    messageList = "";
                }

                haveNewMessageToWrite = false;
            }
    }

    /**
     * Method to store messages to write to file
     * @param type
     * @param newMessage
     */
    public void newMessage(String type, String newMessage) {
        
        if (messageListMap.get(type) == null) {
            LinkedBlockingDeque<String> deque = new LinkedBlockingDeque<String>();
            messageListMap.put(type, deque);
        }
        
        String messageList = null;
        
        try {
            lock.lock();
            {
                try {
                    messageList = messageListMap.get(type).remove();
                }
                catch (Exception e) {

                }
                finally {
                    if (messageList== null) {
                        messageList = System.getProperty("line.separator") + newMessage;
                    }
                    else {
                        messageList += System.getProperty("line.separator") + newMessage;
                    }
                }
                messageListMap.get(type).add(messageList);
                haveNewMessageToWrite = true;
            }
            lock.unlock();
        }
        
        catch (Exception e) {
            System.out.println("Issues in acquiring lock when writing to file - " + e.getMessage());
        }
    }
}
