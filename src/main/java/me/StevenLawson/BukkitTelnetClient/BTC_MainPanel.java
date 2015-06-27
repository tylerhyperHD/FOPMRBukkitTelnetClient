package me.StevenLawson.BukkitTelnetClient;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JScrollBar;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import static me.StevenLawson.BukkitTelnetClient.BTC_TelnetMessage.BTC_LogMessageType.ADMINSAY_MESSAGE;
import static me.StevenLawson.BukkitTelnetClient.BTC_TelnetMessage.BTC_LogMessageType.CHAT_MESSAGE;
import static me.StevenLawson.BukkitTelnetClient.BTC_TelnetMessage.BTC_LogMessageType.ADMINCHATCL1_MESSAGE;
import static me.StevenLawson.BukkitTelnetClient.BTC_TelnetMessage.BTC_LogMessageType.SAY_MESSAGE;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

public class BTC_MainPanel extends javax.swing.JFrame
{
    private final BTC_ConnectionManager connectionManager = new BTC_ConnectionManager();
    private final Collection<FavoriteButtonEntry> favButtonList = BukkitTelnetClient.config.getFavoriteButtons();

    public BTC_MainPanel()
    {
        initComponents();
    }

    public void setup()
    {
        this.txtServer.getEditor().getEditorComponent().addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyTyped(KeyEvent e)
            {
                if (e.getKeyChar() == KeyEvent.VK_ENTER)
                {
                    BTC_MainPanel.this.saveServersAndTriggerConnect();
                }
            }
        });

        this.loadServerList();
        this.loadFonts();
        this.loadRGB();

        final URL icon = this.getClass().getResource("/icon.png");
        if (icon != null)
        {
            setIconImage(Toolkit.getDefaultToolkit().createImage(icon));
        }
        
        this.getConnectionManager().updateTitle(false);
               
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

    private final Queue<BTC_TelnetMessage> telnetErrorQueue = new LinkedList<>();
    private boolean isQueueing = false;

    private void flushTelnetErrorQueue()
    {
        BTC_TelnetMessage queuedMessage;
        while ((queuedMessage = telnetErrorQueue.poll()) != null)
        {
            queuedMessage.setColor(Color.GRAY);
            writeToConsoleImmediately(queuedMessage, true);
        }
    }

    public void writeToConsole(final BTC_ConsoleMessage message)
    {
        if (message.getMessage().isEmpty())
        {
            return;
        }

        if (message instanceof BTC_TelnetMessage)
        {
            final BTC_TelnetMessage telnetMessage = (BTC_TelnetMessage) message;

            if (telnetMessage.isInfoMessage())
            {
                isQueueing = false;
                flushTelnetErrorQueue();
            }
            else if (telnetMessage.isErrorMessage() || isQueueing)
            {
                isQueueing = true;
                telnetErrorQueue.add(telnetMessage);
            }

            if (!isQueueing)
            {
                writeToConsoleImmediately(telnetMessage, false);
            }
        }
        else
        {
            isQueueing = false;
            flushTelnetErrorQueue();
            writeToConsoleImmediately(message, false);
        }
    }

    private void writeToConsoleImmediately(final BTC_ConsoleMessage message, final boolean isTelnetError)
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                if (isTelnetError && chkIgnoreErrors.isSelected())
                {
                    return;
                }

                final StyledDocument styledDocument = mainOutput.getStyledDocument();

                int startLength = styledDocument.getLength();

                try
                {
                    styledDocument.insertString(
                            styledDocument.getLength(),
                            message.getMessage() + SystemUtils.LINE_SEPARATOR,
                            StyleContext.getDefaultStyleContext().addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, message.getColor())
                    );
                }
                catch (BadLocationException ex)
                {
                    throw new RuntimeException(ex);
                }

                if (BTC_MainPanel.this.chkAutoScroll.isSelected() && BTC_MainPanel.this.mainOutput.getSelectedText() == null)
                {
                    final JScrollBar vScroll = mainOutputScoll.getVerticalScrollBar();

                    if (!vScroll.getValueIsAdjusting())
                    {
                        if (vScroll.getValue() + vScroll.getModel().getExtent() >= (vScroll.getMaximum() - 10))
                        {
                            BTC_MainPanel.this.mainOutput.setCaretPosition(startLength);

                            final Timer timer = new Timer(10, new ActionListener()
                            {
                                @Override
                                public void actionPerformed(ActionEvent ae)
                                {
                                    vScroll.setValue(vScroll.getMaximum());
                                }
                            });
                            timer.setRepeats(false);
                            timer.start();
                        }
                    }
                }
                
                final StyledDocument chatDocument = chatOutput.getStyledDocument();

                int chatLength = chatDocument.getLength();
                if (message instanceof BTC_TelnetMessage)
                {
                    BTC_TelnetMessage telnetMessage = (BTC_TelnetMessage) message;
                    BTC_TelnetMessage.BTC_LogMessageType messageType = telnetMessage.getMessageType();
                    
                    switch(messageType)
                    {
                        case CHAT_MESSAGE:
                            if(!showChat.isSelected())
                            {
                                return;
                            }
                        break;
                        case OPCHATCL_MESSAGE:
                            if(!showOpChatCL.isSelected())
                            {
                                return;
                            }
                        break;
                        case ADMINCHATCL1_MESSAGE:
                            if(!showCL1.isSelected())
                            {
                                return;
                            }
                        break;
                        case ADMINCHATCL2_MESSAGE:
                            if(!showCL2.isSelected())
                            {
                                return;
                            }
                        break;
                        case ALLOTHERCLS_MESSAGE:
                            if(!showAllOtherCls.isSelected())
                            {
                                return;
                            }
                        break;
                        case SAY_MESSAGE:
                            if(!showSay.isSelected())
                            {
                                return;
                            }
                        break;
                        case ADMINSAY_MESSAGE:
                            if(!showAdminSay.isSelected())
                            {
                                return;
                            }
                        break;
                        default:
                            return;
                    }
                    try
                    {
                        chatDocument.insertString(
                                                  chatDocument.getLength(),
                                                  message.getMessage() + SystemUtils.LINE_SEPARATOR,
                                                  StyleContext.getDefaultStyleContext().addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, telnetMessage.getColor())
                                                  );
                    }
                    catch (BadLocationException ex)
                    {
                        throw new RuntimeException(ex);
                    }
                    
                    if (BTC_MainPanel.this.chatAutoScroll.isSelected() && BTC_MainPanel.this.chatOutput.getSelectedText() == null)
                    {
                        final JScrollBar vScroll = chatOutputScroll.getVerticalScrollBar();
                        
                        if (!vScroll.getValueIsAdjusting())
                        {
                            if (vScroll.getValue() + vScroll.getModel().getExtent() >= (vScroll.getMaximum() - 10))
                            {
                                BTC_MainPanel.this.chatOutput.setCaretPosition(startLength);
                                
                                final Timer timer = new Timer(10, new ActionListener()
                                                              {
                                    @Override
                                    public void actionPerformed(ActionEvent ae)
                                    {
                                        vScroll.setValue(vScroll.getMaximum());
                                    }
                                });
                                timer.setRepeats(false);
                                timer.start();
                            }
                        }
                    }
                }
            }
        });
    }
   
    public void copyToClipboard(final String myString)
    {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(myString), null);
    }
    
    public final void loadServerList()
    {
        txtServer.removeAllItems();
        for (final ServerEntry serverEntry : BukkitTelnetClient.config.getServers())
        {
            txtServer.addItem(serverEntry);
            if (serverEntry.isLastUsed())
            {
                txtServer.setSelectedItem(serverEntry);
            }
        }
    }
    
    public final void loadFonts()
    {
        FontFace.removeAllItems();
        GraphicsEnvironment e = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Font[] font = e.getAllFonts();
        for (Font f : font)
        {
            FontFace.addItem(f.getFontName());
        }
        FontFace.setSelectedItem(BTC_MainPanel.this.mainOutput.getFont());
    }
    
    public final void loadRGB()
    {
        List<Component> RGB = new ArrayList<>();
        RGB.add(TR);
        RGB.add(TG);
        RGB.add(TB);
        RGB.add(BR);
        RGB.add(BG);
        RGB.add(BB);
        for (Component c : RGB)
        {
            SpinnerModel numbers = new SpinnerNumberModel(0, 0, 255, 1);
            JSpinner spin = (JSpinner) c;
            spin.removeAll();
            spin.setModel(numbers);
        }
    }
    
    public final void saveServersAndTriggerConnect()
    {
        final Object selectedItem = txtServer.getSelectedItem();
        
        ServerEntry entry;
        if (selectedItem instanceof ServerEntry)
        {
            entry = (ServerEntry) selectedItem;
        }
        else
        {
            final String serverAddress = StringUtils.trimToNull(selectedItem.toString());
            if (serverAddress == null)
            {
                return;
            }
            
            String serverName = JOptionPane.showInputDialog(this, "Enter server name:", "Server Name", JOptionPane.PLAIN_MESSAGE);
            if (serverName == null)
            {
                return;
            }
            
            serverName = StringUtils.trimToEmpty(serverName);
            if (serverName.isEmpty())
            {
                serverName = "Unnamed";
            }
            entry = new ServerEntry(serverName, serverAddress);
            
            BukkitTelnetClient.config.getServers().add(entry);
        }
        
        for (final ServerEntry existingEntry : BukkitTelnetClient.config.getServers())
        {
            if (entry.equals(existingEntry))
            {
                entry = existingEntry;
            }
            existingEntry.setLastUsed(false);
        }
        
        entry.setLastUsed(true);
        
        BukkitTelnetClient.config.save();
        
        loadServerList();
        
        getConnectionManager().triggerConnect(entry.getAddress());
    }
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        splitPane = new javax.swing.JSplitPane();
        jPanel3 = new javax.swing.JPanel();
        mainOutputScoll = new javax.swing.JScrollPane();
        mainOutput = new javax.swing.JTextPane();
        btnDisconnect = new javax.swing.JButton();
        btnSend = new javax.swing.JButton();
        txtServer = new javax.swing.JComboBox<ServerEntry>();
        chkAutoScroll = new javax.swing.JCheckBox();
        txtCommand = new javax.swing.JTextField();
        btnConnect = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel2 = new javax.swing.JPanel();
        favoriteButtonsPanelHolder = new javax.swing.JPanel();
        favoriteButtonsPanelScroll = new javax.swing.JScrollPane();
        favoriteButtonsPanel = new BTC_FavoriteButtonsPanel(favButtonList);
        jPanel4 = new javax.swing.JPanel();
        message = new javax.swing.JTextField();
        chat = new javax.swing.JButton();
        chatOutputScroll = new javax.swing.JScrollPane();
        chatOutput = new javax.swing.JTextPane();
        chatAutoScroll = new javax.swing.JCheckBox();
        showChat = new javax.swing.JCheckBox();
        showOpChatCL = new javax.swing.JCheckBox();
        showSay = new javax.swing.JCheckBox();
        showCL1 = new javax.swing.JCheckBox();
        showCL2 = new javax.swing.JCheckBox();
        showAllOtherCls = new javax.swing.JCheckBox();
        showAdminSay = new javax.swing.JCheckBox();
        jTabbedPane2 = new javax.swing.JTabbedPane();
        jPanel7 = new javax.swing.JPanel();
        SaveColours = new javax.swing.JButton();
        jLabel10 = new javax.swing.JLabel();
        BB = new javax.swing.JSpinner();
        BG = new javax.swing.JSpinner();
        BR = new javax.swing.JSpinner();
        TB = new javax.swing.JSpinner();
        TG = new javax.swing.JSpinner();
        TR = new javax.swing.JSpinner();
        jLabel9 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel12 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jPanel6 = new javax.swing.JPanel();
        fflabel = new javax.swing.JLabel();
        FontFace = new javax.swing.JComboBox();
        fslabel = new javax.swing.JLabel();
        FontSize = new javax.swing.JTextField();
        save = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        chkIgnorePlayerCommands = new javax.swing.JCheckBox();
        chkIgnoreServerCommands = new javax.swing.JCheckBox();
        chkShowChatOnly = new javax.swing.JCheckBox();
        chkIgnoreErrors = new javax.swing.JCheckBox();
        PluginName = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        saveplugin = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JSeparator();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("FreedomOpRemasteredTelnetClient");

        splitPane.setResizeWeight(1.0);

        jPanel3.setName(""); // NOI18N

        mainOutput.setFont(new java.awt.Font("Comic Sans MS", 0, 12)); // NOI18N
        mainOutputScoll.setViewportView(mainOutput);

        btnDisconnect.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N
        btnDisconnect.setText("Disconnect");
        btnDisconnect.setEnabled(false);
        btnDisconnect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDisconnectActionPerformed(evt);
            }
        });

        btnSend.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N
        btnSend.setText("Send");
        btnSend.setEnabled(false);
        btnSend.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSendActionPerformed(evt);
            }
        });

        txtServer.setEditable(true);
        txtServer.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N

        chkAutoScroll.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N
        chkAutoScroll.setSelected(true);
        chkAutoScroll.setText("AutoScroll");

        txtCommand.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N
        txtCommand.setEnabled(false);
        txtCommand.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtCommandActionPerformed(evt);
            }
        });
        txtCommand.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txtCommandKeyPressed(evt);
            }
        });

        btnConnect.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N
        btnConnect.setText("Connect");
        btnConnect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnConnectActionPerformed(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N
        jLabel1.setText("Command:");

        jLabel2.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N
        jLabel2.setText("Server:");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(mainOutputScoll)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel1))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtCommand)
                            .addComponent(txtServer, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(btnConnect, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btnSend, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btnDisconnect)
                            .addComponent(chkAutoScroll))))
                .addContainerGap())
        );

        jPanel3Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {btnConnect, btnDisconnect, btnSend, chkAutoScroll});

        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addComponent(mainOutputScoll)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtCommand, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)
                    .addComponent(btnSend)
                    .addComponent(chkAutoScroll))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(btnConnect)
                    .addComponent(btnDisconnect)
                    .addComponent(txtServer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        splitPane.setLeftComponent(jPanel3);

        jTabbedPane1.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N

        favoriteButtonsPanelHolder.setLayout(new java.awt.BorderLayout());

        favoriteButtonsPanelScroll.setBorder(null);

        favoriteButtonsPanel.setLayout(null);
        favoriteButtonsPanelScroll.setViewportView(favoriteButtonsPanel);

        favoriteButtonsPanelHolder.add(favoriteButtonsPanelScroll, java.awt.BorderLayout.CENTER);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(favoriteButtonsPanelHolder, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(0, 0, 0))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(favoriteButtonsPanelHolder, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Commands", jPanel2);

        message.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N
        message.setToolTipText("Message");
        message.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                messageActionPerformed(evt);
            }
        });

        chat.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N
        chat.setText("Chat");
        chat.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chatActionPerformed(evt);
            }
        });

        chatOutput.setEditable(false);
        chatOutput.setFont(new java.awt.Font("Comic Sans MS", 0, 12)); // NOI18N
        chatOutputScroll.setViewportView(chatOutput);

        chatAutoScroll.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N
        chatAutoScroll.setSelected(true);
        chatAutoScroll.setText("AutoScroll");
        chatAutoScroll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chatAutoScrollActionPerformed(evt);
            }
        });

        showChat.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N
        showChat.setSelected(true);
        showChat.setText("Chat");
        showChat.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showChatActionPerformed(evt);
            }
        });

        showOpChatCL.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N
        showOpChatCL.setSelected(true);
        showOpChatCL.setText("CL 0");
        showOpChatCL.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showOpChatCLActionPerformed(evt);
            }
        });

        showSay.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N
        showSay.setSelected(true);
        showSay.setText("Say");

        showCL1.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N
        showCL1.setSelected(true);
        showCL1.setText("CL 1");

        showCL2.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N
        showCL2.setSelected(true);
        showCL2.setText("CL 2");
        showCL2.setDoubleBuffered(true);
        showCL2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showCL2ActionPerformed(evt);
            }
        });

        showAllOtherCls.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N
        showAllOtherCls.setSelected(true);
        showAllOtherCls.setText("All Other CL's");
        showAllOtherCls.setToolTipText("");
        showAllOtherCls.setDoubleBuffered(true);
        showAllOtherCls.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showAllOtherClsActionPerformed(evt);
            }
        });

        showAdminSay.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N
        showAdminSay.setSelected(true);
        showAdminSay.setText("Admin Say");
        showAdminSay.setToolTipText("");
        showAdminSay.setDoubleBuffered(true);
        showAdminSay.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showAdminSayActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(chatOutputScroll, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addComponent(message, javax.swing.GroupLayout.PREFERRED_SIZE, 447, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(chat, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                        .addContainerGap())
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(chatAutoScroll)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(showChat)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(showSay)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(showOpChatCL)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(showCL1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(showCL2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(showAllOtherCls)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(showAdminSay))))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(message, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(chat))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chatOutputScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 401, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(showSay, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(chatAutoScroll)
                        .addComponent(showChat)
                        .addComponent(showCL1)
                        .addComponent(showCL2)
                        .addComponent(showOpChatCL)
                        .addComponent(showAllOtherCls)
                        .addComponent(showAdminSay)))
                .addContainerGap())
        );

        chatAutoScroll.getAccessibleContext().setAccessibleName("chatAutoScroll");

        jTabbedPane1.addTab("Chat", jPanel4);

        jTabbedPane2.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N

        SaveColours.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N
        SaveColours.setText("Save");
        SaveColours.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SaveColoursActionPerformed(evt);
            }
        });

        jLabel10.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N
        jLabel10.setText("Background");

        BB.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N

        BG.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N

        BR.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N

        TB.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N

        TG.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N

        TR.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N

        jLabel9.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N
        jLabel9.setText("Text");

        jLabel11.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N
        jLabel11.setText("Custom (RGB Values)");

        jLabel12.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N
        jLabel12.setText("Presets");

        jButton1.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N
        jButton1.setText("Default");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N
        jButton2.setText("Black / White");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jButton3.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N
        jButton3.setText("High Contrast");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jButton4.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N
        jButton4.setText("Blue / Yellow");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator1, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addComponent(jLabel11)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(SaveColours))
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addComponent(TR, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(TG, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(TB, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(BR, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(BG, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(BB, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addComponent(jLabel9)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel10))
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel12)
                            .addGroup(jPanel7Layout.createSequentialGroup()
                                .addComponent(jButton1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton4)))
                        .addGap(0, 128, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel11)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(jLabel10))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(TR, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(TG, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(TB, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(BB, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(BG, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(BR, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 22, Short.MAX_VALUE)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel12)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(jButton2)
                    .addComponent(jButton3)
                    .addComponent(jButton4))
                .addGap(205, 205, 205)
                .addComponent(SaveColours)
                .addContainerGap())
        );

        jTabbedPane2.addTab("Colours", jPanel7);

        fflabel.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N
        fflabel.setText("Font Face");

        FontFace.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N
        FontFace.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        FontFace.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FontFaceActionPerformed(evt);
            }
        });

        fslabel.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N
        fslabel.setText("Font Size");

        FontSize.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N

        save.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N
        save.setText("Save");
        save.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(fflabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(FontFace, 0, 553, Short.MAX_VALUE))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(fslabel)
                        .addGap(18, 18, 18)
                        .addComponent(FontSize, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addGap(8, 8, 8))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(save)
                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(fflabel)
                    .addComponent(FontFace, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(fslabel)
                    .addComponent(FontSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 335, Short.MAX_VALUE)
                .addComponent(save)
                .addContainerGap())
        );

        jTabbedPane2.addTab("Font Settings", jPanel6);

        chkIgnorePlayerCommands.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N
        chkIgnorePlayerCommands.setSelected(true);
        chkIgnorePlayerCommands.setText("Ignore \"[PLAYER_COMMAND]\" messages");

        chkIgnoreServerCommands.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N
        chkIgnoreServerCommands.setSelected(true);
        chkIgnoreServerCommands.setText("Ignore \"issued server command\" messages");

        chkShowChatOnly.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N
        chkShowChatOnly.setText("Show chat only");
        chkShowChatOnly.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkShowChatOnlyActionPerformed(evt);
            }
        });

        chkIgnoreErrors.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N
        chkIgnoreErrors.setText("Ignore warnings and errors");
        chkIgnoreErrors.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkIgnoreErrorsActionPerformed(evt);
            }
        });

        PluginName.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N

        jLabel5.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N
        jLabel5.setText("Plugin Name ~ Requires Restart");

        saveplugin.setFont(new java.awt.Font("Comic Sans MS", 0, 13)); // NOI18N
        saveplugin.setText("Save");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator2, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(saveplugin))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(chkIgnorePlayerCommands)
                            .addComponent(chkIgnoreServerCommands)
                            .addComponent(chkShowChatOnly)
                            .addComponent(chkIgnoreErrors)
                            .addComponent(PluginName, javax.swing.GroupLayout.PREFERRED_SIZE, 252, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel5))
                        .addGap(0, 333, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(chkIgnorePlayerCommands, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chkIgnoreServerCommands, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chkShowChatOnly, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chkIgnoreErrors, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(PluginName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 203, Short.MAX_VALUE)
                .addComponent(saveplugin)
                .addContainerGap())
        );

        jTabbedPane2.addTab("Misc", jPanel5);

        jTabbedPane1.addTab("Options", jTabbedPane2);

        splitPane.setRightComponent(jTabbedPane1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(splitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 1292, Short.MAX_VALUE)
                .addGap(0, 0, 0))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(splitPane)
                .addGap(0, 0, 0))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    
    private void txtCommandKeyPressed(java.awt.event.KeyEvent evt)//GEN-FIRST:event_txtCommandKeyPressed
    {//GEN-HEADEREND:event_txtCommandKeyPressed
        if (!txtCommand.isEnabled())
        {
            return;
        }
        if (evt.getKeyCode() == KeyEvent.VK_ENTER)
        {
            getConnectionManager().sendCommand(txtCommand.getText());
            txtCommand.selectAll();
        }
    }//GEN-LAST:event_txtCommandKeyPressed
    
    private void btnConnectActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnConnectActionPerformed
    {//GEN-HEADEREND:event_btnConnectActionPerformed
        if (!btnConnect.isEnabled())
        {
            return;
        }
        saveServersAndTriggerConnect();
    }//GEN-LAST:event_btnConnectActionPerformed
    
    private void btnDisconnectActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnDisconnectActionPerformed
    {//GEN-HEADEREND:event_btnDisconnectActionPerformed
        if (!btnDisconnect.isEnabled())
        {
            return;
        }
        getConnectionManager().triggerDisconnect();
    }//GEN-LAST:event_btnDisconnectActionPerformed
    
    private void btnSendActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnSendActionPerformed
    {//GEN-HEADEREND:event_btnSendActionPerformed
        if (!btnSend.isEnabled())
        {
            return;
        }
        getConnectionManager().sendCommand(txtCommand.getText());
        txtCommand.selectAll();
    }//GEN-LAST:event_btnSendActionPerformed
    
    private void txtCommandActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtCommandActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtCommandActionPerformed
    
    private void saveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveActionPerformed
        for (Component c : getAllComponents((Container) this))
        {
            c.setFont(setFontSize());
        }
    }//GEN-LAST:event_saveActionPerformed
    
    private void FontFaceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FontFaceActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_FontFaceActionPerformed
    
    private void chkIgnoreErrorsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkIgnoreErrorsActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_chkIgnoreErrorsActionPerformed
    
    private void chkShowChatOnlyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkShowChatOnlyActionPerformed
        boolean enable = !chkShowChatOnly.isSelected();
        chkIgnorePlayerCommands.setEnabled(enable);
        chkIgnoreServerCommands.setEnabled(enable);
    }//GEN-LAST:event_chkShowChatOnlyActionPerformed
    
    private void showChatActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showChatActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_showChatActionPerformed
    
    private void chatAutoScrollActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chatAutoScrollActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_chatAutoScrollActionPerformed
    
    private void chatActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chatActionPerformed
        getConnectionManager().sendCommand("csay 0 " + message.getText());
        message.selectAll();
        message.requestFocus();
    }//GEN-LAST:event_chatActionPerformed
    
    private void messageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_messageActionPerformed
    }//GEN-LAST:event_messageActionPerformed
    
    private void SaveColoursActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SaveColoursActionPerformed
        Color foreground = new Color((int) TR.getValue(), (int) TG.getValue(), (int) TB.getValue());
        Color background = new Color((int) BR.getValue(), (int) BG.getValue(), (int) BB.getValue());
        this.setColours(foreground, background);
    }//GEN-LAST:event_SaveColoursActionPerformed
    
    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        setColours(Color.BLACK, Color.WHITE);
    }//GEN-LAST:event_jButton1ActionPerformed
    
    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        setColours(Color.WHITE, Color.BLACK);
    }//GEN-LAST:event_jButton2ActionPerformed
    
    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        setColours(Color.GREEN, Color.BLACK);
    }//GEN-LAST:event_jButton3ActionPerformed
    
    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        setColours(Color.YELLOW, Color.BLUE);
    }//GEN-LAST:event_jButton4ActionPerformed
    
    private void showOpChatCLActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showOpChatCLActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_showOpChatCLActionPerformed
    
    private void showCL2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showCL2ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_showCL2ActionPerformed

    private void showAllOtherClsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showAllOtherClsActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_showAllOtherClsActionPerformed

    private void showAdminSayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showAdminSayActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_showAdminSayActionPerformed
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JSpinner BB;
    private javax.swing.JSpinner BG;
    private javax.swing.JSpinner BR;
    private javax.swing.JComboBox FontFace;
    private javax.swing.JTextField FontSize;
    private javax.swing.JTextField PluginName;
    private javax.swing.JButton SaveColours;
    private javax.swing.JSpinner TB;
    private javax.swing.JSpinner TG;
    private javax.swing.JSpinner TR;
    private javax.swing.JButton btnConnect;
    private javax.swing.JButton btnDisconnect;
    private javax.swing.JButton btnSend;
    private javax.swing.JButton chat;
    private javax.swing.JCheckBox chatAutoScroll;
    private javax.swing.JTextPane chatOutput;
    private javax.swing.JScrollPane chatOutputScroll;
    private javax.swing.JCheckBox chkAutoScroll;
    private javax.swing.JCheckBox chkIgnoreErrors;
    private javax.swing.JCheckBox chkIgnorePlayerCommands;
    private javax.swing.JCheckBox chkIgnoreServerCommands;
    private javax.swing.JCheckBox chkShowChatOnly;
    private javax.swing.JPanel favoriteButtonsPanel;
    private javax.swing.JPanel favoriteButtonsPanelHolder;
    private javax.swing.JScrollPane favoriteButtonsPanelScroll;
    private javax.swing.JLabel fflabel;
    private javax.swing.JLabel fslabel;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTabbedPane jTabbedPane2;
    private javax.swing.JTextPane mainOutput;
    private javax.swing.JScrollPane mainOutputScoll;
    private javax.swing.JTextField message;
    private javax.swing.JButton save;
    private javax.swing.JButton saveplugin;
    private javax.swing.JCheckBox showAdminSay;
    private javax.swing.JCheckBox showAllOtherCls;
    private javax.swing.JCheckBox showCL1;
    private javax.swing.JCheckBox showCL2;
    private javax.swing.JCheckBox showChat;
    private javax.swing.JCheckBox showOpChatCL;
    private javax.swing.JCheckBox showSay;
    private javax.swing.JSplitPane splitPane;
    private javax.swing.JTextField txtCommand;
    private javax.swing.JComboBox<ServerEntry> txtServer;
    // End of variables declaration//GEN-END:variables
    
    public javax.swing.JButton getBtnConnect()
    {
        return btnConnect;
    }
    
    public javax.swing.JButton getBtnDisconnect()
    {
        return btnDisconnect;
    }
    
    public javax.swing.JButton getBtnSend()
    {
        return btnSend;
    }
    
    public javax.swing.JTextPane getMainOutput()
    {
        return mainOutput;
    }
    
    public javax.swing.JTextField getTxtCommand()
    {
        return txtCommand;
    }
    
    public javax.swing.JComboBox<ServerEntry> getTxtServer()
    {
        return txtServer;
    }
    
    public JCheckBox getChkAutoScroll()
    {
        return chkAutoScroll;
    }
    
    public JCheckBox getChkIgnorePlayerCommands()
    {
        return chkIgnorePlayerCommands;
    }
    
    public JCheckBox getChkIgnoreServerCommands()
    {
        return chkIgnoreServerCommands;
    }
    
    public JCheckBox getChkShowChatOnly()
    {
        return chkShowChatOnly;
    }
    
    public JCheckBox getChkIgnoreErrors()
    {
        return chkIgnoreErrors;
    }
    
    public Font getFont()
    {
        String name = (String) FontFace.getSelectedItem();
        GraphicsEnvironment e = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Font[] font = e.getAllFonts();
        for(Font f : font)
        {
            if(f.getFontName().equals(name))
            {
                return f;
            }
        }
        return null;
    }
    
    public Font setFontSize()
    {
        Font font = getFont();
        int size = 10;
        try
        {
            size = Integer.parseInt(FontSize.getText());
        }
        catch(NumberFormatException e)
        {
            
        }
        return new Font(font.getFontName(), 0, size);
    }
    
    public List<Component> getAllComponents(Container c)
    {
        Component[] comps = c.getComponents();
        List<Component> compList = new ArrayList<Component>();
        for (Component comp : comps)
        {
            compList.add(comp);
            if (comp instanceof Container)
            {
                compList.addAll(getAllComponents((Container) comp));
            }
        }
        return compList;
    }
    
    public void setColours(Color foreground, Color background)
    {
        for (Component c : this.getAllComponents(this))
        {
            c.setForeground(foreground);
            c.setBackground(background);
        }
    }
    
    public BTC_ConnectionManager getConnectionManager()
    {
        return connectionManager;
    }
}