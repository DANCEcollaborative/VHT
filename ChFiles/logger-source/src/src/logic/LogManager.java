package src.logic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;

import edu.usc.ict.vhmsg.MessageEvent;
import edu.usc.ict.vhmsg.MessageListener;
import edu.usc.ict.vhmsg.VHMsg;

/**
 * This class performs all of the functionality a logger should have: like start logging, stop logging, import and save.
 * The only thing it does not do is display or filter.
 * @author Robert Kanter
 * @author Brenda Medina
 */
public class LogManager implements MessageListener {

    public static final String LOG_PATH = "log_path";
    public static final String LOG_PATH_ABSOLUTE = "log_path_absolute";
    public static final String LOG_PATH_RELATIVE = "log_path_relative";
    public static final String LOCAL_TESTS = "local_tests";

    private static final String LOG_PATH_STOP = "log_path_stop";
    private static final String START = "start";
    private static final String STOP = "stop";
    private static final String LOG = "log";
    private static final String ID = "id";
    private static final String SUBJECT_TEST = "subject_test";
    private static final String RELATIVE = "relative";
    private static final String ABSOLUTE = "absolute";
    /**
     * The default session name used
     */
    private final String DEFAULT_SESSION_NAME = "automated_logging-1";
    /**
     * Separator
     */
    private final String SEPARATOR = "-";
    /**
     * The path where the logging directories will be stored at
     */
    //assumes Jlogger is in ROOT/core/JLogger (2 directories down from where the test directory should be)
    public static File ROOT = new File("..", "..");
    /**
     * The logging session count
     */
    private int loggingSessionCount = 1;
    /**
     * The VHMsg to receive and send messages
     */
    private VHMsg vhmsg;
    /**
     * A map of Buffered Writers that it is using to log (there is one writer per current log file)
     */
    private Map<String, PrintStream> logFileMap;
    /**
     * A list of logger listeners the logger informs when there is a new message or when an error occurs
     */
    private ArrayList<LogMessageListener> listeners;
    /**
     * A boolean specifying whether the logger is currently logging or not
     */
    private boolean inSession;
    /**
     * The name of the current session, initially it is set to the default session name
     */
    private String sessionName;
    /**
     * The date (year, month, day, hour, minute and second) that logging begins
     */
    private String[] startDate;
    /**
     * The date (year, month, day, hour, minute and second) that logging begins; formatted for log file headers
     */
    private String startDateFormatted;
    /**
     * The type of test (subject_test, local_tests, etc)
     */
    private String testType;
    /**
     * The path of the directories created when the log session began; used to create new log file if a new type or agent is found in a message
     */
    private File testingDirectory;
    /**
     * The prefic every log file has: the date and the session name
     */
    private String filePrefix;
    /**
     * Current Path of log
     */
    private File logPath;
    /**
     * Thread to write to files only in set intervals
     */
    private FileWriterThread fileWriterThread;

    /**
     * Filter adc flag
     */
    private boolean filterAdc = true;

    /**
     * Filter wsp flag
     */
    private boolean filterWsp = true;
    
    /*
     * Added by Apar Suri
     * Filter saccade messages
     */
    private boolean filterSBMBMLSaccade = true;
    
    /*
     * Filter PML (vrPerception) messages
     */
    private boolean filterPMLMessages = true;

     /**
     * Filter ping / vrAllCall related messages
     */
    private boolean filterPing = true;
    
    /*
     * FLag which defines if the log_path is active (which means the log is being logged at some place other than default (which is local_test)
     */
    private boolean isLogPathActive = false;
    
    /*
     * path which saves the folder for log when log_path is used (isLogPathActive is true)
     */
    private String logPathDirectory;
    
    /*
     * if log_path is used, this is used to save which type it is, relative to absolute path
     * By default assuming it is true
     */
    private boolean isLogPathRelative;

    /**
     * Parameterized Constructor
     * @param listener a logger listener to subscribe to this logger
     */
    public LogManager(LogMessageListener listener) {
        startVariables();
        initVHMsgConnection(null, null);
        subscribeToLogger(listener);
        
        startLogSession(LOCAL_TESTS, null); //automatically start logging
    }
    
