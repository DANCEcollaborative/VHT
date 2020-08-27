package src.view;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.locks.Lock;
import javax.swing.*;
import javax.swing.text.*;

import src.logic.LogManager;
import src.logic.LogMessageListener;
import java.awt.event.MouseWheelEvent;
import src.logic.ExclusiveLock;
import java.awt.event.*;
//import src.logic.AutoComplete;

/**
 * This is the main class for the JLogger tool, a logger program for VHMsg messages.  It allows you to create custom filters
 * to search for specific types of messages.  It also lets you save all of the messages and filtered
 * messages.  The filters are not case sensitive and search the entire message.  
 * You can create "Positive" (contains) or "Negative" (does not contain) filters.  
 * You can also create an advanced filter that lets you have more than one filter term.  
 * 
 * @author Robert Kanter
 * @author Brenda Medina
 */
public class JLogger extends javax.swing.JFrame implements LogMessageListener, MouseWheelListener {
    
    /**
     * The logger
     */
    private LogManager logger;
    /**
     * Keeps track of what the user search for last
     */
    private String lastSeatch;
    /**
     * Position of the search for text in the last search the user perfomed:
     * is equal to -1 if the text was not found or if no searches have been made
     */
    private int lastSearchPosition;

    /**
     * Vriable that stores whether the word wrap feature is set or not
     */
    private boolean wordWrap;

    /**
     * Thread to take care of updating the GUI's text area
     */
    private AllMessagesConsoleWriterThread addMessageThread;

    /**
     * Maximum number of lines, gets set when almost out of memory
     */
    private int maxNumberOfLines=-1;

    /**
     * Boolean to indicate whether to scroll or not when new lines are added
     */
    private boolean autoScroll;

    /*
     * Added by Apar Suri
     * Boolean to indicate that the mouse wheel is scrolling. If so, disable auto scrolling
     */
    private boolean isMouseScrolling;

    /*
     * Added by Apar Suri
     * The SearchDialog object so that the class has a personal copy
     */
    //not used anymore. Once added in build, will remove this.
    private SearchDialog searchDialog;

    /*
     * Added by Apar Suri
     * This is for autocomplete of the send message
     */
    //AutoComplete autoComplete;
    
    //Added by Apar Suri for autoscrolling
        //This part is added to make sure that only 1 thread writes on the textArea. (Exclusive write)
        //java.util.concurrent.locks.Lock lock = new java.util.concurrent.locks.() {
    ExclusiveLock exLock;
    

    /**
     * Parametrized constructor that allows starting up JLogger with an imported file (via command line)
     *
     * The constructor calls initComponents to setup the GUI.  Then it starts the
     * Connection and initializes logList.  It also adds a window listener so that
     * the program will send "vrProcEnd logger jlogger" when it quits. It initializes the logger which sends the
     * "vrComponent logger jlogger" message on startup and automatically starts logging.
     * @param fileToImport  File to import
     * @param iconified     true, if logger needs to start in conified state
     */
    public JLogger(String fileToImport, boolean iconified, String server, String scope){
		setSystemProperty("apple.laf.useScreenMenuBar", "true");
		setSystemProperty("com.apple.mrj.application.apple.menu.about.name", "JLogger");
                exLock = new ExclusiveLock();

        setIconImage( new ImageIcon("AppIcon.png").getImage());

        initializeVariables(server, scope);
        //autoComplete = new AutoComplete();
        //load the autocompletion file
        //autoComplete.loadCompleteAutoCompleteList();

        if (fileToImport != null)
        {
            //import file
            File toImport = new File(fileToImport);
            importFile(toImport);
        }

        if (iconified == true)
        {
            this.setExtendedState(ICONIFIED); // Minimize window on startup
        }

        //Add a window listener to this so that it will send "vrProcEnd logger jlogger" when it exits
        addWindowListener(new java.awt.event.WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent winEvt) {
                //autoComplete.saveCompleteAutoCompleteList();
                logger.sendExitMessageAndExit();
                System.exit(0);
            }
        });

        //Make the SessionNameTextField have the focus when the program starts up
        SearchjTextField.requestFocus();
    }

    /**
     * Method to initilize JLogger's variables
     */
    private void initializeVariables(String server, String scope){
        //Set the Windows Look and Feel
        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {/*Do nothing, will just use the Java Look and Feel */

        }
        SwingUtilities.updateComponentTreeUI(this);
        
        initComponents();
        //Added by Apar Suri
        //default value being false since mouse is not scrolling
        isMouseScrolling = false;
        //Added by Apar Suri
        //For custom mouse wheel movement so that auto scroll does not get messed up
        textArea.addMouseWheelListener(this);
        //Added by Apar Suri
        //For next and previous buttons for the search field
         SearchjTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent evt) {
                searchFieldKeyReleased(evt);
            }
        });

        //Added by Apar Suri
        //For next and previous when textArea is in focus
        textArea.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent evt) {
                searchFieldKeyReleased(evt);
            }
        });

        lastSeatch = "";
        lastSearchPosition = -1;
        wordWrap = true;
        autoScroll = true;
        this.setTitle("JLogger");
        addMessageThread = new AllMessagesConsoleWriterThread(this);
        addMessageThread.start();
        if (server == null && scope == null)
            logger = new LogManager(this);
        else logger = new LogManager(this, server, scope);
        this.jLabelVHMSGServer.setText(logger.getVHMSGServer());
        this.jLabelVHMSGScope.setText(logger.getVHMSGScope());
        SessionNameTextField.setText(logger.getCurrentSessionName());

        messageHistoryComboBox.setEditable(true);
        messageHistoryComboBox.getEditor().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                messageHistoryComboBox.setSelectedItem(messageHistoryComboBox.getEditor().getItem());
                sendMessage(e.getActionCommand());

                //// Make sure the item that was sent is also the one selected
                //messageHistoryComboBox.setSelectedItem(e.getActionCommand());
            }
        });
        
        JTextComponent editorComponent = (JTextComponent) messageHistoryComboBox.getEditor().getEditorComponent();
        editorComponent.addFocusListener(new FocusListener(){
            public void focusGained(java.awt.event.FocusEvent e ){
                DisableAccelerators();
            }

            public void focusLost( FocusEvent e ){
                EnableAccelerators();
            }
        });

		// remove Quit menu item on MacOS and add our quitMenuItem handler to the
		// Quit item provide by the OS.
        if (System.getProperty("os.name").toLowerCase().contains("mac os")) {

			fileMenu.remove(quitMenuItem);
			if (fileMenu.getItemCount() > 0) {
				Object lastItem = fileMenu.getMenuComponent(fileMenu.getItemCount()-1);
				if (lastItem instanceof JSeparator) {
					fileMenu.remove(fileMenu.getItemCount()-1);
				}
			}

			// remove file menu if it's empty
			if (fileMenu.getItemCount() == 0) {
				jMenuBar1.remove(fileMenu);
			}

			helpjMenu.remove(AboutjMenuItem);
			if (helpjMenu.getItemCount() == 0) {
				jMenuBar1.remove(helpjMenu);
			}

			registerMacApplication(null);
        }

