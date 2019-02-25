/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.danielrub.standalonejavaweb;

import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;
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
import java.io.File;
import java.io.FileWriter;
import static java.lang.Thread.sleep;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import org.glassfish.embeddable.Deployer;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishProperties;
import org.glassfish.embeddable.GlassFishRuntime;

/**
 *
 * @author daniel
 */
public class Start {
    
    static ResourceBundle bundle = ResourceBundle.getBundle("settings");
    static TrayIcon trayIcon = new TrayIcon(new ImageIcon(Start.class.getResource("/images/yellow.gif")).getImage());
    static boolean DbhasStarted = false;
    static boolean glassfishHasStarted = false;
    static DB db = null;
    static GlassFish glassfish = null;
    static Splash splash = null;
    
    public static void main(String[] args) {
        if (!DbhasStarted | !glassfishHasStarted) {
            splash = new Splash();
            splash.getjLApplicationName().setText(bundle.getString("app.name"));
            splash.getjLApplicationVersion().setText("Ver " + bundle.getString("app.version"));
            splash.setVisible(true);
            try {
                sleep(100);
            } catch (InterruptedException ex) {
                Logger.getLogger(Splash.class.getName()).log(Level.SEVERE, null, ex);
            }
            updateProgressBar("Loading components", 5);
            
            File logFolder = new File(bundle.getString("app.logs.path")), updateAppFolder = new File(bundle.getString("app.updates.path") + "\\app"), updateDbFolder = new File(bundle.getString("app.updates.path") + "\\db");
            logFolder.mkdirs();
            updateAppFolder.mkdirs();
            updateDbFolder.mkdirs();

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

            trayIcon.setImageAutoSize(true);
            createAndShowGUI();
            //type = TrayIcon.MessageType.INFO;
            // trayIcon.displayMessage(bundle.getString("app.name"),
            //        "Application is starting", TrayIcon.MessageType.WARNING);
            //JOptionPane.showMessageDialog(null, "Application is Starting...");
            updateProgressBar("starting embedded servers", 7);
            try {
                if (!DbhasStarted) {
                    int MySQLPort = Integer.parseInt(bundle.getString("mysql.port"));
                    String MySQLUser = bundle.getString("mysql.user");
                    String MySQLPassword = bundle.getString("mysql.password");
                    String MySQLDataBase = bundle.getString("mysql.database");
                    
                    DBConfigurationBuilder configBuilder = DBConfigurationBuilder.newBuilder();
                    configBuilder.setPort(MySQLPort); // OR, default: setPort(0); => autom. detect free port
                    configBuilder.setDataDir(bundle.getString("app.installDir") + "\\DBServer");
                    configBuilder.setBaseDir(bundle.getString("app.installDir") + "\\DBServer");
                    db = DB.newEmbeddedDB(configBuilder.build());
                    updateProgressBar("Mdb files load and start", 9);
                    db.start();
                    
                    updateProgressBar("database has been launched", 15);
                    db.createDB(MySQLDataBase, MySQLUser, MySQLPassword);
                    updateProgressBar("default database created", 20);
                    //JOptionPane.showMessageDialog(null, "Database has started");
                    System.out.println("db has started!");
                    DbhasStarted = true;
                    if (updateDbFolder.listFiles().length > 0) {
                        File dbScriptUpdated = updateDbFolder.listFiles()[0];
                    }
                }
                
                if (!glassfishHasStarted) {
                    //lets delete previous temp folder is existing
                    File temFolder = new File(bundle.getString("app.installDir") + "\\Glassfish");
                    if (temFolder.exists()) {
                        updateProgressBar("deleting appServ temp files", 23);
                        deleteFolder(temFolder);
                        updateProgressBar("AppServ temp files deleted", 25);
                    }
                    GlassFishProperties glassfishProperties = new GlassFishProperties();
                    glassfishProperties.setPort("http-listener", Integer.parseInt(bundle.getString("glassfish.http.port")));
                    glassfishProperties.setPort("https-listener", Integer.parseInt(bundle.getString("glassfish.https.port")));
                    glassfishProperties.setProperty("glassfish.embedded.tmpdir", bundle.getString("app.installDir") + "\\Glassfish");
                    glassfish = GlassFishRuntime.bootstrap().newGlassFish(glassfishProperties);
                    updateProgressBar("starting embedded AppServ", 35);
                    glassfish.start();
                    updateProgressBar("embedded AppServer has started", 50);
                    //JOptionPane.showMessageDialog(null, "Glassfish has started");
                    System.out.println("glassfish has started!");
                    Deployer deployer = glassfish.getDeployer();
                    boolean warUpdateExecuted = false;
                    if (updateAppFolder.listFiles().length > 0) {
                        File warUpdated = updateAppFolder.listFiles()[0];
                        if (warUpdated.getName().endsWith("_deployed.war")) {
                            updateProgressBar("deploying java web app update", 55);
                            deployer.deploy(warUpdated, "--force=true", "--contextroot=" + bundle.getString("app.war.name").replace(".war", ""), "--name=" + bundle.getString("app.war.name").replace(".war", ""));
                            updateProgressBar("App deployment step ended", 70);
                            warUpdateExecuted = true;
                        } else if (warUpdated.getName().endsWith(".war")) {
                            updateProgressBar("deploying java web app update", 55);
                            deployer.deploy(warUpdated, "--force=true", "--contextroot=" + bundle.getString("app.war.name").replace(".war", ""), "--name=" + bundle.getString("app.war.name").replace(".war", ""));
                            warUpdated.renameTo(new File(warUpdated.getPath().replace(".war", "_deployed.war")));
                            updateProgressBar("App updates deployment step ended", 70);
                            warUpdateExecuted = true;
                        }
                    }                    
                    if (!warUpdateExecuted) {
                        URI appURI = Start.class.getResource("/apps/" + bundle.getString("app.war.name")).toURI();
                        // JOptionPane.showMessageDialog(null, "file exist :" + appPath.exists() + " is directory :" + appPath.isDirectory());
                        updateProgressBar("deploying java web app", 55);
                        deployer.deploy(appURI, "--force=true", "--contextroot=" + bundle.getString("app.war.name").replace(".war", ""), "--name=" + bundle.getString("app.war.name").replace(".war", ""));
                        updateProgressBar("App deployment step ended", 70);                        
                    }
                    
                    trayIcon.setImage(new ImageIcon(Start.class.getResource("/images/green.gif")).getImage());
                    // trayIcon.displayMessage(bundle.getString("app.name"),
                    //        "Application has started", TrayIcon.MessageType.INFO);
                    glassfishHasStarted = true;
                    updateProgressBar("All configes has been loaded", 80);
                    
                }
                for (int i = 0; i < 20; i++) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Start.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    updateProgressBar("Lauching default browser", 81 + i);
                }
                openWebpage(new URL("http://localhost:" + bundle.getString("glassfish.http.port") + "/" + bundle.getString("app.war.name").replace(".war", "")));
                splash.setVisible(false);
            } catch (Exception e) {
                //String appLogPath = bundle.getString("app.logs.path") + "\\error.txt";
                JOptionPane.showMessageDialog(null, "Oops! Something wrong happened while performing the task");
                //trayIcon.displayMessage(bundle.getString("app.name"),
                //        "Oops. Something wrong happened when starting the app. Error log at : " + appLogPath, TrayIcon.MessageType.ERROR);

                e.printStackTrace();
                writeToLog(e);
                splash.setVisible(false);
                System.exit(0);
            }
            
        } else {
            try {
                openWebpage(new URL("http://localhost:" + bundle.getString("glassfish.http.port") + "/" + bundle.getString("app.war.name").replace(".war", "")));
            } catch (MalformedURLException e) {
                JOptionPane.showMessageDialog(null, "Oops! Something wrong happened while performing the task");
                writeToLog(e);
                splash.setVisible(false);
                
            }
            
        }
    }
    
    public static void updateProgressBar(String text, int a) {
        splash.getJlabelLoadingText().setText(text);
        splash.getjProgressBar1().setValue(a);
    }
    
    public static void writeToLog(Exception e) {
        if (null != e) {
            String appLogPath = bundle.getString("app.logs.path") + "\\error.txt";
            FileWriter fileWriter = null;
            try {
                File file = new File(appLogPath);
                file.createNewFile();
                String error = e.getMessage();
                try {
                    error += "\n\n" + e.getCause().toString();
                    error += "\n\n" + Arrays.toString(e.getCause().getStackTrace());
                    error += "\n\n" + e.getLocalizedMessage();
                } catch (Exception edd) {
                    edd.printStackTrace();
                }
                
                fileWriter = new FileWriter(file);
                fileWriter.write("\n " + new SimpleDateFormat("dd-MM-yyyy hh:mm:ss").format(new Date()) + "\n ----------------" + " \n " + error);
                fileWriter.close();
            } catch (Exception ex) {
                Logger.getLogger(Start.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
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
                    openWebpage(new URL("http://localhost:" + bundle.getString("glassfish.http.port") + "/" + bundle.getString("app.war.name").replace(".war", "")));
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
                try {
                    glassfish.stop();
                    db.stop();
                    splash.dispose();
                } catch (Exception ex) {
                    writeToLog(ex);
                }
                System.exit(0);
            }
        });
    }
}