    public LogManager(LogMessageListener listener, String server, String scope) {
        startVariables();
        initVHMsgConnection(server, scope);
        subscribeToLogger(listener);
        
        startLogSession(LOCAL_TESTS, null); //automatically start logging
    }
    
    
    /*
     * getter for isLogPathRelative when using log_path
     */
    public boolean getIsLogPathRelative() {
        return isLogPathRelative;
    }
    

    /**
     * Private method to initiate the class variables
     */
    private void startVariables() {
        logFileMap = new HashMap<String, PrintStream>();
        listeners = new ArrayList<LogMessageListener>();
        inSession = false;
        testType = LOCAL_TESTS;
        startDate = null;
        sessionName = DEFAULT_SESSION_NAME;
        loggingSessionCount = 1;
        testingDirectory = new File("");
        startDateFormatted = "";
        logPath = new File("");
        filePrefix = "";
        
        logPathDirectory = LOCAL_TESTS;
        isLogPathRelative = true;
        isLogPathActive = false;
    }

    /**
     * Private method to establish the VHMsg connection
     */
    private void initVHMsgConnection(String server, String scope) {
        // start the VHMsg connection
        vhmsg = null;
        if (server == null && scope == null)
        {
            vhmsg = new VHMsg();
            vhmsg.openConnection();
        }
        else if (server == null)
        {
            vhmsg = new VHMsg(scope);
            vhmsg.openConnection();
        }
        else if (scope == null)
        {
            vhmsg = new VHMsg();
            vhmsg.openConnection(server);
        }
        else
        {
            vhmsg = new VHMsg(server, scope);
            vhmsg.openConnection();
        }
        vhmsg.addMessageListener(this);

        //src.view.JLogger.setVHMSGServerLabel(vhmsg.getServer());
        //src.view.setVHMSGScopeLabel(vhmsg.getScope());
        
        // subscribe to all of the messages
        vhmsg.subscribeMessage("*");
        //Send a message that the program is running
        sendMessage("vrComponent logger jlogger");


    }

    public String getVHMSGServer() {
        return vhmsg.getServer();
    }

    public String getVHMSGScope() {
        return vhmsg.getScope();
    }

    /**
     * Method to start a logging session; sets up the files and directories. creates the default log files and creates a writer for each file
     * @param aMsg The text after removing the 'logging start' part of the message
     */
    public void startLogSession(String aMsg, String logPath) {
        aMsg = aMsg.trim();
        aMsg = aMsg.replaceAll(" +", " ");
        aMsg = aMsg.replaceAll("\"", "");

        if (!inSession) {
            if ((!aMsg.startsWith(LOCAL_TESTS)) && (!aMsg.startsWith(SUBJECT_TEST)) && (!aMsg.startsWith(ID)) && (!aMsg.startsWith(
                    LOG_PATH))) {
                return; //will ignore regression_test for now; subject_test was replaced by local_tests
            }
            
            if (aMsg.startsWith(LOG_PATH)) {
                logPathDirectory = logPath;
                if (isLogPathRelative) {
                    setupDirectoriesForLogging(RELATIVE, logPath);
                }
                else {
                    setupDirectoriesForLogging(ABSOLUTE, logPath);
                }
            }
            else {
                testType = LOCAL_TESTS;
                setupFilesAndDirectories(logPath);
            }
            
            fileWriterThread = new FileWriterThread(this);
            fileWriterThread.start();
            inSession = true;

            // if we're passing in a log id to name the log file
            // this would ruin that, so we skip it
            // othwer
            if (logPath == null && !isLogPathActive) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    
                }
                sendMessage("logging start local_tests");
            }
            