//        messageHistoryComboBox.getEditor().getEditorComponent().addKeyListener(new KeyListener() {
//
//            public void keyTyped(KeyEvent e) {
//                //throw new UnsupportedOperationException("Not supported yet.");
//            }
//
//            public void keyPressed(KeyEvent e) {
//                //throw new UnsupportedOperationException("Not supported yet.");
//            }
//
//            public void keyReleased(KeyEvent e) {
//                //throw new UnsupportedOperationException("Not supported yet.");
//                //String text = messageHistoryComboBox.getEditor().getItem().toString();
//                //messageHistoryComboBox.removeAllItems();
//                for (int i = 1; i < messageHistoryComboBox.getItemCount(); ++i) {
//                    messageHistoryComboBox.removeItemAt(i);
//                }
//                //messageHistoryComboBox.getEditor().setItem(text.concat(e.getKeyText(e.getKeyCode())));
//                //String test = messageHistoryComboBox.get;
//                String text = messageHistoryComboBox.getEditor().getItem().toString();
//                ArrayList<String> autoCompleteList = autoComplete.getCurrentAutoCompleteList(text);
//
//
//                if (autoCompleteList!=null) {
//                    //messageHistoryComboBox.addItem(text);
//                    for (int i = 0; i < autoCompleteList.size(); ++i) {
//                        messageHistoryComboBox.addItem(autoCompleteList.get(i));
//                    }
//                    if (messageHistoryComboBox.getItemCount() > 0) {
//                        messageHistoryComboBox.showPopup();
//                        messageHistoryComboBox.getEditor().setItem(text);
//                    }
//                }
//            }
//        });

        sendButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String message = (String)messageHistoryComboBox.getSelectedItem();
                sendMessage(message);
            }
        });

        clearButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                clearLogs();
            }
        });

        clearHistoryButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                clearMessageHistory();
            }
        });
    }

            /**
     * Action Listener that allows the user to press F3 or enter for the next instance or shift+F3 for the previous one
     * @param evt
     */
    private void searchFieldKeyReleased(java.awt.event.KeyEvent evt) {
       if(evt.getKeyCode() == KeyEvent.VK_ENTER){
           //resetSearchSetting();
            String text = SearchjTextField.getText();
        if(text.trim().compareTo("") == 0){
            return;
        }
        searchForwarFor(text);
       }else if(evt.isShiftDown() && evt.getKeyCode() == KeyEvent.VK_F3){
        String text = SearchjTextField.getText();
        if(text.trim().compareTo("") == 0){
            return;
        }
        searchBackwardFor(text);
      }else if (evt.getKeyCode() == KeyEvent.VK_F3) {
         String text = SearchjTextField.getText();
        if(text.trim().compareTo("") == 0){
            return;
        }
        searchForwarFor(text);
      }
       else if (evt.isControlDown() && evt.getKeyCode() == KeyEvent.VK_F) {
         String text = SearchjTextField.getText();
        if(text.trim().compareTo("") == 0){
            return;
        }
        searchForwarFor(text);
      }
       else if(evt.isControlDown() && evt.getKeyCode() == KeyEvent.VK_R){
        String text = SearchjTextField.getText();
        if(text.trim().compareTo("") == 0){
            return;
        }
        searchBackwardFor(text);
      }

    }
    
    /*
     * Added by Apar Suri
     * accessor for the stop button when logging stop is called.
     */
    public javax.swing.JButton getStopButton() {
        return this.stopjButton;
    }
    
    /*
     *Added by Apar Suri.
     * To get the value of word wrap for different filters
     */
    public boolean getWordWrap() {
        //boolean temp = !this.WordWrapjMenuItem.isSelected();
        return wordWrap;
    }
    
    public void EnableAccelerators() {
        importMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        allSaveAsMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        currentSaveAsMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.SHIFT_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        quitMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        clearjMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.SHIFT_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        resetSessionNamejMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.event.InputEvent.SHIFT_MASK));
        loggingPathjMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        DefaultSessionNamejMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, java.awt.event.InputEvent.SHIFT_MASK));
        WordWrapjMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        CloseCurrentFilterjMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        SimpleFilterjMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T, java.awt.event.InputEvent.SHIFT_MASK));
        CreateAdvancedFilterjMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        KeywordjMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        AboutjMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.event.InputEvent.ALT_MASK));
        HowTojMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_H, java.awt.event.InputEvent.ALT_MASK));
        CloseTabjMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        SaveTabjMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.SHIFT_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    }
    
    public void DisableAccelerators() {
        importMenuItem.setAccelerator(null);
        allSaveAsMenuItem.setAccelerator(null);
        currentSaveAsMenuItem.setAccelerator(null);
        quitMenuItem.setAccelerator(null);
        clearjMenuItem.setAccelerator(null);
        resetSessionNamejMenuItem.setAccelerator(null);
        loggingPathjMenuItem.setAccelerator(null);
        DefaultSessionNamejMenuItem.setAccelerator(null);
        WordWrapjMenuItem.setAccelerator(null);
        CloseCurrentFilterjMenuItem.setAccelerator(null);
        SimpleFilterjMenuItem.setAccelerator(null);
        CreateAdvancedFilterjMenuItem.setAccelerator(null);
        KeywordjMenuItem.setAccelerator(null);
        AboutjMenuItem.setAccelerator(null);
        HowTojMenuItem.setAccelerator(null);
        CloseTabjMenuItem.setAccelerator(null);
        SaveTabjMenuItem.setAccelerator(null);
    }

    /*
     * Added by Apar Suri
     * This method is wired to the even mouse wheel moved for custom scrolling so that it does not interfere with autoscrolling
     */
    public void mouseWheelMoved(MouseWheelEvent e) {
        
        //Since you are inside here, mouse is definitely scrolling
        isMouseScrolling = true;
        autoScroll = false;
        JScrollBar vbar = scrollPane.getVerticalScrollBar();
        int notches = e.getWheelRotation();

        //check for direction of the wheel movement
        int rotation = 0;
        if (notches < 0) rotation = -1;
        else rotation = 1;


        if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
                    vbar.setValue(vbar.getValue() + rotation * vbar.getUnitIncrement(e.getUnitsToScroll()));
        } else {
                    vbar.setValue(vbar.getValue() + rotation * vbar.getBlockIncrement());
        }

        //a small bit of padding so that if you are off by a couple of pixels from bottom, it should still auto scroll,
        //padding aprroximately equal to 1% of the max size
        int padding = (int) 0.01 * vbar.getMaximum();
        //if you actually scroll back to the max location, auto scroll should start again and the is scrolling should stop at that point
        if (vbar.getMaximum() <= vbar.getValue() + padding)
        {
            autoScroll = true;
            isMouseScrolling = false;
        }
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        typeButtonGroup = new javax.swing.ButtonGroup();
        TabMenujPopupMenu = new javax.swing.JPopupMenu();
        CloseTabjMenuItem = new javax.swing.JMenuItem();
        SaveTabjMenuItem = new javax.swing.JMenuItem();
        tabbedPane = new javax.swing.JTabbedPane();
        logPanel = new javax.swing.JPanel();
        scrollPane = new javax.swing.JScrollPane();
        textArea = new javax.swing.JTextArea();
        textArea.setLineWrap(true);
        StatusjPanel = new javax.swing.JPanel();
        StartJButton = new javax.swing.JButton();
        SessionNameTextField = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        SearchjTextField = new javax.swing.JTextField();
        stopjButton = new javax.swing.JButton();
        buttonPrevious = new javax.swing.JButton();
        buttonNext = new javax.swing.JButton();
        jLabelSearchStatus = new javax.swing.JLabel();
        messageHistoryComboBox = new javax.swing.JComboBox();
        sendButton = new javax.swing.JButton();
        clearButton = new javax.swing.JButton();
        clearHistoryButton = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jLabelVHMSGScope = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jLabelVHMSGServer = new javax.swing.JLabel();
        jMenuBar1 = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        importMenuItem = new javax.swing.JMenuItem();
        replayLogMenuItem = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JSeparator();
        allSaveAsMenuItem = new javax.swing.JMenuItem();
        currentSaveAsMenuItem = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JSeparator();
        quitMenuItem = new javax.swing.JMenuItem();
        jMenu1 = new javax.swing.JMenu();
        clearjMenuItem = new javax.swing.JMenuItem();
        resetSessionNamejMenuItem = new javax.swing.JMenuItem();
        jMenu5 = new javax.swing.JMenu();
        loggingPathjMenuItem = new javax.swing.JMenuItem();
        DefaultSessionNamejMenuItem = new javax.swing.JMenuItem();
        WordWrapjMenuItem = new javax.swing.JMenuItem();
        jMenu6 = new javax.swing.JMenu();
        CloseCurrentFilterjMenuItem = new javax.swing.JMenuItem();
        SimpleFilterjMenuItem = new javax.swing.JMenuItem();
        CreateAdvancedFilterjMenuItem = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JSeparator();
        filterAdcCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        filterWspCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        filterPingCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        filterSaccadeCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        filtervrPerceptionCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        jMenu2 = new javax.swing.JMenu();
        KeywordjMenuItem = new javax.swing.JMenuItem();
        helpjMenu = new javax.swing.JMenu();
        AboutjMenuItem = new javax.swing.JMenuItem();
        HowTojMenuItem = new javax.swing.JMenuItem();

        CloseTabjMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        CloseTabjMenuItem.setText("Close Current Filter");
        CloseTabjMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CloseTabjMenuItemActionPerformed(evt);
            }
        });
        TabMenujPopupMenu.add(CloseTabjMenuItem);

        SaveTabjMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.SHIFT_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        SaveTabjMenuItem.setText("Save Current Filter");
        SaveTabjMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SaveTabjMenuItemActionPerformed(evt);
            }
        });
        TabMenujPopupMenu.add(SaveTabjMenuItem);

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);

        tabbedPane.setComponentPopupMenu(TabMenujPopupMenu);
        tabbedPane.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                tabbedPaneStateChanged(evt);
            }
        });

        textArea.setColumns(20);
        textArea.setEditable(false);
        textArea.setFont(new java.awt.Font("ËÎÌו", 0, 12));
        textArea.setRows(5);
        textArea.setWrapStyleWord(true);
        scrollPane.setViewportView(textArea);

        javax.swing.GroupLayout logPanelLayout = new javax.swing.GroupLayout(logPanel);
        logPanel.setLayout(logPanelLayout);
        logPanelLayout.setHorizontalGroup(
            logPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 1055, Short.MAX_VALUE)
        );
        logPanelLayout.setVerticalGroup(
            logPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 385, Short.MAX_VALUE)
        );

        tabbedPane.addTab("All Messages", logPanel);

        StartJButton.setText("Start");
        StartJButton.setEnabled(false);
        StartJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                StartActionPerformed(evt);
            }
        });

        SessionNameTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                SessionNameTextFieldFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                SessionNameTextFieldFocusLost(evt);
            }
        });
        SessionNameTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                SessionNameTextFieldKeyReleased(evt);
            }
        });

        jLabel5.setText("Search:");

        SearchjTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                SearchjTextFieldFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                SearchjTextFieldFocusLost(evt);
            }
        });
        SearchjTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                SearchjTextFieldKeyReleased(evt);
            }
        });

        stopjButton.setText("Stop");
        stopjButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopjButtonActionPerformed(evt);
            }
        });

        buttonPrevious.setText("Previous");
        buttonPrevious.setName("buttonPrevious"); // NOI18N
        buttonPrevious.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                previousButtonActionPerformed(evt);
            }
        });

        buttonNext.setText("Next");
        buttonNext.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nextButtonActionPerformed(evt);
            }
        });

        jLabelSearchStatus.setName("labelSearchStatus"); // NOI18N

        javax.swing.GroupLayout StatusjPanelLayout = new javax.swing.GroupLayout(StatusjPanel);
        StatusjPanel.setLayout(StatusjPanelLayout);
        StatusjPanelLayout.setHorizontalGroup(
            StatusjPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(StatusjPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(StartJButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(stopjButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(SessionNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 116, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(SearchjTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonNext)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonPrevious)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabelSearchStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 294, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(202, Short.MAX_VALUE))
        );
        StatusjPanelLayout.setVerticalGroup(
            StatusjPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(StatusjPanelLayout.createSequentialGroup()
                .addGroup(StatusjPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(StatusjPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(StartJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(stopjButton)
                        .addComponent(SessionNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(StatusjPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel5)
                        .addComponent(SearchjTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(buttonNext)
                        .addComponent(buttonPrevious))
                    .addComponent(jLabelSearchStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(16, 16, 16))
        );

        sendButton.setText("Send");
        sendButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendButtonActionPerformed(evt);
            }
        });

        clearButton.setText("Clear Messages");

        clearHistoryButton.setText("Clear History");
        clearHistoryButton.setMaximumSize(new java.awt.Dimension(107, 23));
        clearHistoryButton.setMinimumSize(new java.awt.Dimension(107, 23));
        clearHistoryButton.setPreferredSize(new java.awt.Dimension(107, 23));

        jLabel2.setText("VHMGS Scope:");

        jLabelVHMSGScope.setText("N/A");

        jLabel1.setText("VHMGS Server:");

        jLabelVHMSGServer.setText("N/A");

        fileMenu.setText("File");

        importMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        importMenuItem.setText("Import Log...");
        importMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(importMenuItem);

        replayLogMenuItem.setText("Replay Log ...");
        replayLogMenuItem.setToolTipText("Tool for replaying a log one message at a time.");
        replayLogMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                replayLogMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(replayLogMenuItem);
        fileMenu.add(jSeparator3);

        allSaveAsMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        allSaveAsMenuItem.setText("[All Messages] Save As...");
        allSaveAsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                allSaveAsMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(allSaveAsMenuItem);

        currentSaveAsMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.SHIFT_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        currentSaveAsMenuItem.setText("[Current Filter] Save As...");
        currentSaveAsMenuItem.setEnabled(false);
        currentSaveAsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                currentSaveAsMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(currentSaveAsMenuItem);
        fileMenu.add(jSeparator2);

        quitMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        quitMenuItem.setText("Quit");
        quitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                quitMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(quitMenuItem);

        jMenuBar1.add(fileMenu);

        jMenu1.setText("Edit");

        clearjMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.SHIFT_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        clearjMenuItem.setText("Clear Log");
        clearjMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearjMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(clearjMenuItem);

        resetSessionNamejMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.event.InputEvent.SHIFT_MASK));
        resetSessionNamejMenuItem.setText("Reset Session Name");
        resetSessionNamejMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetSessionNamejMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(resetSessionNamejMenuItem);

        jMenuBar1.add(jMenu1);

        jMenu5.setText("Options");

        loggingPathjMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_P, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        loggingPathjMenuItem.setText("Get Logging Path");
        loggingPathjMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loggingPathjMenuItemActionPerformed(evt);
            }
        });
        jMenu5.add(loggingPathjMenuItem);

        DefaultSessionNamejMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, java.awt.event.InputEvent.SHIFT_MASK));
        DefaultSessionNamejMenuItem.setText("Get Default Session Name");
        DefaultSessionNamejMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DefaultSessionNamejMenuItemActionPerformed(evt);
            }
        });
        jMenu5.add(DefaultSessionNamejMenuItem);

        WordWrapjMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_L, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        WordWrapjMenuItem.setText("Disable Word Wrap");
        WordWrapjMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                WordWrapjMenuItemActionPerformed(evt);
            }
        });
        jMenu5.add(WordWrapjMenuItem);

        jMenuBar1.add(jMenu5);

        jMenu6.setText("Filters");

        CloseCurrentFilterjMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        CloseCurrentFilterjMenuItem.setText("Close Current Filter");
        CloseCurrentFilterjMenuItem.setEnabled(false);
        CloseCurrentFilterjMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CloseCurrentFilterjMenuItemActionPerformed(evt);
            }
        });
        jMenu6.add(CloseCurrentFilterjMenuItem);

        SimpleFilterjMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T, java.awt.event.InputEvent.SHIFT_MASK));
        SimpleFilterjMenuItem.setText("Create Simple Filter");
        SimpleFilterjMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SimpleFilterjMenuItemActionPerformed(evt);
            }
        });
        jMenu6.add(SimpleFilterjMenuItem);

        CreateAdvancedFilterjMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_T, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        CreateAdvancedFilterjMenuItem.setText("Create Advanced Filter");
        CreateAdvancedFilterjMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CreateAdvancedFilterjMenuItemActionPerformed(evt);
            }
        });
        jMenu6.add(CreateAdvancedFilterjMenuItem);
        jMenu6.add(jSeparator4);

        filterAdcCheckBoxMenuItem.setSelected(true);
        filterAdcCheckBoxMenuItem.setText("Filter adc");
        filterAdcCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterAdcCheckBoxMenuItemActionPerformed(evt);
            }
        });
        jMenu6.add(filterAdcCheckBoxMenuItem);

        filterWspCheckBoxMenuItem.setSelected(true);
        filterWspCheckBoxMenuItem.setText("Filter wsp");
        filterWspCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterWspCheckBoxMenuItemActionPerformed(evt);
            }
        });
        jMenu6.add(filterWspCheckBoxMenuItem);

        filterPingCheckBoxMenuItem.setSelected(true);
        filterPingCheckBoxMenuItem.setText("Filter ping msgs (vrAllCall)");
        filterPingCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterPingCheckBoxMenuItemActionPerformed(evt);
            }
        });
        jMenu6.add(filterPingCheckBoxMenuItem);

        filterSaccadeCheckBoxMenuItem.setSelected(true);
        filterSaccadeCheckBoxMenuItem.setText("Filter saccade msgs (SBM - BML)");
        filterSaccadeCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterSaccadeCheckBoxMenuItemActionPerformed(evt);
            }
        });
        jMenu6.add(filterSaccadeCheckBoxMenuItem);

        filtervrPerceptionCheckBoxMenuItem.setSelected(true);
        filtervrPerceptionCheckBoxMenuItem.setText("Filter PML msgs (vrPerception)");
        filtervrPerceptionCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filtervrPerceptionCheckBoxMenuItemActionPerformed(evt);
            }
        });
        jMenu6.add(filtervrPerceptionCheckBoxMenuItem);

        jMenuBar1.add(jMenu6);

        jMenu2.setText("Search");

        KeywordjMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        KeywordjMenuItem.setText("By Keyword");
        KeywordjMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                KeywordjMenuItemActionPerformed(evt);
            }
        });
        jMenu2.add(KeywordjMenuItem);

        jMenuBar1.add(jMenu2);

        helpjMenu.setText("Help");

        AboutjMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.event.InputEvent.ALT_MASK));
        AboutjMenuItem.setText("About JLogger");
        AboutjMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AboutjMenuItemActionPerformed(evt);
            }
        });
        helpjMenu.add(AboutjMenuItem);

        HowTojMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_H, java.awt.event.InputEvent.ALT_MASK));
        HowTojMenuItem.setText("How to ...");
        HowTojMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                HowTojMenuItemActionPerformed(evt);
            }
        });
        helpjMenu.add(HowTojMenuItem);

        jMenuBar1.add(helpjMenu);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(StatusjPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(tabbedPane)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(messageHistoryComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 301, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sendButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(clearHistoryButton, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(clearButton, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabelVHMSGScope)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabelVHMSGServer)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(StatusjPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(5, 5, 5)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(messageHistoryComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(clearButton)
                    .addComponent(clearHistoryButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sendButton)
                    .addComponent(jLabel2)
                    .addComponent(jLabelVHMSGScope)
                    .addComponent(jLabel1)
                    .addComponent(jLabelVHMSGServer))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tabbedPane)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents


    /**
     * This function will set the vhmsg host field
     *
     * @param host The VHMSG host
     */
    public void setVHMSGServerLabel(String host) {
        jLabelVHMSGServer.setText(host);
    }

    /**
     * This function will set the vhmsg scope field
     *
     * @param scope The VHMSG scope
     */
    public void setVHMSGScopeLabel(String scope) {
        jLabelVHMSGScope.setText(scope);
    }

/**
 * Method to create a simple filter:
 * This method gets called from the SimpleFilterCreatorDialog class.
 * @param filterText the filter text/term
 * @param isPositive tru if the filter is positive, false otherwise.
 */
public void createSimpleFilter(String filterText, boolean isPositive){
    String filterTabLabel = "[-] " + filterText;
        if (isPositive) {
            filterTabLabel = "[+] " + filterText;
        }

        //create a new FilterPanel and add it to the tabbedPane
        try {
            //Added by Apar Suri
            String textToBeFiltered = textArea.getText();
            String[] messages = textToBeFiltered.split(System.getProperty("line.separator"));

            //Added by Apar Suri to make sure that the already present messages are also filtered
            FilterPanel filterP = new FilterPanel(isPositive, filterText, messages, autoScroll, this);
            //FilterPanel filterP = new FilterPanel(isPositive, filterText, this);
            tabbedPane.add(filterTabLabel, filterP);
        } catch (Exception err) {
            String errormsg = "An Unknown Parsing Error occured:\n\n" + err.toString();
            JOptionPane.showMessageDialog(this, errormsg, "Unknown Parsing Error", JOptionPane.ERROR_MESSAGE);
        }
}

    /**
     * ChangeListener for the tabbedPane.  When the user changes tabs, it enables/disables
     * the "Close Current Filter" button and "[Current Filter] Save As..." menu item
     * as appropriate.
     *
     * @param evt The ChangeEvent
     */
private void tabbedPaneStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_tabbedPaneStateChanged
    int selectedTab = tabbedPane.getSelectedIndex();//GEN-LAST:event_tabbedPaneStateChanged

        //"All Messages" is selected
        if (selectedTab == 0) {
            CloseCurrentFilterjMenuItem.setEnabled(false);
            currentSaveAsMenuItem.setEnabled(false);
        } else //a filter is selected
        {
            CloseCurrentFilterjMenuItem.setEnabled(true);
            currentSaveAsMenuItem.setEnabled(true);
        }
    }

    /**
     * ActionListener for the "[All Messages] Save As..." menu item.  It gets the text
     * from textArea and passes it to saveFileAs().
     *
     * @param evt The ActionEvent
     */
private void allSaveAsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_allSaveAsMenuItemActionPerformed
    String text = textArea.getText();//GEN-LAST:event_allSaveAsMenuItemActionPerformed
        saveFileAs(text, logger.getHeaderForSavingAllMessagesFromWindow());
    }

    /**
     * This method will display a JFileChooser asking the user where to save their
     * log file.  Then it takes the passed in text and saves it to the file adding a header and the session name to the file (the first two lines).  If there
     * was a problem saving, it will display an alert message informing the user.
     *
     * @param text The text to save to a file
     */
    private void saveFileAs(String text, String header) {
        JFileChooser fc = new JFileChooser();
        fc.setAcceptAllFileFilterUsed(false);
        fc.addChoosableFileFilter(new ExtensionFilter("txt", "Text File"));

        int result = fc.showDialog(this, "Save");
        if (result == JFileChooser.APPROVE_OPTION) //If the user wants to save
        {
            File outFile = fc.getSelectedFile();        //Get the selected file

            //If the file is missing the .txt extension, add it
            if (!outFile.getName().endsWith(".txt")) {
                outFile = new File(outFile.getPath() + ".txt");
            }
            String[] lines;
            //Try to write the text to the file using PrintStream
            try {
                PrintStream ps = new PrintStream(new FileOutputStream(outFile));
                ps.println(header);
                lines = text.split(System.getProperty("line.separator"));
                for (int index = 0; index < lines.length; index++) {
                    ps.println(lines[index]);
                }
                ps.close();

            //If there was an Exception, inform the user
            } catch (Exception e) {
                String msg = "Could not save file.\n\nPlease make sure that you have permission to write it.";
                JOptionPane.showMessageDialog(this, msg, "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * ActionListener for the "[Current Filter] Save As..." menu item.  It checks to make sure
     * that the current tab is not "All Messages" and that its not invalid.  Then it gets the
     * text from the current filter and passes it to saveFileAs() with a header containing the session name for saving filtered text.
     *
     * @param evt The ActionEvent
     */
private void currentSaveAsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_currentSaveAsMenuItemActionPerformed
    int selectedTab = tabbedPane.getSelectedIndex();//GEN-LAST:event_currentSaveAsMenuItemActionPerformed
        if (selectedTab == 0 || selectedTab == -1) {
            return;   //make sure its not "All Messages" and that its valid
        }
        //Get the text from the filter
        String text = ((FilterPanel) tabbedPane.getComponentAt(selectedTab)).getText();
        String label = tabbedPane.getTitleAt(selectedTab);
        try {
            saveFileAs(text, logger.getHeaderForFilterSaving(label));
        } catch (Exception ex) {
            String msg = "An Unknown Error occurred.\n" + ex.toString();
            JOptionPane.showMessageDialog(this, msg, "Filter Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * ActionListener for the "Quit" menu item.  It creates a window closing event queue
     * so that it will cause the program to send "vrProcEnd JLogger" when exiting.
     *
     * @param evt The ActionEvent
     */
private void quitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_quitMenuItemActionPerformed
    //These three lines will queue a windowClosing event//GEN-LAST:event_quitMenuItemActionPerformed
        Toolkit t = Toolkit.getDefaultToolkit();
        EventQueue eq = t.getSystemEventQueue();
        eq.postEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }

    /**
     * This method will send the message to Active MQ
     *
     */
    private void sendMessage(String message) {
        if (message == null || (message.trim()).equals("")) {
            return;    //The string is empty so there is nothing to send
        }
        
        logger.sendMessage(message);
        boolean found = false;

        for (int i = 0; i < messageHistoryComboBox.getItemCount(); i++)
        {
            if (((String) messageHistoryComboBox.getItemAt(i)).equals(message) == true)
            {
                found = true;
                break;
            }
        }

        if (found == false)
        {
            messageHistoryComboBox.addItem(message);
        }
    }

    /**
     * This method will clear all logs by removing all of the messages from the logger
     * and setting the text of every tab's text area to "" (empty string).
     *
     */
    private void clearLogs() {
        textArea.setText("");           //clear the text in the textArea

        //clear the text of the text areas of all of the filters
        for (int i = 1; i < tabbedPane.getTabCount(); i++) {
            FilterPanel fp = (FilterPanel) tabbedPane.getComponentAt(i);
            fp.clearText();
        }
    }

    /**
     * This method will clear the history of all the messages from the combo box.
     *
     */
    private void clearMessageHistory() {
        messageHistoryComboBox.removeAllItems();
    }
    
    /**
     * ActionLIstener to start a logging session
     * @param evt
     */
private void StartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StartActionPerformed
//    logger.incrementLoggingSession();
    if (!logger.getIsLogPathActive()) {
        logger.startLogSession(LogManager.LOCAL_TESTS, null);
    }
    else {
        if (logger.getIsLogPathRelative()) {
            logger.startLogSession(LogManager.LOG_PATH, logger.getLogPathDirectory());
        }
        else {
            logger.startLogSession(LogManager.LOG_PATH_ABSOLUTE, logger.getLogPathDirectory());
        }
    }
        
  //  SessionNameTextField.setText(logger.getCurrentSessionName());
    StartJButton.setEnabled(false);
    stopjButton.setEnabled(true);
    
}//GEN-LAST:event_StartActionPerformed

    /**
     * ActionLIstener to close the current filter tab, checks if the current tab is not the main tab and if it is a valid tab
     * @param evt
     */
private void CloseCurrentFilterjMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CloseCurrentFilterjMenuItemActionPerformed
    int selectedTab = tabbedPane.getSelectedIndex();
    if (selectedTab == 0 || selectedTab == -1) {
        return;   //make sure its not "All Messages" and that its valid
    }
    //Remove the tab
    tabbedPane.remove(selectedTab);
}//GEN-LAST:event_CloseCurrentFilterjMenuItemActionPerformed

    /**
     * ActionLIstener to display a dialog that lists common HOt to's; simply creates a HowTo object
     * @param evt
     */
private void HowTojMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_HowTojMenuItemActionPerformed
    new HowTo(this);
}//GEN-LAST:event_HowTojMenuItemActionPerformed

    /**
     * ActionListener to change the session name, first makes sure it is not the mepty string and then it changes the current session name
     * @param evt
     */
private void SessionNameTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_SessionNameTextFieldKeyReleased
    if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
        String newSessionName = SessionNameTextField.getText();
        if (newSessionName.trim().compareTo("") != 0) {
           // logger.setSessionName(SessionNameTextField.getText());
            textArea.requestFocus();
            String txt = "The session name has been changed; if the logger is currently logging, the changes will not take effect until you press stop and then start again.";
            JOptionPane.showConfirmDialog(this, txt, "Session Name Changed", JOptionPane.CLOSED_OPTION, JOptionPane.INFORMATION_MESSAGE);
        } else {
            SessionNameTextField.setText(logger.getSessionName());
            textArea.requestFocus();
            String txt = "The Session name cannot be empty.";
            JOptionPane.showConfirmDialog(this, txt, "Session Name Error", JOptionPane.CLOSED_OPTION, JOptionPane.INFORMATION_MESSAGE);
        }

    }
}//GEN-LAST:event_SessionNameTextFieldKeyReleased

    /**
     * Action LIstener to allow user to close a filter tab by using the popup menu when the tab is right clicked and the close option is selected
     * @param evt
     */
