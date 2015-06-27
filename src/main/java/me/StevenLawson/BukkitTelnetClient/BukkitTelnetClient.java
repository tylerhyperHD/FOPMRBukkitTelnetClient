package me.StevenLawson.BukkitTelnetClient;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.UnsupportedLookAndFeelException;

public class BukkitTelnetClient
{
    public static final String VERSION_STRING = "vC2.1.7-FOPMR";
    public static final Logger LOGGER = Logger.getLogger(BukkitTelnetClient.class.getName());
    public static BTC_MainPanel mainPanel = null;
    public static BTC_ConfigLoader config = new BTC_ConfigLoader();

    public static void main(String args[])
    {
        config.load(true);

        findAndSetLookAndFeel("Windows");

        java.awt.EventQueue.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                mainPanel = new BTC_MainPanel();
                mainPanel.setup();
            }
        });
    }

    private static void findAndSetLookAndFeel(final String searchStyleName)
    {
        try
        {
            javax.swing.UIManager.LookAndFeelInfo foundStyle = null;
            javax.swing.UIManager.LookAndFeelInfo fallbackStyle = null;

            for (javax.swing.UIManager.LookAndFeelInfo style : javax.swing.UIManager.getInstalledLookAndFeels())
            {
                if (searchStyleName.equalsIgnoreCase(style.getName()))
                {
                    foundStyle = style;
                    break;
                }
                else if ("Nimbus".equalsIgnoreCase(style.getName()))
                {
                    fallbackStyle = style;
                }
            }

            if (foundStyle != null)
            {
                javax.swing.UIManager.setLookAndFeel(foundStyle.getClassName());
            }
            else if (fallbackStyle != null)
            {
                javax.swing.UIManager.setLookAndFeel(fallbackStyle.getClassName());
            }
        }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex)
        {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }
    
   // JDK 7 safe getDeclaredAnnotation
    public static <T extends Annotation> T getDeclaredAnnotation(final Method method, final Class<T> annotationClass)
    {
        java.util.Objects.requireNonNull(annotationClass);

        T annotation = null;

        for (final Annotation _annotation : method.getDeclaredAnnotations())
        {
            if (_annotation.annotationType() == annotationClass)
            {
                annotation = annotationClass.cast(_annotation);
                break;
            }
        }

        return annotation;
    }
}