            //notify listener
            for (int index = 0; index < listeners.size(); index++) {
                listeners.get(index).startedLogging("");
            }
        }
    }
    
    /*
     *getter for logPathDirectory. This is used for log_path variable. logPath is not used in this case 
     */
    public String getLogPathDirectory() {
        return logPathDirectory;
    }

    /**
     * Method to subscribe as a logger listener; the logger will inform its listeners when there is a new message or when there is an error
     * @param aListener The logger listener that wants to subscribe to this logger
     */
    public void subscribeToLogger(LogMessageListener aListener) {
        listeners.add(aListener);
    }

    /**
     * This method will send a properly formatted message based on the
     * passed in String.  If the String is empty, it won't do anything.
     *
     * @param message The message to send
     */
    public void sendMessage(String message) {
        if (message == null || (message.trim()).equals("")) {
            return;    //The string is empty so there is nothing to send
        }

        // send the message
        vhmsg.sendMessage(message);
    }

    /**
     * Method to receive messages, without a time stamp, via VHMsg
     * @param m
     */
    public void messageAction(MessageEvent m) {
        try {
			addMessage(m.toString());
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    /**
     * Method to add a message to the list of current messages; provides an
     * alternative to receiving messages only through VHMsg)
     *
     * @param aMSgWithoutTimeStamp a message without a time stamp
     * @throws UnsupportedEncodingException 
     */
    public void addMessage(String aMSgWithoutTimeStamp) throws UnsupportedEncodingException {

        // Process the message
        // TODO: see which processing (if any) is needed
        String timeStamp = getTimestamp();
        String msg = aMSgWithoutTimeStamp;
        
        msg = msg.replaceAll(" +", " ");    // AH: why would we need this? //AS: I think this is because sometimes if webbased messaging is used, it could have a + instead of " "
        msg = msg.trim();                   // AH: and this? //AS: This is good to do even if you know the message is in proper format so remove spaces from front and back.
        //Align the message if it has multiple lines
        msg = msg.replaceAll("\n", System.getProperty("line.separator") + "                      ");
        msg = URLDecoder.decode(msg,"UTF8");

        // Always write message to log file
        try {
            if (inSession) {
                fileWriterThread.newMessage("Message", timeStamp + " " + msg);
            }
        } catch (Exception e) {
            logLoggerError(aMSgWithoutTimeStamp, e);
        }

        // No further processing needed if filtered adc message
        if (filterAdc == true && aMSgWithoutTimeStamp.startsWith("adc ") == true)
        {
            return;
        }
        
        //If the message contains saccade, ignore (assuming that only messages which contain saccade is a sbm bml message with saccade tag
        if (filterSBMBMLSaccade == true && aMSgWithoutTimeStamp.contains("saccade") == true)
        {
            return;
        }
        
        //If message contains vrPercetion, ignore
        if (filterPMLMessages == true && aMSgWithoutTimeStamp.contains("vrPerception") == true)
        {
            return;
        }

        // No further processing needed if filtered wsp message
        if (filterWsp == true && aMSgWithoutTimeStamp.startsWith("wsp ") == true)
        {
            return;
        }

        // Partial processing if filtered vrAllCall or vrComponent message
        if (filterPing == true && ( aMSgWithoutTimeStamp.startsWith("vrAllCall") == true
                || aMSgWithoutTimeStamp.startsWith("vrComponent ") ) )
        {
            if (msg.equals("vrAllCall")) 
               sendMessage("vrComponent logger jlogger");
            return;
        }

        // All remaining messages need to be communicated to GUI
        for (int cursor = 0; cursor < listeners.size(); cursor++) {
            listeners.get(cursor).newMessage(timeStamp + " " + msg);
        }

        // Process remaining special messages
        if (msg.equals("vrAllCall"))
            sendMessage("vrComponent logger jlogger");
        else if (msg.startsWith("logging"))
            logging(msg.substring(7), timeStamp); //deletes 'logging' word
        else if (msg.startsWith("vrKillComponent ")){
            //Split the message by " " (spaces)
            String[] msg2 = msg.split(" ");

            //If the message has two words
            if (msg2.length == 2) {

                //If the first word is "vrKillComponent"
                if (msg2[0].equals("vrKillComponent")) {

                    //And the second word is either "logger" or "all"
                    //then we should respond with "vrProcEnd logger" and exit the program
                    if (msg2[1].equals("logger") || msg2[1].equals("all")) {
                        sendExitMessageAndExit();
                    }
                }
            }
        }
    }

    /**
     * Method to increment logging session number when start button is pressed
     */
    public void incrementLoggingSession() {
        loggingSessionCount++;
    }

    /**
     * Method to return session name 
     * @return the session name
     */
    public String getCurrentSessionName() {
        return sessionName;
    }

    public PrintStream getLogFile(String type) {
        return logFileMap.get(type);
    }

    /**
     * Method to return current time in format for a timezone
     * @param String format eg "yyyy-MM-dd-HH-mm-ss"
     * @param String timeZoneString e.g "UTC"
     * @return String formatted time in said time zone
     */
    private String getCurrentTimeInFormatTZ(String format, String timeZoneString) {
        Calendar calendar = Calendar.getInstance();

        TimeZone uTCTimeZone = TimeZone.getTimeZone(timeZoneString); 

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);

        simpleDateFormat.setTimeZone(uTCTimeZone);

        return simpleDateFormat.format(calendar.getTime());
    }
     
    /**
     * Method to return current time in format 
     * @param String format eg "yyyy-MM-dd-HH-mm-ss"     
     * @return String formatted local time
     */
    private String getCurrentTimeInFormat(String format) {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);

        return simpleDateFormat.format(calendar.getTime());
    }

    private String getTimeZone() {
        return Calendar.getInstance().getTimeZone().getDisplayName();
    }

    /**
    * Method to get daylight savings status at current location
    * @return Boolean true if On false if Off
    */
    private boolean getDaylightSavingsStatus() 
    {
        Calendar calObj = Calendar.getInstance();
        return calObj.getTimeZone().inDaylightTime(calObj.getTime());
    }

    /**
    * Method to get daylight savings status at current location as string
    * @return String "On" if On "Off" if Off
    */
    private String getDaylightSavingsStatusString()
    {
        return (getDaylightSavingsStatus()?"On":"Off");
    }

    private String getTimestamp() {
        String currentTime = getCurrentTimeInFormatTZ("kkmmssSSS", "UTC"); //hourMinSecMIllisecond (where hour is from 1-24)
        //Create the timestamp
        String timeStamp = currentTime + "";

        //Why 11?: time stamp can be at most 9 digits (2 for hour, 2 for min, 2 for sec, and 3 for millisec); I add 2 spaces
        while (timeStamp.length() < 11) {
            timeStamp += " ";
        }

        return timeStamp;
    }

    private void logLoggerError(String message, Exception e) {
        for (int index = 0; index < listeners.size(); index++) {
            listeners.get(index).errorWithMessage(getTimestamp() + " " + message, e);
        }

        System.out.println("Error while adding message.");
    }

    /**
     * Method to send the exiting message and end the application
     */
    public void sendExitMessageAndExit() {
        stopLogSession("", true);
        sendMessage("vrProcEnd logger jlogger");
        System.exit(0);
    }

    /**
     * Method to reset the session name to the default session name
     */
    public void resetSessionName() {
        sessionName = DEFAULT_SESSION_NAME;
    }

    /**
     * Method to get the default session name the logger is using
     * @return the default session name being used.
     */
    public String getDefaultSessionName() {
        return DEFAULT_SESSION_NAME;
    }

    /**
     * Method to get the path of the log file, if a log session is in place
     * @return the string of the path where the current log files are being stored to.
     */
    public File getLogPath() {
        if (inSession) {
            return logPath;
        }

        return null;
    }

    /**
     * Method to log a message (mainly parses the message)
     * @param msg a message to log (message no longer contains the 'logging log' part of the message)
     * @param timeStamp the time stamp associated with the message
     */
    private void logMessage(String msg, String timeStamp) {

        if (!inSession) {
            return; //not currently logging
        }

        // TODO: isn't this duplicating earlier processing?
        msg = msg.replaceAll(" +", " ");
        msg = msg.trim();
        int firstWhiteSpace = msg.indexOf(" "); //delimits the type and the agent

        if (firstWhiteSpace == -1) {
            return; //invalid format for logging log message
        }

        String type = msg.substring(0, firstWhiteSpace);
        msg = msg.substring(firstWhiteSpace);
        msg = msg.trim();
        firstWhiteSpace = msg.indexOf(" "); //delimits the agent and the text

        if (firstWhiteSpace == -1) {
            return; //invalid format for logging log message
        }

        String agent = msg.substring(0, firstWhiteSpace);
        String text = msg.substring(firstWhiteSpace);
        logMessageHelper(type, agent, text, timeStamp);
    }

    /**
     * A private method to aid in logging a message (mainly writes 
     * the message to the correct file)
     *
     * @param type The type of logging message (example error, aar, etc)
     * @param agent the name of the agent the message was associated with
     * @param theText the rest of the message after removing the time stamp, the 'loggin log' part, the type, and the agent (the text to log)
     * @param aTimeStamp the time stamp associated with the message
     */
    private void logMessageHelper(String type, String agent, String theText, String aTimeStamp) {
        try {
            type = type.trim();
            type = type.replaceAll(" ", "-");
            type = type.toLowerCase();

            agent = agent.trim();
            agent = agent.replaceAll(" ", "-");
            agent = agent.toLowerCase();

            theText = theText.trim();

            if (logFileMap.get(type + "," + agent) == null) {

                //if we get to this part in the code, a match wasn't found for the type and agent, so we create a new log file
                createNewLogFile(type, agent, theText);
            }

            fileWriterThread.newMessage(type + "," + agent, aTimeStamp + " " + theText);
        } catch (Exception e) {
            logLoggerError(theText, e);
        }
    }

    /**
     * Private method to create a new log file in the case a new type or agent is encoutered in a logging message
     * @param type the logging message type (example error, arr, etc)
     * @param agent the agent name the message refers to
     * @param theText The text to log (essentially anything after the time agent name)
     */
    private void createNewLogFile(String type, String agent, String theText) {

        File file = new File(testingDirectory, filePrefix + "-" + agent + "-" + type + "_log.txt");

        try {
            PrintStream bw = new PrintStream(new FileOutputStream(file));
            bw.println(type.toUpperCase() + " Log File for " + agent + " created on " + startDateFormatted);
            bw.println("Session Name: " + getCurrentSessionName());
            logFileMap.put(type + "," + agent, bw);
        } catch (IOException e) {
            logLoggerError(theText, e);
        }
    }

    /**
     * Updates session name based on name in text box (given as parameter).
     * Tries to determine if the name ends with a dash and a numeral and if so
     * increases the number by one. If not, '-1' is added.
     */
    public String updateSessionName(String currentSessionName) {
        int counter = 0;
        String sessionNumber = "";
        
        if (currentSessionName.compareTo("") != 0) {
            currentSessionName = currentSessionName.replaceAll(" +", " "); // Why?
            currentSessionName = currentSessionName.trim();
            currentSessionName = currentSessionName.replaceAll(" ", "-");
            if (currentSessionName.contains("-")) {
                sessionNumber = currentSessionName.substring(currentSessionName.lastIndexOf("-") + 1, currentSessionName.length());
            }

            try {
                counter = Integer.parseInt(sessionNumber.trim());
                currentSessionName = currentSessionName.substring(0, currentSessionName.lastIndexOf("-") + 1) + ++counter;
            } catch (NumberFormatException nfe) {
                currentSessionName += "-1";
            }

            sessionName = currentSessionName;
        }
        return sessionName;
    }

    /**
     * Method to set the session name
     * @param newSessionName the new session name
     */
    public void setSessionName(String newSessionName) {
       sessionName = newSessionName;
    }

    /**
     * Method to stop the logging session: more or less just a method to close the file writers and to set the inSession field to false;
     * @param message Any text after the stop command, thatis, after removing the 'logging stop' portion of the message
     */
    public void stopLogSession(String message, boolean shouldSendMessage) {
        message = message.replaceAll("\"", "");

        if (inSession) {
            inSession = false;
            if (shouldSendMessage) {
                sendMessage("logging stop");
            }
            
            try {
                fileWriterThread.join(2001);
            }
            catch (Exception e) {
                
            }

            try {
                PrintStream temp;
                Collection<PrintStream> logFileList = logFileMap.values();

                for (Iterator<PrintStream> itr = logFileList.iterator(); itr.hasNext();) {
                    temp = itr.next();
                    temp.flush();
                    temp.close();
                }
            } catch (Exception e) {
                logLoggerError("Closing files for end of logging session failed (IO).", e);
            }

            logFileMap.clear();  //deletes all of the current writers from the list

            //notify listeners
            for (int index = 0; index < listeners.size(); index++) {
                listeners.get(index).stoppedLogging();
            }
        }
    }

    @SuppressWarnings({"HardCodedStringLiteral", "StringConcatenation"})
    private File makeLogDirectoryName(File inLogDirectoryNameBase) {
        String currentTime = getCurrentTimeInFormatTZ("yyyy-MM-dd-HH-mm-ss", "UTC");
        //time stamp for header of the log files
        startDateFormatted = getCurrentTimeInFormatTZ("dd MMM yyyy HH:mm:ss", "UTC") + ". All timestamps are UTC. System time zone was " + getTimeZone() + ". Daylight savings is " + getDaylightSavingsStatusString() + ".";
        String[] date = currentTime.split("-");
        startDate = date;

        //creating directories
        String yearMonth = date[0] + date[1];
        String yearMonthDay_HourMinSec = yearMonth + startDate[2] + "_" + startDate[3] + startDate[4] + startDate[5];
        String filesDir = yearMonthDay_HourMinSec + "-" + getCurrentSessionName();
        filePrefix = filesDir;

        return new File(new File(inLogDirectoryNameBase, yearMonth), filesDir);
    }

    /**
     * Private method to setup the directories and files to start a loggging session
     */
    private void setupDirectoriesForLogging(String type, String inLogDirectoryPathFromMessage) {

        File logDirectoryPath = new File("");
        if (type.equalsIgnoreCase(RELATIVE)) {
            logDirectoryPath = makeLogDirectoryName(new File(ROOT, inLogDirectoryPathFromMessage));
        }
        else if (type.equalsIgnoreCase(ABSOLUTE)) {
            logDirectoryPath = makeLogDirectoryName(new File(inLogDirectoryPathFromMessage));
        }

        try {
            logPath = logDirectoryPath.getCanonicalFile();
            makeMessagePrintStream(new File(logPath, filePrefix + "-" + ".txt"));
        } catch (Exception e) {
            logLoggerError("Setting up files and directories for logging failed.", e);
        }
    }

    /**
     * Private method to setup the directories and files to start a loggging session
     * The only time logFile is not null, is when we receive the ID message with the id. We use id as the file name.
     */
    private void setupFilesAndDirectories(String logFile) {
        testingDirectory = makeLogDirectoryName(new File(ROOT, testType));
        testingDirectory.mkdirs();

        try {
            // if the logFile is not null, use it as the logPath
            // otherwise, use the testingDirectory
            if (logFile != null) {
                logPath = new File(logFile).getCanonicalFile();
                makeMessagePrintStream(new File(logPath.getParent(), logPath.getName() + ".txt"));
            } else {
                logPath = testingDirectory.getCanonicalFile();
                makeMessagePrintStream(new File(testingDirectory, getCurrentSessionName() + "-" + "message_log.txt"));
            }

        } catch (Exception e) {
            logLoggerError("Setting up files and directories for logging failed.", e);
        }
    }

    private void makeMessagePrintStream(File inFile) {
        try {
            inFile.getParentFile().mkdirs();
            PrintStream stream = new PrintStream(new FileOutputStream(inFile));
            stream.println("Message Log File created on " + startDateFormatted);
            stream.print("Session Name: " + getCurrentSessionName());
            logFileMap.put("Message", stream);

        } catch (Exception e) {
            logLoggerError("Setting up files and directories for logging failed.", e);
        }
    }

    /**
     * Method to get a header if all of the messages will be saved through another method other than the oe provided by the logger
     * @return a header to be included as the first line and the session name on the second line of the file when saving all of the current messages
     */
    public String getHeaderForSavingAllMessagesFromWindow() {
        return "All Messages saved Non-Automatically on " +
                getCurrentTimeInFormatTZ("dd MMM yyyy HH:mm:ss", "UTC") +
                " (" + getTimeZone() + ")" + System.getProperty("line.separator") + "Session Name: " + getCurrentSessionName();
    }

    /**
     * Method to get a header if the messages satisfying a certain filter will be saved
     * @param filterString A string describing the filter
     * @return a header to be included as the first line of the file with the session name on the second line of the file when saving all of the messages that satisfy this filter
     */
    public String getHeaderForFilterSaving(String filterString) {
        filterString = filterString.replaceAll(" +", " ");
        filterString = filterString.trim();

        return "All Messages from Filter [" +
                filterString +
                "] saved Non-Automatically on " +
                getCurrentTimeInFormatTZ("dd MMM yyyy HH:mm:ss", "UTC") +
                " (" + getTimeZone() + ")" + System.getProperty("line.separator") + "Session Name: " + getCurrentSessionName();
    }

    /**
     * Private method used when a message is recognized to start with 'logging log'
     * @param aMsg the message received without the time stamp or the 'logging log' part
     * @param timeStamp the time stamp associated with this message
     * @throws UnsupportedEncodingException 
     */
    private void logging(String aMsg, String timeStamp) throws UnsupportedEncodingException {
        aMsg = aMsg.trim();
        String temp = aMsg.replaceAll("\"", "");
        temp = URLDecoder.decode(temp,"UTF8");

        if (temp.startsWith(START)) {
            startLogSession(temp.substring(START.length()), null); //remove 'start' word
        } else if (temp.startsWith(STOP)) {
            stopLogSession(temp.substring(STOP.length()), true); //remove 'stop' word
        }
        else if (temp.startsWith(LOG_PATH_STOP)) {
            stopLogSession(temp.substring(LOG_PATH_STOP.length()), true);
            isLogPathActive = false;
        }
        else if (temp.startsWith(LOG_PATH)) {
            if (inSession) {
                //the shouldSendMessage is false because if it sends the message then may be the log_path where we want to start logging might also stop
                //depnding on when the message is receieved which may not be right way to do things.
                stopLogSession("", false);
            }
            isLogPathActive = true;
            if (temp.startsWith(LOG_PATH_ABSOLUTE)) {
                isLogPathRelative = false;
                startLogSession(aMsg, temp.substring(LOG_PATH_ABSOLUTE.length()+1));
            }
            else if (temp.startsWith(LOG_PATH_RELATIVE)){
                isLogPathRelative = true;
                startLogSession(aMsg, temp.substring(LOG_PATH_RELATIVE.length()+1));
            }
            else {
                //assuming it is log_path which is assumed to be relative
                isLogPathRelative = true;
                startLogSession(aMsg, temp.substring(LOG_PATH.length()+1));
            }
        }
        else if (temp.startsWith(LOG) && inSession) {
            logMessage(temp.substring(LOG.length()), timeStamp); //remove 'log' word
        } else if (temp.startsWith(ID)) {

            // the token after `id' should be the actual ID
            // so pass that as the logFile parameter to startLogSession
            startLogSession(temp, temp.substring(ID.length()+1));
        }
        
        //will ignore any other
    }
    
    /*
     * Getter for IsLogPathActive
     */
    public boolean getIsLogPathActive() {
        return isLogPathActive;
    }
            
    

    /**
     * Method to import messages (clears the list of messages before importing the new messages)
     * @param newMsgs The list of messages to import
     */
    public void importMessages(ArrayList<String> newMsgs) {
        String temp;

        for (int index = 0; index < newMsgs.size(); index++) {
            temp = newMsgs.get(index);
            temp = temp.replaceAll(" +", " ");
            temp = temp.trim();
        }
    }

    /**
     * Field getter
     * @return the current session name
     */
    public String getSessionName() {
        return sessionName;
    }

    /**
     * Field getter
     * @return true if the logger is currently logging, false otheriwse
     */
    public boolean inSession() {
        return inSession;
    }

    public void setFilterAdc(boolean filterAdc) {
        this.filterAdc = filterAdc;
    }
    
    public void setFilterSBMBMLSaccade(boolean filterSaccade) {
        this.filterSBMBMLSaccade = filterSaccade;
    }
    
    public void setFilerPMLMessages(boolean filterPML) {
        this.filterPMLMessages = filterPML;
    }

    public void setFilterWsp(boolean filterWsp) {
        this.filterWsp = filterWsp;
    }

     public void setFilterPing(boolean filterPing) {
        this.filterPing = filterPing;
    }
}