private void CloseTabjMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CloseTabjMenuItemActionPerformed
    CloseCurrentFilterjMenuItemActionPerformed(evt);
}//GEN-LAST:event_CloseTabjMenuItemActionPerformed

    /**
     * Action Listener to allow user to save the text on a filter tab by using the popup menu when the tab is right clicked and the save option is selected
     * @param evt
     */
private void SaveTabjMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SaveTabjMenuItemActionPerformed
    currentSaveAsMenuItemActionPerformed(evt);
}//GEN-LAST:event_SaveTabjMenuItemActionPerformed

    /**
     * Action Listener to perform a search when the user presses enter from the search text field
     * @param evt
     */
private void SearchjTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_SearchjTextFieldKeyReleased
// Commented vy Apar Suri.
// The enter  key was being processed twice
//    if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
//        String searhFor = SearchjTextField.getText().trim();
//        if (searhFor.compareTo("") != 0) { //checking if there is text to search for, otherwise ignore
//            lastSeatch = searhFor;
//            //lastSearchPosition = -1;
//            searchForwarFor(searhFor);
//        }
//    }
}//GEN-LAST:event_SearchjTextFieldKeyReleased

/**
 * Method that resets variables used for searching (reset when new search will begin)
 */
public void resetSearchSetting(){
    lastSearchPosition = -1;
    lastSeatch = "";
}

