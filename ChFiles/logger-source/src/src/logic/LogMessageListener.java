package src.logic;

/**
 * Interface to listen to logger: logger will inforn its listeners when there is a new message, there is an error, or when it
 * starts.stops logging
 * @author Brenda Medina
 */
public interface LogMessageListener {
    /**
     * Method to inform listeners about the new message (like display it to the user)
     * @param messageWithTimeStamp the new message the logger received
     */
    public void newMessage(String messageWithTimeStamp);

    /**
     * Method to inform listeners about the error the logger encountered (like tell the user or log it)
     * @param message the message the logger was processing while the error occurred
     * @param e the exception that got thrown because of the error
     */
    public void errorWithMessage(String message, Exception e);

    /**
     * Method to inform listeners that the logger has started logging
     * @param logFile the full path of the main log file the logger is currently using to log
     */
    public void startedLogging(String logFile);

    /**
     * Method to inform listeners that the logger has stopped logging
     */
    public void stoppedLogging();
}