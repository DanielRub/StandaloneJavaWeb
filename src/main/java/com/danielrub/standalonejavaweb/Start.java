/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.danielrub.standalonejavaweb;

import ch.vorburger.exec.ManagedProcessException;
import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfiguration;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;
import com.wix.mysql.config.MysqldConfig;
import com.wix.mysql.EmbeddedMysql;

import java.util.concurrent.TimeUnit;

import static com.wix.mysql.config.MysqldConfig.aMysqldConfig;
import static com.wix.mysql.EmbeddedMysql.anEmbeddedMysql;
import com.wix.mysql.ScriptResolver;
import static com.wix.mysql.distribution.Version.v5_6_23;
import static com.wix.mysql.config.Charset.UTF8;
import com.wix.mysql.config.DownloadConfig;
import static com.wix.mysql.config.DownloadConfig.aDownloadConfig;
import com.wix.mysql.distribution.Version;
import java.awt.AWTException;
import java.awt.CheckboxMenuItem;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import org.glassfish.embeddable.BootstrapProperties;
import org.glassfish.embeddable.Deployer;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.GlassFishProperties;
import org.glassfish.embeddable.GlassFishRuntime;

/**
 *
 * @author daniel
 */
public class Start {

    static ResourceBundle bundle = ResourceBundle.getBundle("settings");
    static TrayIcon trayIcon = new TrayIcon(createImage(bundle.getString("app.trayicons.path")+"\\tray.gif", bundle.getString("app.name")));

    public static void main(String[] args) {

        /* Use an appropriate Look and Feel */
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
            //UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
        } catch (UnsupportedLookAndFeelException | IllegalAccessException | InstantiationException | ClassNotFoundException ex) {
        }
        /* Turn off metal's use of bold fonts */
        UIManager.put("swing.boldMetal", Boolean.FALSE);
        //Schedule a job for the event-dispatching thread:
        //adding TrayIcon.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                trayIcon.setImageAutoSize(true);
                createAndShowGUI();
                //type = TrayIcon.MessageType.INFO;
                trayIcon.displayMessage(bundle.getString("app.name"),
                        "Application is starting", TrayIcon.MessageType.WARNING);