/**
 * Method to be used by FilterPanel to search
 */
public void searchForwardWithLastSearch(){
    searchForwarFor(lastSeatch);
}

/**
 * Method to be used by FilterPanel to search
 */
public void searchBackwardWithLastSearch(){
    searchBackwardFor(lastSeatch);
}

/**
 * Helper method to search
 * @param searhFor the non-empty string of text to search for.
 */
    public void searchForwarFor(String searhFor) {

        if (searchDialog!= null && searchDialog.isVisible()) this.searchDialog.setSearchText("Searching for " + searhFor, Color.BLACK);
        else {
            this.jLabelSearchStatus.setForeground(Color.BLACK);
            this.jLabelSearchStatus.setText("Searching for " + searhFor);
        }

        int currentTabIndex = tabbedPane.getSelectedIndex();
        if(currentTabIndex < 0){return;} //invalid tab index
        
        if (searhFor.trim().compareTo("") == 0) {
            return; //ignore
        }
//        if(autoScrollCheckBox.isSelected()){
//            autoScrollCheckBox.doClick();
//        }
        searhFor = searhFor.toLowerCase(); //searc ignoring case

        JTextArea currentTextArea;
        String currentText;
        if(currentTabIndex == 0){
            currentTextArea = textArea;
            currentText = textArea.getText();
        }else{
             FilterPanel currentTab = (FilterPanel) (tabbedPane.getComponentAt(currentTabIndex));
             currentTextArea = currentTab.getTextArea();
             currentText = currentTextArea.getText();
        }

        
        //determining start position
        if (lastSeatch.compareToIgnoreCase(searhFor) == 0) {

            //checking if doing a search on same tab (if last search position is valid)
            if(lastSearchPosition != -1){
                try{
                    if(currentText.substring(lastSearchPosition, lastSearchPosition + searhFor.length()).compareToIgnoreCase(searhFor) != 0){
                    //start search from beggining (might be different tab)
                    lastSearchPosition = -1;
                    lastSeatch = "";
                    }
                }catch(Exception e){
                    //maybe index out of range (not same tab)
                    lastSearchPosition = -1;
                    lastSeatch = "";
                } 
            }
            
           //set starting point for search
            if (lastSearchPosition == -1) {
                currentTextArea.setCaretPosition(0);
            } else {
                currentTextArea.setCaretPosition(lastSearchPosition + 1);
            }
        } else {
            currentTextArea.setCaretPosition(0);
        }

        lastSeatch = searhFor;
        int caretPosition = currentTextArea.getCaretPosition();
        String allText = currentText.toLowerCase();
        int indexOfSearchText = allText.indexOf(searhFor, caretPosition);

        if (indexOfSearchText != -1) {
            lastSearchPosition = indexOfSearchText;
            currentTextArea.requestFocus();
            currentTextArea.select(indexOfSearchText, indexOfSearchText + searhFor.length());
            if (searchDialog!= null) this.searchDialog.setSearchText("Searching for " + searhFor, Color.BLACK);
            jLabelSearchStatus.setText("");
        } else {
            lastSearchPosition = -1;
            //tell user text was not found
            currentTextArea.requestFocus(); //if you press enter to remove the dialog below, it will generate an enter key-release for the search text field => the dialog below will keep popping up over and over
            currentTextArea.select(-1, -1); //deselect all text
            String txt = "The text you searched for was not found.";
            //Added by Apar Suri
            if (searchDialog!= null &&searchDialog.isVisible()) searchDialog.setSearchText(txt, Color.red);
            else {
            this.jLabelSearchStatus.setForeground(Color.red);
            this.jLabelSearchStatus.setText(txt);
            }
            this.SearchjTextField.requestFocus();

            //JOptionPane.showConfirmDialog(this, txt, "Search Completed ...", JOptionPane.CLOSED_OPTION, JOptionPane.ERROR_MESSAGE);
        }
    }

    /*
     * Added by Apar Suri
     * This method is to send the text of the searchtextbox to the search dialog box
     */
     public String getSearchText() {
         return this.SearchjTextField.getText();
     }

      /*
     * Added by Apar Suri
     * This method is to set the text of the searchtextbox
     */
     public void setSearchText(String value) {
         this.SearchjTextField.setText(value);
     }


    /**
     * Method to search backwards
     * @param searhFor the string to search for
     */
    public void searchBackwardFor(String searhFor){
        if (searchDialog!= null && searchDialog.isVisible()) this.searchDialog.setSearchText("Searching for " + searhFor, Color.BLACK);
        else {
        this.jLabelSearchStatus.setForeground(Color.BLACK);

        this.jLabelSearchStatus.setText("Searching for " + searhFor);
        }

        if (searhFor.trim().compareTo("") == 0) {
            return; //ignore
        }
//        if(autoScrollCheckBox.isSelected()){
//            autoScrollCheckBox.doClick();
//        }
        int currentTabIndex = tabbedPane.getSelectedIndex();
        if(currentTabIndex < 0){return;} //invalid tab index

        searhFor = searhFor.toLowerCase(); //searc ignoring case
        
         JTextArea currentTextArea;
         String currentText;
        if(currentTabIndex == 0){
            currentTextArea = textArea;
            currentText = textArea.getText();
        }else{
             FilterPanel currentTab = (FilterPanel) (tabbedPane.getComponentAt(currentTabIndex));
             currentTextArea = currentTab.getTextArea();
             currentText = currentTextArea.getText();
        }
         
         //checking if doing a search on same tab (if last search position is valid)
         if(lastSearchPosition != -1){
             try{
                 if(currentText.substring(lastSearchPosition, lastSearchPosition + searhFor.length()).compareToIgnoreCase(searhFor) != 0){
                 //start search from beggining (might be different tab)
                 lastSearchPosition = -1;
                 lastSeatch = "";
                 }
             }catch(Exception e){
                 //maybe index out of range (not same tab)
                 lastSearchPosition = -1;
                 lastSeatch = "";
             }
         }

        int indexOfPreviousInstance = getIndexOfPreviousInstance(searhFor, lastSearchPosition - 1, currentText);
        if(indexOfPreviousInstance != -1){
            currentTextArea.requestFocus();
            currentTextArea.select(indexOfPreviousInstance, indexOfPreviousInstance + searhFor.length());
            if (searchDialog!= null) this.searchDialog.setSearchText("Searching for " + searhFor, Color.BLACK);
            this.jLabelSearchStatus.setText("");

        }else{
            currentTextArea.select(-1, -1); //deselect all text
            String txt = "The text you searched for was not found.";
            //Added by Apar Suri
            if (searchDialog!= null &&searchDialog.isVisible()) searchDialog.setSearchText(txt, Color.red);
            else {
                this.jLabelSearchStatus.setForeground(Color.red);
                this.jLabelSearchStatus.setText(txt);
            }
            this.SearchjTextField.requestFocus();

            //JOptionPane.showConfirmDialog(this, txt, "Search Completed ...", JOptionPane.CLOSED_OPTION, JOptionPane.ERROR_MESSAGE);
        }
    }

    
    /**
     * Helper method: helps to search backwards, returns the index of the previous instance of the text to search for, or -1 if no instances are found
     * @param searhFor the string to search for
     * @param endIndex the index of the place where you want to start searching backwards from.
     * @return the index (integer) of the previous instance, with respect to endIndex, of the given string
     */
    private int getIndexOfPreviousInstance(String searhFor, int endIndex, String allText){
        int currentIndex = 0, CursorIndex = 0, start = 0;
        boolean foundIt = false;
        while((endIndex - start) >= searhFor.length()){
            CursorIndex = allText.toLowerCase().indexOf(searhFor.toLowerCase(), start);
            if(CursorIndex == -1 || CursorIndex >= endIndex){
                if(foundIt){
                    lastSearchPosition = currentIndex;
                    lastSeatch = searhFor;
                    return currentIndex;
                }else{
                    lastSearchPosition = -1;
                    lastSeatch = searhFor;
                    return -1;
                }
            }else{
                foundIt = true;
                currentIndex = CursorIndex;
            }
            start = currentIndex + 1;
        }
        lastSeatch = searhFor;
        lastSearchPosition = -1;
        return -1;
    }

    /**
     * Action listener to implement forward (next) seatch and backward (previous) search;
     * if the user wants to search for the next instance of the search item, pressing F3 (for next) will do it
     * If the user wants to search for the previous instance of the search item, presswing Shift + F3 (for previous) will do it
     * @param evt
     */
