package src.view;

import java.awt.event.KeyEvent;
import javax.swing.JDialog;

/**
 * This class will Display a dialog informing the how to do certain tasks in the JLogger
 * 
 * @author Brenda Medina
 */
public class HowTo extends javax.swing.JPanel
{
    
    /**
     * The dialog that we're displaying
     */
    private JDialog myDialog;

    /**
     * The parameterized constructor displays the dialog
     * 
     * @param logger the JLogger that this dialog will associated with
     */
    public HowTo(JLogger logger)
    {
   
        initComponents();
        
        //Display the JDialog
        myDialog = new JDialog(logger, "How to ...", true);
        myDialog.setContentPane(this);
        myDialog.pack();
        //myDialog.setResizable(false);
        myDialog.setLocationRelativeTo(logger);
        String how =    "HOW TO ... \n\n" +
                        "1. change or reset the log session Name:\n" +
                        "   To change the session name: Type in the name of the session in the space provided and then\n" +
                        "   you MUST press enter.\n" +
                        "   To reset the session name: Click on the Edit menu and select the Reset Session Name Option.\n" +
                        "     To see what the default session name is, before resetting it, click on the Options menu and select\n" +
                        "     the Get Default Session Name option.\n" +
                        "   Note: if JLogger is logging, then the session name will not take effect until after you press stop,\n" +
                        "         and then start logging again.\n\n" +
                        "2. Start/Stop a log session:\n" +
                        "   Click on the Start Logging or Stop Logging button on the JLogger.\n\n" +
                        "3. Create an advanced filter:\n" +
                        "   Click on the Filters menu, and then select the Create-Advanced-Filter option.\n" +
                        "   Enter up to five terms and select if each one is positive or negative,\n" +
                        "   select the logical operator,'and' or 'or' option, and then click on the create button.\n\n" +
                        "4. Get the path of the directory where JLogger is saving the current logs:\n" +
                        "   If JLogger is currently logging, click on the Options menu and select the Get Logging Path option.\n" +
                        "   A dialog will appear displaying the path, click ok or cancel when you are done viewing the path.\n\n" +
                        "5. Import a log file:\n" +
                        "   Click on the File menu and select the Import Log option.\n" +
                        "   A dialog will appear in which you have to tell JLogger where the file is.\n" +
                        "   The Log file must have been created by JLogger, otherwise correct behavior is not guaranteed.\n\n" +
                        "6. Clear the log:\n" +
                        "   Click on the Edit menu and select the Clear Log option.\n" +
                        "   A dialog will appear asking if you are sure you want to clear the logs, select Yes to clear the logs.\n\n" +
                        "7. Search the messages:\n" +
                        "   Click on the Search tab and select the By Keyword option.\n" +
                        "   A dialog will appear: type in the keyword you want to search for in the text field provided.\n" +
                        "   Press F3 to find the next instance of the keyword or Shift + F3 for the previous instance.\n" +
                        "   Note: Search results will be highlighted in the text area.\n" +
                        "   Click on Cancel when you are finished searching.\n" +
                        "   Note: If the Autoscroll checkbox is selected, JLogger will deselect it when you are searching, and you must\n" +
                        "   Select it again when you are done searching if you wish to enable it again.\n" +
                        "   Note: If you press enter while searching, when the cursor is still in the searching text field, then\n" +
                        "   the search will start from the beginning of the document again.\n\n" +
                        "8. Save all messages:\n" +
                        "   Click on the File menu and select the [All Messages] Save As option.\n" +
                        "   A dialog will appear where you must select the location and name of the file where \n" +
                        "   the messages will be saved to, then select the save option.\n\n" +
                        "9. Save messaegs from a filter:\n" +
                        "   Select the tab of the filter you wish to save.\n" +
                        "   Click on the File menu and select the [Current Filter] Save As option.\n" +
                        "   A dialog will appear where you must specify the file name and location.\n" +
                        "   Select the save option when you are done.\n" +
                        "   Note: you can also save a filter by selecting the tab of the filter and right clicking on it, \n" +
                        "   and then selecting the Save option.\n\n\n" +
                        "NOTE: Most of JLogger's optons have a keyboard shortcut, see the menus for these shortcuts.\n";
        HowToJLoggerjTextArea.append(how);
        HowToJLoggerjTextArea.setCaretPosition(0);
        HowToJLoggerjTextArea.requestFocus();
        myDialog.setVisible(true);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSeparator1 = new javax.swing.JSeparator();
        jSeparator6 = new javax.swing.JSeparator();
        OkButton = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        HowToJLoggerjTextArea = new javax.swing.JTextArea();

        OkButton.setText("Ok");
        OkButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                OkButtonActionPerformed(evt);
            }
        });

        jLabel1.setText("How To");

        HowToJLoggerjTextArea.setColumns(20);
        HowToJLoggerjTextArea.setEditable(false);
        HowToJLoggerjTextArea.setLineWrap(true);
        HowToJLoggerjTextArea.setRows(5);
        HowToJLoggerjTextArea.setWrapStyleWord(true);
        HowToJLoggerjTextArea.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                HowToJLoggerjTextAreaKeyReleased(evt);
            }
        });
        jScrollPane1.setViewportView(HowToJLoggerjTextArea);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSeparator1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 890, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 149, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(731, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addComponent(jSeparator6, javax.swing.GroupLayout.DEFAULT_SIZE, 880, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(805, Short.MAX_VALUE)
                .addComponent(OkButton, javax.swing.GroupLayout.PREFERRED_SIZE, 59, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(26, 26, 26))
            .addGroup(layout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 861, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 237, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(jSeparator6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(OkButton)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Private method to close the dialog when the user clicks ok
     * @param evt
     */
    private void OkButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_OkButtonActionPerformed
        myDialog.setVisible(false);
}//GEN-LAST:event_OkButtonActionPerformed

    /**
     * Action LIstener to allow user to press enter to exit instead of clikcing ok button
     * @param evt
     */
    private void HowToJLoggerjTextAreaKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_HowToJLoggerjTextAreaKeyReleased
        if(evt.getKeyCode() == KeyEvent.VK_ENTER)
        {
            OkButton.doClick();
        }
    }//GEN-LAST:event_HowToJLoggerjTextAreaKeyReleased

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextArea HowToJLoggerjTextArea;
    private javax.swing.JButton OkButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator6;
    // End of variables declaration//GEN-END:variables

}