                try {
                    int MySQLPort = Integer.parseInt(bundle.getString("mysql.port"));
                    String MySQLUser = bundle.getString("mysql.user");
                    String MySQLPassword = bundle.getString("mysql.password");
                    String MySQLDataBase = bundle.getString("mysql.database");

                    DBConfigurationBuilder configBuilder = DBConfigurationBuilder.newBuilder();
                    configBuilder.setPort(MySQLPort); // OR, default: setPort(0); => autom. detect free port
                    configBuilder.setDataDir(bundle.getString("app.installDir") + "\\DBServer");
                    configBuilder.setBaseDir(bundle.getString("app.installDir") + "\\DBServer");
                    DB db = DB.newEmbeddedDB(configBuilder.build());
                    System.out.println("database version = " + configBuilder.getDatabaseVersion());
                    db.start();
                    db.createDB(MySQLDataBase, MySQLUser, MySQLPassword);
                    System.out.println("db has started!");
                    //lets delete previous temp folder is existing
                    File temFolder = new File(bundle.getString("app.installDir") + "\\Glassfish");
                    if (temFolder.exists()) {
                        deleteFolder(temFolder);
                    }
                    GlassFishProperties glassfishProperties = new GlassFishProperties();
                    glassfishProperties.setPort("http-listener", Integer.parseInt(bundle.getString("glassfish.http.port")));
                    glassfishProperties.setPort("https-listener", Integer.parseInt(bundle.getString("glassfish.https.port")));
                    glassfishProperties.setProperty("glassfish.embedded.tmpdir", bundle.getString("app.installDir") + "\\Glassfish");
                    GlassFish glassfish = GlassFishRuntime.bootstrap().newGlassFish(glassfishProperties);
                    glassfish.start();
                    System.out.println("glassfish has started!");
                    Deployer deployer = glassfish.getDeployer();
                    File war = new File(bundle.getString("app.war.path"));
                    deployer.deploy(war, "--name="+bundle.getString("app.context"), "--contextroot="+bundle.getString("app.context"), "--force=true");
                    System.out.println("the app is deployed!");
                    trayIcon.setImage(createImage(bundle.getString("app.trayicons.path")+"\\tray1.gif",  bundle.getString("app.name")));
                    trayIcon.displayMessage(bundle.getString("app.name"),
                            "Application has started", TrayIcon.MessageType.INFO);
                    openWebpage(new URL("http://localhost:" + bundle.getString("glassfish.http.port") + "/" + bundle.getString("app.context")));

                } catch (Exception e) {
                    String logPath = bundle.getString("app.installDir")+"\\error.txt";
                    trayIcon.displayMessage(bundle.getString("app.name"),
                            "Oops. Something wrong happened when starting the app. Error log at : " + logPath, TrayIcon.MessageType.ERROR);
                    try {
                        File file = new File(logPath);
                        FileReader fileReader = new FileReader(file);
                        BufferedReader bufferedReader = new BufferedReader(fileReader);
                        String line = "", content = "";
                        while ((line = bufferedReader.readLine()) != null) {
                            content += line;
                        }
                        FileWriter fileWriter = new FileWriter(file);

                        fileWriter.write(content + " \n \n " + new SimpleDateFormat("dd-MM-yyyy à hh:mm:ss").format(new Date()) + "\n-----------------------------" + " \n " + e.getMessage());
                        fileWriter.close();
                        fileReader.close();
                        bufferedReader.close();
                    } catch (IOException ex) {
                        Logger.getLogger(Start.class.getName()).log(Level.SEVERE, null, ex);
                    }

                }

            }
        });

    }

    public static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) { //some JVMs return null for empty dirs
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }

    protected static Image createImage(String path, String description) {
        
        ImageIcon imageIcon = new ImageIcon(path, description);
        System.out.println("path = " + path);
        
       return imageIcon.getImage();
    }
    //opening the webapp in the browser

    public static void openWebpage(URI uri) {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(uri);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void openWebpage(URL url) {
        try {
            openWebpage(url.toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private static void createAndShowGUI() {
        //Check the SystemTray support
        if (!SystemTray.isSupported()) {
            System.out.println("SystemTray is not supported");
            return;
        }
        final PopupMenu popup = new PopupMenu();

        final SystemTray tray = SystemTray.getSystemTray();

        // Create a popup menu components
        MenuItem HomeItem = new MenuItem("Home page");
        MenuItem aboutItem = new MenuItem("About us");
        CheckboxMenuItem cb1 = new CheckboxMenuItem("Set auto size");
        CheckboxMenuItem cb2 = new CheckboxMenuItem("Set tooltip");
        Menu displayMenu = new Menu("Display");
        MenuItem errorItem = new MenuItem("Error");
        MenuItem warningItem = new MenuItem("Warning");
        MenuItem infoItem = new MenuItem("Info");
        MenuItem noneItem = new MenuItem("None");
        MenuItem exitItem = new MenuItem("Exit");

        //Add components to popup menu
        popup.add(HomeItem);
        popup.addSeparator();
        popup.add(aboutItem);

//        popup.add(cb1);
//        popup.add(cb2);
//        popup.addSeparator();
//        popup.add(displayMenu);
//        displayMenu.add(errorItem);
//        displayMenu.add(warningItem);
//        displayMenu.add(infoItem);
//        displayMenu.add(noneItem);
        popup.add(exitItem);

        trayIcon.setPopupMenu(popup);

        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            System.out.println("TrayIcon could not be added.");
            return;
        }

        trayIcon.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(null, bundle.getString("app.name") + " Is running in your browser, \n "
                        + "Please open Home page");
            }
        });

        aboutItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(null, bundle.getString("app.name") + " version " + bundle.getString("app.version") + " \n"
                        + "Apache License"
                        + "Contribute on github");
            }
        });
        HomeItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                try {
                    openWebpage(new URL("http://localhost:" + bundle.getString("glassfish.http.port") + "/" + bundle.getString("app.context")));
                } catch (MalformedURLException ex) {
                    Logger.getLogger(Start.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        });

        cb1.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                int cb1Id = e.getStateChange();
                if (cb1Id == ItemEvent.SELECTED) {
                    trayIcon.setImageAutoSize(true);
                } else {
                    trayIcon.setImageAutoSize(false);
                }
            }
        });

        cb2.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                int cb2Id = e.getStateChange();
                if (cb2Id == ItemEvent.SELECTED) {
                    trayIcon.setToolTip("Sun TrayIcon");
                } else {
                    trayIcon.setToolTip(null);
                }
            }
        });

        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                MenuItem item = (MenuItem) e.getSource();
                //TrayIcon.MessageType type = null;
                System.out.println(item.getLabel());
                if ("Error".equals(item.getLabel())) {
                    //type = TrayIcon.MessageType.ERROR;
                    trayIcon.displayMessage("Sun TrayIcon Demo",
                            "This is an error message", TrayIcon.MessageType.ERROR);

                } else if ("Warning".equals(item.getLabel())) {
                    //type = TrayIcon.MessageType.WARNING;
                    trayIcon.displayMessage("Sun TrayIcon Demo",
                            "This is a warning message", TrayIcon.MessageType.WARNING);

                } else if ("Info".equals(item.getLabel())) {
                    //type = TrayIcon.MessageType.INFO;
                    trayIcon.displayMessage("Sun TrayIcon Demo",
                            "This is an info message", TrayIcon.MessageType.INFO);

                } else if ("None".equals(item.getLabel())) {
                    //type = TrayIcon.MessageType.NONE;
                    trayIcon.displayMessage("Sun TrayIcon Demo",
                            "This is an ordinary message", TrayIcon.MessageType.NONE);
                }
            }
        };

        errorItem.addActionListener(listener);
        warningItem.addActionListener(listener);
        infoItem.addActionListener(listener);
        noneItem.addActionListener(listener);

        exitItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tray.remove(trayIcon);
                System.exit(0);
            }
        });
    }
}