/**
 * Action LIstener to create a simple filter
 * @param evt
 */
private void SimpleFilterjMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SimpleFilterjMenuItemActionPerformed
    new SimpleFilterCreatorDialog(this);
}//GEN-LAST:event_SimpleFilterjMenuItemActionPerformed

/**
 * Action LIstener to obtain the path of the current log (only enabled if the logger is currently logging)
 * @param evt
 */
private void loggingPathjMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loggingPathjMenuItemActionPerformed
   if(logger.inSession()){
	   File path = logger.getLogPath();
       String txt = "The Current Logging Directory: " + (path == null ? "Not Logging." : path.getPath());
       JOptionPane.showConfirmDialog(this, txt, "Logging Path", JOptionPane.CLOSED_OPTION, JOptionPane.INFORMATION_MESSAGE);
   }
}//GEN-LAST:event_loggingPathjMenuItemActionPerformed

/**
 * Action LIstener to enable and disable the word wrap feature.
 * @param evt
 */
private void WordWrapjMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_WordWrapjMenuItemActionPerformed
        wordWrap = !wordWrap;
        textArea.setLineWrap(wordWrap);

        //set GUI label
        if(wordWrap){
            WordWrapjMenuItem.setText("Disable Word Wrap"); //word wrap is on/set
        }else{
            WordWrapjMenuItem.setText("Enable Word Wrap"); //word wrap is not set/off
        }

        //Iterate through the rest of the tabs (the FilterPanel objects) and call
        //their setWrap() methods
        for (int i = 1; i < tabbedPane.getTabCount(); i++) {
            FilterPanel fp = (FilterPanel) tabbedPane.getComponentAt(i);
            fp.setWrap(wordWrap);
        }
}//GEN-LAST:event_WordWrapjMenuItemActionPerformed

/**
 * Action LIstener to display a search dialog
 * @param evt
 */
private void KeywordjMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_KeywordjMenuItemActionPerformed
    //Commented by Apar Suri to remove the search dialog box since new next and rpevious buttons are added in the UI
    //textArea.requestFocus();
    //if (searchDialog == null) searchDialog = new SearchDialog(this);

}//GEN-LAST:event_KeywordjMenuItemActionPerformed

  /**
     * ActionLIstener to stop the logging session, if the logger is currently logging
     * @param evt
     */
private void stopjButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopjButtonActionPerformed
    logger.stopLogSession("", true);
    SessionNameTextField.setText(logger.updateSessionName(SessionNameTextField.getText()));
    StartJButton.setEnabled(true);
    stopjButton.setEnabled(false);
}//GEN-LAST:event_stopjButtonActionPerformed

/**
     * ActionLIstener to create an advanced filter; simply creates a AdvancedFilterCreatorDialog object
     * @param evt
     */
private void CreateAdvancedFilterjMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CreateAdvancedFilterjMenuItemActionPerformed
    new AdvancedFilterCreatorDialog(this);
}//GEN-LAST:event_CreateAdvancedFilterjMenuItemActionPerformed

