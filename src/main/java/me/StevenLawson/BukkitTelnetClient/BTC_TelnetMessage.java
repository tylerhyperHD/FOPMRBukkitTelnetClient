package me.StevenLawson.BukkitTelnetClient;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class BTC_TelnetMessage extends BTC_ConsoleMessage
{
    public static final List<String> RANKORDER = Arrays.asList("Admin", "Super Admin", "Senior Admin", "Executive", "Specialist", "System Admin", "Owner", "Overlord");
    public static final List<String> OVERME = Arrays.asList("Senior Admin", "Executive", "Specialist", "System Admin", "Owner", "Overlord");
    private static final String PATTERN_PREFIX = "^:\\[.+? INFO\\]: ";
    private static final Color PURPLE = new Color(128, 0, 128);
    private static final Color DARK_GREEN = new Color(86, 130, 3);

    private static final Pattern ERROR_MESSAGE = Pattern.compile("^:\\[.+? (?:(WARN)|(ERROR))\\]: ");
    private static final Pattern INFO_MESSAGE = Pattern.compile(PATTERN_PREFIX);

    private final BTC_LogMessageType messageType;

    public BTC_TelnetMessage(String message)
    {
        super(message);
        this.messageType = BTC_LogMessageType.getMessageType(message);
    }

    public BTC_LogMessageType getMessageType()
    {
        return this.messageType;
    }

    public boolean isErrorMessage()
    {
        return ERROR_MESSAGE.matcher(this.getMessage()).find();
    }

    public boolean isInfoMessage()
    {
        return INFO_MESSAGE.matcher(this.getMessage()).find();
    }

    private boolean isType(final BTC_LogMessageType checkType)
    {
        return this.messageType != null ? this.messageType == checkType : false;
    }

    public boolean skip()
    {
        final BTC_MainPanel mainPanel = BukkitTelnetClient.mainPanel;

        if (mainPanel == null)
        {
            return false;
        }

        if (mainPanel.getChkShowChatOnly().isSelected())
        {
            return !isType(BTC_LogMessageType.CHAT_MESSAGE)
                    && !isType(BTC_LogMessageType.OPCHATCL_MESSAGE)
                    && !isType(BTC_LogMessageType.ADMINCHATCL1_MESSAGE)
                    && !isType(BTC_LogMessageType.ADMINCHATCL2_MESSAGE)
                    && !isType(BTC_LogMessageType.ALLOTHERCLS_MESSAGE)
                    && !isType(BTC_LogMessageType.SAY_MESSAGE)
                    && !isType(BTC_LogMessageType.ADMINSAY_MESSAGE);
        }
        
        if (mainPanel.getdisableIdioticStuff1().isSelected() && isType(BTC_LogMessageType.LARGEENTITIES))
        {
            return true;
        }

        if (mainPanel.getdisableIdioticStuff1().isSelected() && isType(BTC_LogMessageType.CHECKSHADE))
        {
            return true;
        }

        if (mainPanel.getChkIgnoreServerCommands().isSelected() && isType(BTC_LogMessageType.ISSUED_SERVER_COMMAND))
        {
            return true;
        }

        if (mainPanel.getChkIgnorePlayerCommands().isSelected() && isType(BTC_LogMessageType.PLAYER_COMMAND))
        {
            return true;
        }

        if (mainPanel.getChkIgnoreErrors().isSelected())
        {
            if (!isType(BTC_LogMessageType.CHAT_MESSAGE)
                    && !isType(BTC_LogMessageType.OPCHATCL_MESSAGE)
                    && !isType(BTC_LogMessageType.ADMINCHATCL1_MESSAGE)
                    && !isType(BTC_LogMessageType.ADMINCHATCL2_MESSAGE)
                    && !isType(BTC_LogMessageType.ALLOTHERCLS_MESSAGE)
                    && !isType(BTC_LogMessageType.SAY_MESSAGE)
                    && !isType(BTC_LogMessageType.ADMINSAY_MESSAGE))
            {
                return false;
            }
        }

        return false;
    }

    @Override
    public Color getColor()
    {
        if (this.messageType == null)
        {
            return super.getColor();
        }
        else
        {
            return this.messageType.getColor();
        }
    }

    public static enum BTC_LogMessageType
    {
        CHAT_MESSAGE(PATTERN_PREFIX + "\\<", Color.BLUE),
        SAY_MESSAGE(PATTERN_PREFIX + "\\[CONSOLE :", DARK_GREEN),
        OPCHATCL_MESSAGE(PATTERN_PREFIX + "\\[Op Chat\\] ", Color.BLACK),
        ADMINCHATCL1_MESSAGE(PATTERN_PREFIX + "\\[Admin Chat\\] ", Color.BLUE),
        ADMINCHATCL2_MESSAGE(PATTERN_PREFIX + "\\[Super Admin Chat\\] ", Color.BLUE),
        ALLOTHERCLS_MESSAGE(PATTERN_PREFIX + "\\[" + OVERME + " Chat\\] ", Color.RED),
        //
        ADMINSAY_MESSAGE(PATTERN_PREFIX + "\\[" + RANKORDER + " :", DARK_GREEN),
        //
        WORLD_EDIT(PATTERN_PREFIX + "WorldEdit: ", Color.RED),
        //
        LARGEENTITIES(PATTERN_PREFIX + "Filtered out large getEntities call"),
        //
        BANSHADED(PATTERN_PREFIX + "com.mojang.authlib.GameProfile", Color.GRAY),  // Future release API needed
        //
        JOINEVENT(PATTERN_PREFIX + "UUID of player", Color.RED), // Future release API needed
        //
        CHECKSHADE(PATTERN_PREFIX + "\\[me -> .+?\\] "),
        //
        LOSTCONNECTION(PATTERN_PREFIX + ".+? left the game.", Color.RED),
        //
        PREPROCESS_COMMAND(PATTERN_PREFIX + "\\[PREPROCESS_COMMAND\\] ", DARK_GREEN),
        //
        ISSUED_SERVER_COMMAND(PATTERN_PREFIX + ".+? issued server command: "),
        PLAYER_COMMAND(PATTERN_PREFIX + "\\[PLAYER_COMMAND\\] ");

        private final Pattern messagePattern;
        private final Color color;

        private BTC_LogMessageType(final String messagePatternStr)
        {
            this.messagePattern = Pattern.compile(messagePatternStr);
            this.color = Color.BLACK;
        }

        private BTC_LogMessageType(final String messagePatternStr, final Color color)
        {
            this.messagePattern = Pattern.compile(messagePatternStr);
            this.color = color;
        }

        public Pattern getMessagePattern()
        {
            return this.messagePattern;
        }

        public Color getColor()
        {
            return this.color;
        }

        public static BTC_LogMessageType getMessageType(final String message)
        {
            for (final BTC_LogMessageType type : values())
            {
                if (type.getMessagePattern().matcher(message).find())
                {
                    return type;
                }
            }
            return null;
        }
    }
}