/**
     * ActionLIstener to inform the user about JLogger; simply creates an AboutJLogger object
     * @param evt
     */
private void AboutjMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AboutjMenuItemActionPerformed
    new AboutJLogger(this);
}//GEN-LAST:event_AboutjMenuItemActionPerformed

/**
     * ActionListener for clearing the logs from the logger and clearing the displayed messages from the main tabs
     * as well as the filter tabs
     *
     * @param evt The ActionEvent
     */
private void clearjMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearjMenuItemActionPerformed
    String text = "Are you sure that you want to clear the log?\n\nAll messages from all filters will be cleared.";
    int answer = JOptionPane.showConfirmDialog(this, text, "Clear Log?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
    if (answer == JOptionPane.YES_OPTION) {
        clearLogs();
    }
}//GEN-LAST:event_clearjMenuItemActionPerformed

/**
 * Action Listener to allow users to reset the session name to the default session name
 * @param evt
 */
private void resetSessionNamejMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetSessionNamejMenuItemActionPerformed
    logger.resetSessionName();
    SessionNameTextField.setText(logger.getSessionName());
    textArea.requestFocus();
    String txt = "The session name has been reset; if the logger is currently logging, the changes will not take effect until you press stop and then start again.";
    JOptionPane.showConfirmDialog(this, txt, "Session Name Changed", JOptionPane.CLOSED_OPTION, JOptionPane.INFORMATION_MESSAGE);
}//GEN-LAST:event_resetSessionNamejMenuItemActionPerformed

/**
 * Action Listener to allow the user to retrieve the default session name (this session name will be used if the reset session name option is selected)
 * @param evt
 */
private void DefaultSessionNamejMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DefaultSessionNamejMenuItemActionPerformed
    String txt = "The Default Session Name: " + logger.getDefaultSessionName();
    JOptionPane.showConfirmDialog(this, txt, "Default Session Name", JOptionPane.CLOSED_OPTION, JOptionPane.INFORMATION_MESSAGE);
}//GEN-LAST:event_DefaultSessionNamejMenuItemActionPerformed

private void filterAdcCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filterAdcCheckBoxMenuItemActionPerformed
    logger.setFilterAdc(filterAdcCheckBoxMenuItem.getState());
}//GEN-LAST:event_filterAdcCheckBoxMenuItemActionPerformed

private void filterWspCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filterWspCheckBoxMenuItemActionPerformed
    logger.setFilterWsp(filterWspCheckBoxMenuItem.getState());
}//GEN-LAST:event_filterWspCheckBoxMenuItemActionPerformed

private void sendButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sendButtonActionPerformed
    // TODO add your handling code here:
    String text = messageHistoryComboBox.getEditor().getItem().toString();
    messageHistoryComboBox.getEditor().setItem(text);
    //autoComplete.getCompleteAutoCompleteList().add(text);
}//GEN-LAST:event_sendButtonActionPerformed

private void filterPingCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filterPingCheckBoxMenuItemActionPerformed
    logger.setFilterPing(filterPingCheckBoxMenuItem.getState());
}//GEN-LAST:event_filterPingCheckBoxMenuItemActionPerformed

private void SessionNameTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_SessionNameTextFieldFocusLost
    EnableAccelerators();
    logger.setSessionName(SessionNameTextField.getText());
}//GEN-LAST:event_SessionNameTextFieldFocusLost

private void replayLogMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_replayLogMenuItemActionPerformed
    JFileChooser fc = new JFileChooser();
    fc.setAcceptAllFileFilterUsed(false);
    fc.addChoosableFileFilter(new ExtensionFilter("txt", "Text File"));

    int result = fc.showDialog(this, "Import for Replay");
    if (result == JFileChooser.APPROVE_OPTION) //If the user wants to save
    {
        File outFile = fc.getSelectedFile();        //Get the selected file
        new ReplayLog(logger, outFile).setVisible(true);
    }
}//GEN-LAST:event_replayLogMenuItemActionPerformed

private void previousButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_previousButtonActionPerformed
    // TODO add your handling code here:
    searchBackwardFor(this.SearchjTextField.getText().trim());
}//GEN-LAST:event_previousButtonActionPerformed

private void nextButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nextButtonActionPerformed
    // TODO add your handling code here:
    searchForwarFor(this.SearchjTextField.getText().trim());
}//GEN-LAST:event_nextButtonActionPerformed

private void SearchjTextFieldFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_SearchjTextFieldFocusGained
    // TODO add your handling code here:
    DisableAccelerators();
}//GEN-LAST:event_SearchjTextFieldFocusGained

private void SearchjTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_SearchjTextFieldFocusLost
    // TODO add your handling code here:
    EnableAccelerators();
}//GEN-LAST:event_SearchjTextFieldFocusLost

private void SessionNameTextFieldFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_SessionNameTextFieldFocusGained
    // TODO add your handling code here:
    DisableAccelerators();
}//GEN-LAST:event_SessionNameTextFieldFocusGained

private void filterSaccadeCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filterSaccadeCheckBoxMenuItemActionPerformed
// TODO add your handling code here:
    logger.setFilterSBMBMLSaccade(filterSaccadeCheckBoxMenuItem.getState());
}//GEN-LAST:event_filterSaccadeCheckBoxMenuItemActionPerformed

    private void filtervrPerceptionCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filtervrPerceptionCheckBoxMenuItemActionPerformed
        // TODO add your handling code here:
        logger.setFilerPMLMessages(filtervrPerceptionCheckBoxMenuItem.getState());
    }//GEN-LAST:event_filtervrPerceptionCheckBoxMenuItemActionPerformed

    /**
     * ActionListener for the "Import..." menu itme.  It first asks the user if they are sure.  If yes, then
     * it will open a file chooser to let the user pick a txt file.  Then it calls clearLogs() to clear the
     * log.  If there was a problem reading the file, then it will display an error dialog
     * message.  If the file was not created with JLogger, then it may cause unexpected behavior or results when importing.
     *
     * @param evt The ActionEvent
     */
    private void importMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        String text = "Are you sure that you want to import a log file?\nBe sure to choose a file that was created by JLogger.\n\nThe current log will be cleared.";
        int answer = JOptionPane.showConfirmDialog(this, text, "Import Log?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (answer == JOptionPane.YES_OPTION) {
            JFileChooser fc = new JFileChooser();
            fc.setAcceptAllFileFilterUsed(false);
            fc.addChoosableFileFilter(new ExtensionFilter("txt", "Text File"));

            int result = fc.showDialog(this, "Import");
            if (result == JFileChooser.APPROVE_OPTION) //If the user wants to save
            {
                File outFile = fc.getSelectedFile();        //Get the selected file
                importFile(outFile);
            }
        }
    }
    
    /*
     * Check if the sentence starts with a number
     * which possibly means that it's a new message
     */
    public boolean StringStartsWithNumber(String testLine) {
        
        if (testLine.startsWith("0") || testLine.startsWith("1") || testLine.startsWith("2") || testLine.startsWith("3") || testLine.startsWith("4")
                || testLine.startsWith("5") || testLine.startsWith("6") || testLine.startsWith("7") || testLine.startsWith("8") || testLine.startsWith("9"))
            return true;
        
        return false;
    }

    /**
     * Method to import a file.
     * @param outFile
     */
    private void importFile(File outFile){   
        //Get true or false if we are auto-scrolling the textareas
        //boolean scroll = autoScrollCheckBox.isSelected();
        //Updated by Apar Suri
        boolean scroll = autoScroll;
        ArrayList<String> toImport = new ArrayList<String>();
        String toDisplay = "";
        //Use Scanner to read the file
        try {
            Scanner sc = new Scanner(new BufferedReader(new FileReader(outFile.toString())));
            String line = sc.nextLine(); //header
            line = sc.nextLine(); //session name (second line of file).
            line = "";
            String temp = "";
            //String nextMessage = "";
            boolean nextMessagePresent = false;
            String in;
            boolean flag = false;
            clearLogs();        //clear all logs
            while (sc.hasNextLine()) {
                in = sc.nextLine().trim();
                if (in.compareTo("") == 0) {
                    continue;
                }
                if (line.startsWith("[") && in.startsWith("[")) {
                    temp = in;
                    flag = true;
                }else {
                    if (StringStartsWithNumber(in)) {
                       
                        
                        if (nextMessagePresent) {
                            toDisplay += line;
                            toDisplay += System.getProperty("line.separator");
                            nextMessagePresent = false;
                        }
                        else {
                            toDisplay += System.getProperty("line.separator");
                        }
                        line = "";
                    }
                    line = line + in;
                    while (sc.hasNextLine()) { //reading multi line messages
                        
                       
                        temp = sc.nextLine().trim();
                        if (!temp.startsWith("[") && !StringStartsWithNumber(temp)) {
                                line = line + temp;
                        }else {
                            if (!StringStartsWithNumber(temp))
                            {
                                flag = true;
                                break;
                            }
                            else // this means it is a new message with a timestamp
                            {
                                //nextMessage = temp;
                                nextMessagePresent = true;
                                flag = true;
                                break;
                            }
                        }
                    }
                }
                //Add the line to the arraylist
                toImport.add(line);
                
//                if (nextMessagePresent)
//                {
//                    toImport.add(nextMessage);
//                }

                if(toDisplay.compareTo("") == 0){
                    toDisplay = System.getProperty("line.separator");
                }
                toDisplay += line;

                //Iterate through the rest of the tabs (the FilterPanel objects) and call their appendText() methods
                //with the formatted message and if we're auto-scrolling
                for (int i = 1; i < tabbedPane.getTabCount(); i++) {
                    
                    FilterPanel fp = (FilterPanel) tabbedPane.getComponentAt(i);
                    fp.appendText(line, scroll);
                    
                    
                    if (nextMessagePresent && !sc.hasNextLine())
                    {
                        fp.appendText(temp, scroll);
                    }
                }
                if (flag) {
                    line = temp;
                } else {
                    line = "";
                }
                flag = false;
                //toDisplay += System.getProperty("line.separator");
                
                if (nextMessagePresent)
                {
                    toDisplay += System.getProperty("line.separator");
                    //toDisplay += nextMessage;
                    //nextMessagePresent = false;
                    if (!sc.hasNextLine())
                    {
                        toDisplay += line;
                        toDisplay += System.getProperty("line.separator");
                        break;
                    }
                    //flag = false;
                }
                
            }
            sc.close();
            textArea.setText(toDisplay);

            //auto scroll check
            if(scroll){
                textArea.setCaretPosition(textArea.getDocument().getLength() - toDisplay.length());
            }
            
            logger.importMessages(toImport); //import messages to logger
            //If there was an Exception, inform the user
        } catch (Exception e) {
            String msg = "Could not import log file.";
            JOptionPane.showMessageDialog(this, msg, "Import Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            clearLogs();
       }
    }

    /**
     * Method to append text to the text area and scroll it if necessary.
     * This method is to be used by the addMessageToAllMessagesTabThread class.
     * @param someText
     */
    public void appendToTextArea(String someText, int numberOfMessages){
        //assumes thread always adds a new line char at the start of every message
//       if(textArea.getText().compareTo("") == 0){
//            someText = someText.substring(1);
//            numberOfMessages--;
//        }
        JScrollBar vbar = scrollPane.getVerticalScrollBar();
        
       
        try {
             exLock.lock();
            //if the scroll is being dragged, disable autoscroll
            if (vbar.getValueIsAdjusting()) autoScroll = false;
            else
            {
                //if mouse is not being scrolled and the textarea caret is at the end, start autoscroll
                if (textArea.getCaretPosition() == textArea.getText().length() && isMouseScrolling == false) autoScroll = true;
                else autoScroll = false;
                //textArea.append(AllMessagesConsoleWriterThread.getCurrentDateTime() + " Outside - " + "Minimum - " + vbar.getMinimum() + ", Value - " + vbar.getValue() + ", Extent - " + vbar.getVisibleAmount() + ", Max - " + vbar.getMaximum());
                //Padding added for the times when user is close to the end but missed by a small amount (padding is approx 1% of max size of scroll)
                int padding = (int)0.01*vbar.getMaximum();
                //even though the mouse is being dragged or even though the caret is not at the end, if the scroll is infact at the end, autoscroll should be enabled
                if (vbar.getMaximum() <= vbar.getValue() + vbar.getVisibleAmount() + padding)
                {
                    autoScroll = true;
                    isMouseScrolling = false;
                }
                //else autoScroll = false;
            }
            //JOptionPane.showMessageDialog(null, "Value - " + vbar.getValue() + ", Extent - " + vbar.getVisibleAmount() + ", Max - " + vbar.getMaximum());

            //old code
            //if (autoScroll)
            //    vbar.setValue(vbar.getMaximum());

            //Older code
            //int val = vbar.getValue();
            //int vis = vbar.getVisibleAmount();
            //int max = vbar.getMaximum();
            //autoScroll = ((vbar.getValue() + vbar.getVisibleAmount()) == vbar.getMaximum());

            // Old code for check box
           //if (autoScrollCheckBox.isSelected())
           //     autoScroll = true;
           //else
           //     autoScroll = false;


    //        if (!autoScroll)
    //            System.out.println("weird");



            textArea.append(someText);
            //validate added to make sure changes are reflected onto the scrollbar
            //vbar.validate();
            //just to make sure that text is all appended and the vbar has adjusted
            //Thread.yield();
            //Thread.yield();

            //if autoscroll is on, make sure the vbar stays at the end
            if (autoScroll) {
                //textArea.append("AutoScrolling - " + "Value - " + vbar.getValue() + ", Extent - " + vbar.getVisibleAmount() + ", Max - " + vbar.getMaximum());
                //vbar.setValue(vbar.getMaximum());
                textArea.setCaretPosition(textArea.getText().length());
                //textArea.setCaretPosition(textArea.getDocument().getLength());
                //textArea.append("Final setValue - " + (vbar.getMaximum() - vbar.getVisibleAmount()));
                //Just to make sure that the changes are infact reflected
                textArea.validate();
                //vbar.validate();
                vbar.setValues(vbar.getMaximum(), vbar.getVisibleAmount(), vbar.getMinimum(), vbar.getMaximum());
                //vbar.validate();
                //vbar.setValue(vbar.getMaximum());
                //autoScroll = true;

                //JOptionPane.showMessageDialog(null, "AutoScrolling");
            }
            //else
                 //textArea.append(System.getProperty("line.separator") + "Value - " + vbar.getValue() + ", Max - " + vbar.getMaximum());
                //vbar.setValues(vbar.getMaximum(), vbar.getVisibleAmount(), vbar.getMinimum(), vbar.getMaximum());
        }
        catch (Exception ex) {
            System.out.println(ex);
        }
        finally {
            exLock.unlock();
        }

//         val = vbar.getValue();
//         vis = vbar.getVisibleAmount();
//         max = vbar.getMaximum();
    }

    /**
     * Keeps the number of visible lines in the GUI to a certain maximum by
     * deleting older ones. This has no effect to lines writen to file.
     * This function is called if the console writer thread detects the process
     * is close to running out of memory.
     */
    public void timeToStartDeletingThemLines() {
        if (maxNumberOfLines < 0)
            maxNumberOfLines = textArea.getLineCount();

        Element root = textArea.getDocument().getDefaultRootElement();
        
        //System.out.println("\nAct lines: " + root.getElementCount());
        //System.out.println("Max lines: " + this.maxNumberOfLines);

        while (root.getElementCount() > maxNumberOfLines)
        {
            Element line = root.getElement(500); // Delete 100 lines at once

            try
            {
                textArea.getDocument().remove(0, line.getEndOffset());
            }
            catch(BadLocationException ble)
            {
                System.out.println("Error while trying to keep number of lines in TextArea static." + ble);
            }
        }
        textArea.validate();
    }

    /**
     * Removes a given number of lines from the beginning of the textarea.
     * Useful to prevent buffer overflow.
     * @param numberOfLines     the number of lines to be removed
     */
    public void removeFromTextArea(int numberOfLines) {
         System.out.println("doclinc " + textArea.getDocument().getLength());
            System.out.println("linecnt " + textArea.getLineCount());
            Element root = textArea.getDocument().getDefaultRootElement();
            System.out.println("elemcnt " + root.getElementCount());


       // for (int i=0; i<numberOfLines; i++)
        //{
           

            try {
                Element line = root.getElement(numberOfLines-1);
                textArea.getDocument().remove(0,line.getEndOffset());
            } catch (Exception e) {
                //System.out.println("Tried to delete lines from text area. Error: " + e.printStackTrace());
            }
    //}
        textArea.validate();
    
    }

    private static String importFileName = null;
    private static boolean startIconified = false;
    private static String server = null;
    private static String scope = null;

    /**
     * @param args the command line arguments
     **/
	public static void main(final String[] args) {
		String argument = null;

		for (int i = 0; i < args.length; i++) {
			argument = args[i];

			if (argument.startsWith("-f") == true) {
				importFileName = argument.substring(3, argument.length());

				if (importFileName.startsWith("\"") && importFileName.endsWith("\"")) {
					importFileName = importFileName.substring(1, importFileName.length() - 1);
				}
			} else if (argument.equals("-i") == true) {
				startIconified = true;
			} else if (argument.equals("-server") == true) {
				if (++i < args.length)
					server = args[i];
			} else if (argument.equals("-scope") == true) {
				if (++i < args.length)
					scope = args[i];
			} else if (argument.equals("-root") == true) {
				if (++i < args.length)
					LogManager.ROOT = new File(args[i]);
			}
		}

		java.awt.EventQueue.invokeLater(new Runnable() {

			public void run() {
				new JLogger(importFileName, startIconified, server, scope).setVisible(true);
			}
		});
	}

    /**
     * This method will create a new filter tab; however, it is for creating an
     * advanced filter.  It is called by the AdvancedFilterCreatorDialog when the
     * user clicks its "Create" button.  It also builds the filter tab label
     * based on the filter terms.
     *
     * @param filts An array of AdvancedFilterObjects to use when creating the new filter tab
     */
    public void createAdvancedFilter(AdvancedFilterObject[] filts, boolean usesAnd) {
        //Create the tab label
        String filterTabLabel = "";
        for (int i = 0; i < filts.length; i++) {
            if (i != 0) {
                if (usesAnd) {
                    filterTabLabel += "and ";
                } else {
                    filterTabLabel += "or ";
                }
            }
            if (filts[i].isPositive()) {
                filterTabLabel = filterTabLabel + "[+] " + filts[i].getFilter();
            } else {
                filterTabLabel = filterTabLabel + "[-] " + filts[i].getFilter();
            }

            if (i < filts.length - 1) {
                filterTabLabel = filterTabLabel + ", ";
            }
        }

        //create a new AdvancedFilterPanel and add it to the tabbedPane
        try {
            //Added by Apar Suri
            String textToBeFiltered = textArea.getText();
            String[] messages = textToBeFiltered.split(System.getProperty("line.separator"));

            //Added by Apar Suri to make sure that the already present messages are also filtered
            AdvancedFilterPanel aFilterP = new AdvancedFilterPanel(filts, messages, autoScroll, usesAnd, this);
            
            //AdvancedFilterPanel aFilterP = new AdvancedFilterPanel(filts, usesAnd, this);
            tabbedPane.add(filterTabLabel, aFilterP);
        } catch (Exception err) {
            String errormsg = "An Unknown Parsing Error occured:\n\n" + err.toString();
            JOptionPane.showMessageDialog(this, errormsg, "Unknown Parsing Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Method used by the logger to inform this class that it has started logging
     * @param logFile the file (with a full path) the logger is using to log all messages
     */
    public void startedLogging(String logFile) {
        loggingPathjMenuItem.setEnabled(true);
        //StatusjPanel.setBackground(Color.GREEN);
        this.SessionNameTextField.setBackground(new Color(51,255,148));    //   Color.getHSBColor(149,80,81));
    }

    /**
     * Method used by the logger to inform this class that it has stopped logging
     */
    public void stoppedLogging() {
        loggingPathjMenuItem.setEnabled(false);
        //StatusjPanel.setBackground(Color.RED);
        this.SessionNameTextField.setBackground(new Color(255,91,75));
    }

    /**
     * The logger calls this method to inform the gui that an error has occured; it simply informs the user of the error.
     * @param msg the message the logger was processing when the error occurred
     * @param e the exception thrown as a consequence of the error
     */
    public void errorWithMessage(String msg, Exception e) {
        String text = "An error occurred while logging or adding the message: " + msg + "\n" + e.toString();
        e.printStackTrace();
        JOptionPane.showConfirmDialog(this, text, "Error with Message", JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Method the logger uses to inform the GUI that there is a new message to display and filter
     * @param msg the new message
     */
    public void newMessage(String msg) {
        addMessageThread.addMessage(msg); //add message

        //Iterate through the rest of the tabs (the FilterPanel objects) and call their appendText() methods
        //with the formatted message and if we're auto-scrolling
        for (int i = 1; i < tabbedPane.getTabCount(); i++) {
            FilterPanel fp = (FilterPanel) tabbedPane.getComponentAt(i);
            try {
                //fp.appendText(msg, autoScrollCheckBox.isSelected());
                //Updated by Apar Suri
                fp.appendText(msg, autoScroll);
            } catch (Exception err) {
                String errormsg = "An Unknown Parsing Error occured:\n\n" + err.toString();
                JOptionPane.showMessageDialog(this, errormsg, "Unknown Parsing Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem AboutjMenuItem;
    private javax.swing.JMenuItem CloseCurrentFilterjMenuItem;
    private javax.swing.JMenuItem CloseTabjMenuItem;
    private javax.swing.JMenuItem CreateAdvancedFilterjMenuItem;
    private javax.swing.JMenuItem DefaultSessionNamejMenuItem;
    private javax.swing.JMenuItem HowTojMenuItem;
    private javax.swing.JMenuItem KeywordjMenuItem;
    private javax.swing.JMenuItem SaveTabjMenuItem;
    private javax.swing.JTextField SearchjTextField;
    private javax.swing.JTextField SessionNameTextField;
    private javax.swing.JMenuItem SimpleFilterjMenuItem;
    private javax.swing.JButton StartJButton;
    private javax.swing.JPanel StatusjPanel;
    private javax.swing.JPopupMenu TabMenujPopupMenu;
    private javax.swing.JMenuItem WordWrapjMenuItem;
    private javax.swing.JMenuItem allSaveAsMenuItem;
    private javax.swing.JButton buttonNext;
    private javax.swing.JButton buttonPrevious;
    private javax.swing.JButton clearButton;
    private javax.swing.JButton clearHistoryButton;
    private javax.swing.JMenuItem clearjMenuItem;
    private javax.swing.JMenuItem currentSaveAsMenuItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JCheckBoxMenuItem filterAdcCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem filterPingCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem filterSaccadeCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem filterWspCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem filtervrPerceptionCheckBoxMenuItem;
    private javax.swing.JMenu helpjMenu;
    private javax.swing.JMenuItem importMenuItem;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabelSearchStatus;
    private javax.swing.JLabel jLabelVHMSGScope;
    private javax.swing.JLabel jLabelVHMSGServer;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu5;
    private javax.swing.JMenu jMenu6;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JPanel logPanel;
    private javax.swing.JMenuItem loggingPathjMenuItem;
    private javax.swing.JComboBox messageHistoryComboBox;
    private javax.swing.JMenuItem quitMenuItem;
    private javax.swing.JMenuItem replayLogMenuItem;
    private javax.swing.JMenuItem resetSessionNamejMenuItem;
    private javax.swing.JScrollPane scrollPane;
    private javax.swing.JButton sendButton;
    private javax.swing.JButton stopjButton;
    private javax.swing.JTabbedPane tabbedPane;
    private javax.swing.JTextArea textArea;
    private javax.swing.ButtonGroup typeButtonGroup;
    // End of variables declaration//GEN-END:variables

		// Added by Anton Leuski
	// The code below handles Mac-specific events. Currently it only handles the
	// Quit menu item, which is added automatically by the system.
	// The code is a bit weird -- I'm doing "dynamic linking", using reflection to
	// allow this code to compile on other platforms when Apple-provided classes
	// are not present

	@SuppressWarnings({"UnusedDeclaration"})
	private static interface MacApplication {
		public void setEnabledPreferencesMenu(boolean b);
		public void setEnabledAboutMenu(boolean b);
		public boolean getEnabledPreferencesMenu();
		public boolean getEnabledAboutMenu();
		public boolean isAboutMenuItemPresent();
		public void addAboutMenuItem();
		public void removeAboutMenuItem();
		public boolean isPreferencesMenuItemPresent();
		public void addPreferencesMenuItem();
		public void removePreferencesMenuItem();
		public void openHelpViewer();
		public void setDockMenu(java.awt.PopupMenu inPopupMenu);
		public java.awt.PopupMenu getDockMenu();
		public void setDockIconImage(java.awt.Image inImage);
		public java.awt.Image getDockIconImage();
		public void setDockIconBadge(java.lang.String s);
	}

	private static MacApplication	sMacApplication;
	private static MacApplication getMacApplication() {
		if (sMacApplication == null) {
			sMacApplication	= (MacApplication) Proxy.newProxyInstance(JLogger.class.getClassLoader(), new Class[]{MacApplication.class}, new MacApplicationInvocationHandler());
		}
		return sMacApplication;
	}

	private static class MacApplicationInvocationHandler implements InvocationHandler {

		private Object	mMacApplication;

		private MacApplicationInvocationHandler() {
			try {
				//noinspection HardCodedStringLiteral
				mMacApplication	= Class.forName("com.apple.eawt.Application").getMethod("getApplication").invoke(null);
			} catch (Throwable t) {
				mMacApplication	= null;
			}
		}

		public Object invoke(Object proxy, Method inMethod, Object[] args) throws Throwable {
			if (mMacApplication == null) return null;
			return mMacApplication.getClass().getMethod(inMethod.getName(), inMethod.getParameterTypes()).invoke(mMacApplication, args);
		}
	}

	private static void setSystemProperty(String inProperty, String inValue) {
		if (System.getProperty(inProperty) == null)
			System.setProperty(inProperty, inValue);
	}

	@SuppressWarnings({"HardCodedStringLiteral"})
	private boolean registerMacApplication(String inName) {



		try {

			if (getMacApplication().isPreferencesMenuItemPresent()) {
				getMacApplication().setEnabledPreferencesMenu(false);
				getMacApplication().removePreferencesMenuItem();
			}

			if (!getMacApplication().isAboutMenuItemPresent()) {
				getMacApplication().addAboutMenuItem();
				getMacApplication().setEnabledAboutMenu(true);
			}

			Class	appleApplicationClass			= Class.forName("com.apple.eawt.Application");
			Class	appleApplicationListenerClass	= Class.forName("com.apple.eawt.ApplicationListener");
			Class	appleApplicationEventClass		= Class.forName("com.apple.eawt.ApplicationEvent");

			Object  fApplication 					= appleApplicationClass.getMethod("getApplication").invoke(null);

			final Method	setHandled				= appleApplicationEventClass.getMethod("setHandled", 	boolean.class);
			final Method	getFilename				= appleApplicationEventClass.getMethod("getFilename");

			InvocationHandler	theListener			= new InvocationHandler() {

				@SuppressWarnings({"HardCodedStringLiteral"})
				public Object invoke(
						Object proxy,
						Method method,
						Object[] args) throws Throwable
				{
					String	methodName	= method.getName();
					if (methodName.equals("handleAbout")) {
						JLogger.this.AboutjMenuItemActionPerformed(null);
						setHandled.invoke(args[0], true); // we displayed our dialog
//						setHandled.invoke(args[0], Application.this.handleAbout());
					} else if (methodName.equals("handleOpenApplication")) {
//						setHandled.invoke(args[0], Application.this.handleOpenApplication());
					} else if (methodName.equals("handleOpenFile")) {
//						String	fileName	= (String)getFilename.invoke(args[0], (Object[])null);
//						setHandled.invoke(args[0], Application.this.handleOpenFile(fileName));
					} else if (methodName.equals("handlePreferences")) {
//						setHandled.invoke(args[0], Application.this.handlePreferences());
					} else if (methodName.equals("handlePrintFile")) {
//						String	fileName	= (String)getFilename.invoke(args[0], (Object[])null);
//						setHandled.invoke(args[0], Application.this.handlePrintFile(fileName));
					} else if (methodName.equals("handleQuit")) {
						JLogger.this.quitMenuItemActionPerformed(null);
						setHandled.invoke(args[0], false); // reject quit. We handle it ourselves.
					} else if (methodName.equals("handleReOpenApplication")) {
					} else {
//						getLogger().severe("Someone called an unsupported com.apple.eawt.ApplicationListener method: " + methodName);
					}
					return null;
				}
			};


			Method	addApplicationListener			= appleApplicationClass.getMethod("addApplicationListener", appleApplicationListenerClass);
			addApplicationListener.invoke(fApplication, 	Proxy.newProxyInstance( appleApplicationListenerClass.getClassLoader(),
                                          									new Class[] { appleApplicationListenerClass },
                                          									theListener));

		} catch (Throwable t) {
			t.printStackTrace();
			return false;
		}
		return true;
	}

}
