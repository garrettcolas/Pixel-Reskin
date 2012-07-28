/*
 * PixelReskinView.java
 */

package pixelreskin;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import javax.imageio.ImageIO;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;

/**
 * The application's main frame.
 */
public class PixelReskinView extends FrameView {
    
    String path = "";
    Color newColor;
    Color newColor1;
    Color newColor2;
    Color newColor3;
    Color newColor4;
    Color newColor5;
    Color newColor6;
    Color newColor7;
    
    Color oldColor;
    Color oldColor1;
    Color oldColor2;
    Color oldColor3;
    Color oldColor4;
    Color oldColor5;
    Color oldColor6;
    Color oldColor7;
    
    ImageIcon imageFrom;
    BufferedImage imageStart;
    ImageIcon imageTo;
    BufferedImage imageEnd;
    
    //Used to store the color palete
    ImageIcon imageColors;
    BufferedImage imageColorsBuff;
    
    public ArrayList<String> pathList = new ArrayList<String>();
    public ArrayList<String> fileList = new ArrayList<String>();
    public ArrayList<Integer> colorList = new ArrayList<Integer>();
    
    int rgbOldColor;
    int rgbOldColor1;
    int rgbOldColor2;
    int rgbOldColor3;
    int rgbOldColor4;
    int rgbOldColor5;
    int rgbOldColor6;
    int rgbOldColor7;
    
    int rgbNewColor;
    int rgbNewColor1;
    int rgbNewColor2;
    int rgbNewColor3;
    int rgbNewColor4;
    int rgbNewColor5;
    int rgbNewColor6;
    int rgbNewColor7;
    
    public PixelReskinView(SingleFrameApplication app) {
        
        // <editor-fold defaultstate="collapsed" desc="init and status bar code">
        super(app);

        initComponents();

        // status bar initialization - message timeout, idle icon and busy animation, etc
        ResourceMap resourceMap = getResourceMap();
        int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
        messageTimer = new Timer(messageTimeout, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                statusMessageLabel.setText("");
            }
        });
        messageTimer.setRepeats(false);
        int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
        for (int i = 0; i < busyIcons.length; i++) {
            busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
        }
        busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
                statusAnimationLabel.setIcon(busyIcons[busyIconIndex]);
            }
        });
        idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
        statusAnimationLabel.setIcon(idleIcon);
        progressBar.setVisible(false);

        // connecting action tasks to status bar via TaskMonitor
        TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
        taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if ("started".equals(propertyName)) {
                    if (!busyIconTimer.isRunning()) {
                        statusAnimationLabel.setIcon(busyIcons[0]);
                        busyIconIndex = 0;
                        busyIconTimer.start();
                    }
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                } else if ("done".equals(propertyName)) {
                    busyIconTimer.stop();
                    statusAnimationLabel.setIcon(idleIcon);
                    progressBar.setVisible(false);
                    progressBar.setValue(0);
                } else if ("message".equals(propertyName)) {
                    String text = (String)(evt.getNewValue());
                    statusMessageLabel.setText((text == null) ? "" : text);
                    messageTimer.restart();
                } else if ("progress".equals(propertyName)) {
                    int value = (Integer)(evt.getNewValue());
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(value);
                }
            }
        });
        // </editor-fold>
        
        
    }

    //This will iterate recursivly through every png in this folder and all 
    //child folders.
    public void listFiles(String path) {
        String files;
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();

        for (int i = 0; i < listOfFiles.length; i++) {

            if (listOfFiles[i].isFile()) {
                files = listOfFiles[i].getName();
                if (files.endsWith(".png") || files.endsWith(".PNG")
                        || files.endsWith(".gif") || files.endsWith(".GIF")) {
                    //System.out.println(files);
                    pathList.add(path+"\\");
                    fileList.add(files);
                }
            }
            
            else {
                listFiles(path+"\\"+listOfFiles[i].getName());
            }
        }
    }
    public void getAllColors() {
        ImageIcon oldTemp;
        BufferedImage old;
        int tempColor;
        int i = 0;
        //load and convert image
        oldTemp = new ImageIcon(pathList.get(i) + fileList.get(i));
        old = toBufferedImage(oldTemp.getImage());

        //go through the image to find all the colors
        int x = 0;
        int y = 0;
        while (x < oldTemp.getIconWidth()) {
            while (y < oldTemp.getIconHeight()) {
                tempColor = old.getRGB(x, y);
                if (!checkForColor(tempColor)) {
                    colorList.add(tempColor);
                }
                y++;
            }
            y = 0;
            x++;
        }
        Collections.sort(colorList);
        System.out.println("Found " + colorList.size() + " colors.");

        //Generate a pallete of the colors.
        imageColorsBuff = new BufferedImage(colorList.size() * 15, 30, BufferedImage.TYPE_INT_ARGB);
        imageColors = new ImageIcon(toImage(imageColorsBuff));
        x = 0;
        y = 0;
        int pCount = 0;
        int currentColor = 0;
        while (x < imageColors.getIconWidth()) {
            while (y < imageColors.getIconHeight()) {
                imageColorsBuff.setRGB(x, y, colorList.get(currentColor));
                y++;
            }
            y = 0;
            if (pCount >= 15) {
                pCount = 0;
                currentColor++;
            }
            x++;
            pCount++;
        }
        imageColors = new ImageIcon(toImage(imageColorsBuff));
        colorsLabel.setIcon(imageColors);
        
    }
    public void editFiles() {
        ImageIcon oldTemp;
        BufferedImage old;

        int i = 0;
        while(i<pathList.size()) {
            //load and convert image
            oldTemp = new ImageIcon(pathList.get(i)+fileList.get(i));
            old = toBufferedImage(oldTemp.getImage());
            
            //System.out.println("````````````````````````````````````````");
            //System.out.println("Image: " + i);
            //System.out.println("Path: " + pathList.get(i)+fileList.get(i));
            //System.out.println("Image Demensions: " + oldTemp.getIconWidth() +"vs"+oldTemp.getIconHeight());
            //loop through and convert the image
            int x = 0;
            int y = 0;
            while(x < oldTemp.getIconWidth()) {
                while(y < oldTemp.getIconHeight()) {       

                    
                    //System.out.println(old.getRGB(x, y) + " vs " + rgbOldColor + " vs " + imageStart.getRGB(x, y));
                    if(old.getRGB(x, y) == rgbOldColor) {
                        old.setRGB(x, y, rgbNewColor);
                        //System.out.println("Match");
                    }
                    else if(old.getRGB(x, y) == rgbOldColor1) {
                        old.setRGB(x, y, rgbNewColor1);
                        //System.out.println("Match");
                    }
                    else if(old.getRGB(x, y) == rgbOldColor2) {
                        old.setRGB(x, y, rgbNewColor2);
                        //System.out.println("Match");
                    }
                    else if(old.getRGB(x, y) == rgbOldColor3) {
                        old.setRGB(x, y, rgbNewColor3);
                        //System.out.println("Match");
                    }
                    else if(old.getRGB(x, y) == rgbOldColor4) {
                        old.setRGB(x, y, rgbNewColor4);
                        //System.out.println("Match");
                    }
                    else if(old.getRGB(x, y) == rgbOldColor5) {
                        old.setRGB(x, y, rgbNewColor5);
                        //System.out.println("Match");
                    }
                    else if(old.getRGB(x, y) == rgbOldColor6) {
                        old.setRGB(x, y, rgbNewColor6);
                        //System.out.println("Match");
                    }
                    else if(old.getRGB(x, y) == rgbOldColor7) {
                        old.setRGB(x, y, rgbNewColor7);
                        //System.out.println("Match");
                    }
                    //System.out.println("Finished a y line");
                    y++;
                }
                y=0;
                x++;
                //System.out.println("Finished a x line");
            }
            //save the image
            boolean temp = (new File(pathList.get(0)+"edit\\")).mkdirs();
            File outputfile = new File(pathList.get(0)+"edit\\"+fileList.get(i));
            
            try {
                ImageIO.write(old, "png", outputfile);
            } catch (IOException ex) {
                Logger.getLogger(PixelReskinView.class.getName()).log(Level.SEVERE, null, ex);
            }
            i++;
        }
        
    }
    
    public boolean checkForColor(int color) {
        int i = 0;
        while(i < colorList.size()) {
            if(color == colorList.get(i)) {
                return true;
            }
            i++;
        }
        return false;
    }
    @Action
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = PixelReskinApp.getApplication().getMainFrame();
            aboutBox = new PixelReskinAboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        PixelReskinApp.getApplication().show(aboutBox);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jFileChooser1 = new javax.swing.JFileChooser();
        jColorChooser1 = new javax.swing.JColorChooser();
        jPanel2 = new javax.swing.JPanel();
        image1Label = new javax.swing.JLabel();
        colorsLabel = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        pathTextField = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        colorChangedTo = new javax.swing.JTextField();
        colorChangedFrom = new javax.swing.JTextField();
        startButton = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        colorChangedTo1 = new javax.swing.JTextField();
        colorChangedFrom1 = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        colorChangedTo2 = new javax.swing.JTextField();
        colorChangedFrom2 = new javax.swing.JTextField();
        jComboBox1 = new javax.swing.JComboBox();
        colorChangedTo3 = new javax.swing.JTextField();
        colorChangedFrom3 = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        colorChangedTo4 = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        colorChangedFrom4 = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        colorChangedFrom5 = new javax.swing.JTextField();
        jLabel13 = new javax.swing.JLabel();
        colorChangedTo5 = new javax.swing.JTextField();
        colorChangedFrom6 = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        colorChangedTo6 = new javax.swing.JTextField();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        colorChangedFrom7 = new javax.swing.JTextField();
        jLabel17 = new javax.swing.JLabel();
        colorChangedTo7 = new javax.swing.JTextField();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();

        mainPanel.setName("mainPanel"); // NOI18N

        jTabbedPane1.setName("jTabbedPane1"); // NOI18N
        jTabbedPane1.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                jTabbedPane1MouseMoved(evt);
            }
        });

        jPanel1.setName("jPanel1"); // NOI18N

        jFileChooser1.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);
        jFileChooser1.setName("jFileChooser1"); // NOI18N
        jFileChooser1.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                jFileChooser1PropertyChange(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jFileChooser1, javax.swing.GroupLayout.DEFAULT_SIZE, 945, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jFileChooser1, javax.swing.GroupLayout.DEFAULT_SIZE, 392, Short.MAX_VALUE)
                .addContainerGap())
        );

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(pixelreskin.PixelReskinApp.class).getContext().getResourceMap(PixelReskinView.class);
        jTabbedPane1.addTab(resourceMap.getString("jPanel1.TabConstraints.tabTitle"), jPanel1); // NOI18N

        jColorChooser1.setName("jColorChooser1"); // NOI18N
        jTabbedPane1.addTab(resourceMap.getString("jColorChooser1.TabConstraints.tabTitle"), jColorChooser1); // NOI18N

        jPanel2.setName("jPanel2"); // NOI18N

        image1Label.setText(resourceMap.getString("image1Label.text")); // NOI18N
        image1Label.setName("image1Label"); // NOI18N
        image1Label.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                image1LabelMouseClicked(evt);
            }
        });

        colorsLabel.setText(resourceMap.getString("colorsLabel.text")); // NOI18N
        colorsLabel.setName("colorsLabel"); // NOI18N
        colorsLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                colorsLabelMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(image1Label)
                    .addComponent(colorsLabel))
                .addContainerGap(884, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(colorsLabel)
                .addGap(66, 66, 66)
                .addComponent(image1Label)
                .addContainerGap(309, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab(resourceMap.getString("jPanel2.TabConstraints.tabTitle"), jPanel2); // NOI18N

        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N

        pathTextField.setEditable(false);
        pathTextField.setText(resourceMap.getString("pathTextField.text")); // NOI18N
        pathTextField.setName("pathTextField"); // NOI18N

        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N

        jLabel3.setText(resourceMap.getString("jLabel3.text")); // NOI18N
        jLabel3.setName("jLabel3"); // NOI18N

        colorChangedTo.setBackground(jColorChooser1.getColor());
        colorChangedTo.setText(resourceMap.getString("colorChangedTo.text")); // NOI18N
        colorChangedTo.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        colorChangedTo.setDoubleBuffered(true);
        colorChangedTo.setName("colorChangedTo"); // NOI18N

        colorChangedFrom.setEditable(false);
        colorChangedFrom.setText(resourceMap.getString("colorChangedFrom.text")); // NOI18N
        colorChangedFrom.setName("colorChangedFrom"); // NOI18N

        startButton.setText(resourceMap.getString("startButton.text")); // NOI18N
        startButton.setName("startButton"); // NOI18N
        startButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startButtonActionPerformed(evt);
            }
        });

        jLabel4.setText(resourceMap.getString("jLabel4.text")); // NOI18N
        jLabel4.setName("jLabel4"); // NOI18N

        jLabel5.setText(resourceMap.getString("jLabel5.text")); // NOI18N
        jLabel5.setName("jLabel5"); // NOI18N

        colorChangedTo1.setBackground(jColorChooser1.getColor());
        colorChangedTo1.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        colorChangedTo1.setDoubleBuffered(true);
        colorChangedTo1.setName("colorChangedTo1"); // NOI18N

        colorChangedFrom1.setEditable(false);
        colorChangedFrom1.setName("colorChangedFrom1"); // NOI18N

        jLabel6.setText(resourceMap.getString("jLabel6.text")); // NOI18N
        jLabel6.setName("jLabel6"); // NOI18N

        jLabel7.setText(resourceMap.getString("jLabel7.text")); // NOI18N
        jLabel7.setName("jLabel7"); // NOI18N

        colorChangedTo2.setBackground(jColorChooser1.getColor());
        colorChangedTo2.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        colorChangedTo2.setDoubleBuffered(true);
        colorChangedTo2.setName("colorChangedTo2"); // NOI18N

        colorChangedFrom2.setEditable(false);
        colorChangedFrom2.setName("colorChangedFrom2"); // NOI18N

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Color 1", "Color 2", "Color 3", "Color 4", "Color 5", "Color 6", "Color 7", "Color 8" }));
        jComboBox1.setName("jComboBox1"); // NOI18N

        colorChangedTo3.setBackground(jColorChooser1.getColor());
        colorChangedTo3.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        colorChangedTo3.setDoubleBuffered(true);
        colorChangedTo3.setName("colorChangedTo3"); // NOI18N

        colorChangedFrom3.setEditable(false);
        colorChangedFrom3.setName("colorChangedFrom3"); // NOI18N

        jLabel8.setText(resourceMap.getString("jLabel8.text")); // NOI18N
        jLabel8.setName("jLabel8"); // NOI18N

        jLabel9.setText(resourceMap.getString("jLabel9.text")); // NOI18N
        jLabel9.setName("jLabel9"); // NOI18N

        colorChangedTo4.setBackground(jColorChooser1.getColor());
        colorChangedTo4.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        colorChangedTo4.setDoubleBuffered(true);
        colorChangedTo4.setName("colorChangedTo4"); // NOI18N

        jLabel10.setText(resourceMap.getString("jLabel10.text")); // NOI18N
        jLabel10.setName("jLabel10"); // NOI18N

        colorChangedFrom4.setEditable(false);
        colorChangedFrom4.setName("colorChangedFrom4"); // NOI18N

        jLabel11.setText(resourceMap.getString("jLabel11.text")); // NOI18N
        jLabel11.setName("jLabel11"); // NOI18N

        jLabel12.setText(resourceMap.getString("jLabel12.text")); // NOI18N
        jLabel12.setName("jLabel12"); // NOI18N

        colorChangedFrom5.setEditable(false);
        colorChangedFrom5.setName("colorChangedFrom5"); // NOI18N

        jLabel13.setText(resourceMap.getString("jLabel13.text")); // NOI18N
        jLabel13.setName("jLabel13"); // NOI18N

        colorChangedTo5.setBackground(jColorChooser1.getColor());
        colorChangedTo5.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        colorChangedTo5.setDoubleBuffered(true);
        colorChangedTo5.setName("colorChangedTo5"); // NOI18N

        colorChangedFrom6.setEditable(false);
        colorChangedFrom6.setName("colorChangedFrom6"); // NOI18N

        jLabel14.setText(resourceMap.getString("jLabel14.text")); // NOI18N
        jLabel14.setName("jLabel14"); // NOI18N

        colorChangedTo6.setBackground(jColorChooser1.getColor());
        colorChangedTo6.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        colorChangedTo6.setDoubleBuffered(true);
        colorChangedTo6.setName("colorChangedTo6"); // NOI18N

        jLabel15.setText(resourceMap.getString("jLabel15.text")); // NOI18N
        jLabel15.setName("jLabel15"); // NOI18N

        jLabel16.setText(resourceMap.getString("jLabel16.text")); // NOI18N
        jLabel16.setName("jLabel16"); // NOI18N

        colorChangedFrom7.setEditable(false);
        colorChangedFrom7.setName("colorChangedFrom7"); // NOI18N

        jLabel17.setText(resourceMap.getString("jLabel17.text")); // NOI18N
        jLabel17.setName("jLabel17"); // NOI18N

        colorChangedTo7.setBackground(jColorChooser1.getColor());
        colorChangedTo7.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        colorChangedTo7.setDoubleBuffered(true);
        colorChangedTo7.setName("colorChangedTo7"); // NOI18N

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(mainPanelLayout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 108, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel17)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(colorChangedFrom7, javax.swing.GroupLayout.PREFERRED_SIZE, 207, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel16, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(colorChangedTo7, javax.swing.GroupLayout.PREFERRED_SIZE, 215, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addGroup(mainPanelLayout.createSequentialGroup()
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 108, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel15)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(colorChangedFrom6, javax.swing.GroupLayout.PREFERRED_SIZE, 207, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(jLabel14, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(colorChangedTo6, javax.swing.GroupLayout.PREFERRED_SIZE, 215, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addGroup(mainPanelLayout.createSequentialGroup()
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 108, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel13)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(colorChangedFrom5, javax.swing.GroupLayout.PREFERRED_SIZE, 207, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jLabel12, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(colorChangedTo5, javax.swing.GroupLayout.PREFERRED_SIZE, 215, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                            .addGroup(mainPanelLayout.createSequentialGroup()
                                                .addComponent(jLabel1)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(pathTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 548, javax.swing.GroupLayout.PREFERRED_SIZE))
                                            .addGroup(mainPanelLayout.createSequentialGroup()
                                                .addComponent(jComboBox1, 0, 98, Short.MAX_VALUE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(jLabel2)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(colorChangedFrom, javax.swing.GroupLayout.PREFERRED_SIZE, 207, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(colorChangedTo, javax.swing.GroupLayout.PREFERRED_SIZE, 215, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                        .addGroup(mainPanelLayout.createSequentialGroup()
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 108, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                .addGroup(mainPanelLayout.createSequentialGroup()
                                                    .addComponent(jLabel4)
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                    .addComponent(colorChangedFrom1, javax.swing.GroupLayout.PREFERRED_SIZE, 207, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                    .addComponent(colorChangedTo1, javax.swing.GroupLayout.PREFERRED_SIZE, 215, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addGroup(mainPanelLayout.createSequentialGroup()
                                                    .addComponent(jLabel10)
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                    .addComponent(colorChangedFrom4, javax.swing.GroupLayout.PREFERRED_SIZE, 207, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                    .addComponent(jLabel11, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                    .addComponent(colorChangedTo4, javax.swing.GroupLayout.PREFERRED_SIZE, 215, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addGroup(mainPanelLayout.createSequentialGroup()
                                                    .addComponent(jLabel9)
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                    .addComponent(colorChangedFrom3, javax.swing.GroupLayout.PREFERRED_SIZE, 207, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                    .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                    .addComponent(colorChangedTo3, javax.swing.GroupLayout.PREFERRED_SIZE, 215, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addGroup(mainPanelLayout.createSequentialGroup()
                                                    .addComponent(jLabel6)
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                    .addComponent(colorChangedFrom2, javax.swing.GroupLayout.PREFERRED_SIZE, 207, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                    .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                    .addComponent(colorChangedTo2, javax.swing.GroupLayout.PREFERRED_SIZE, 215, javax.swing.GroupLayout.PREFERRED_SIZE))))))))
                        .addGap(108, 108, 108)
                        .addComponent(startButton, javax.swing.GroupLayout.PREFERRED_SIZE, 176, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 970, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(25, 25, 25))))
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 442, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addGap(18, 162, Short.MAX_VALUE)
                        .addComponent(startButton, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addGap(5, 5, 5)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel1)
                            .addComponent(pathTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jComboBox1)
                            .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel2)
                                .addComponent(colorChangedFrom)
                                .addComponent(jLabel3)
                                .addComponent(colorChangedTo)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel4)
                            .addComponent(colorChangedFrom1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel5)
                            .addComponent(colorChangedTo1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel6)
                            .addComponent(colorChangedFrom2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel7)
                            .addComponent(colorChangedTo2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel9)
                            .addComponent(colorChangedFrom3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel8)
                            .addComponent(colorChangedTo3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel10)
                            .addComponent(colorChangedFrom4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel11)
                            .addComponent(colorChangedTo4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel13)
                            .addComponent(colorChangedFrom5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel12)
                            .addComponent(colorChangedTo5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel15)
                            .addComponent(colorChangedFrom6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel14)
                            .addComponent(colorChangedTo6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel17)
                            .addComponent(colorChangedFrom7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel16)
                            .addComponent(colorChangedTo7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap())))
        );

        colorChangedTo.getAccessibleContext().setAccessibleParent(jColorChooser1);

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(pixelreskin.PixelReskinApp.class).getContext().getActionMap(PixelReskinView.class, this);
        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        statusPanel.setName("statusPanel"); // NOI18N

        statusPanelSeparator.setName("statusPanelSeparator"); // NOI18N

        statusMessageLabel.setName("statusMessageLabel"); // NOI18N

        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

        progressBar.setName("progressBar"); // NOI18N

        javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statusPanelSeparator, javax.swing.GroupLayout.DEFAULT_SIZE, 1005, Short.MAX_VALUE)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusMessageLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 835, Short.MAX_VALUE)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusAnimationLabel)
                .addContainerGap())
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addComponent(statusPanelSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(statusMessageLabel)
                    .addComponent(statusAnimationLabel)
                    .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(3, 3, 3))
        );

        setComponent(mainPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);
    }// </editor-fold>//GEN-END:initComponents

    private void jFileChooser1PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_jFileChooser1PropertyChange
        // TODO add your handling code here:
        System.out.println("----------------------------------------------------");
        path = ""+ jFileChooser1.getSelectedFile();
        pathTextField.setText(path);
        pathTextField.setToolTipText(path);
        pathList.clear();
        fileList.clear();
        if(path.length() > 5) {          
            listFiles(path);
            System.out.println("Total Files: "+pathList.size());
        }
        if(pathList.size() > 0) {
            //make our image icon from the first image
            imageFrom = new ImageIcon(pathList.get(0)+fileList.get(0));
            System.out.println("Created an Image from path:  " + pathList.get(0)+fileList.get(0));
            //set the labels image to this image
            image1Label.setIcon(imageFrom);
            System.out.println("imageIcon size:" + imageFrom.getIconHeight() + " by " + imageFrom.getIconWidth());
            System.out.println("Size on Label size:" + image1Label.getIcon().getIconHeight() + " by " + image1Label.getIcon().getIconWidth());     
            //convert the image to a buffered image
            imageStart = toBufferedImage(imageFrom.getImage());
            getAllColors();
        }
        
        
        
    }//GEN-LAST:event_jFileChooser1PropertyChange

    private void jTabbedPane1MouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTabbedPane1MouseMoved
        // TODO add your handling code here:
        
        if(jComboBox1.getSelectedIndex() == 0) {
            newColor = jColorChooser1.getColor();
            colorChangedTo.setBackground(jColorChooser1.getColor());
            colorChangedTo.setText(newColor.toString());
            rgbNewColor = newColor.getRGB();
        }
        else if(jComboBox1.getSelectedIndex() == 1) {
            newColor1 = jColorChooser1.getColor();
            colorChangedTo1.setBackground(jColorChooser1.getColor());
            colorChangedTo1.setText(newColor1.toString());
            rgbNewColor1 = newColor1.getRGB();
        }
        else if(jComboBox1.getSelectedIndex() == 2) {
            newColor2 = jColorChooser1.getColor();
            colorChangedTo2.setBackground(jColorChooser1.getColor());
            colorChangedTo2.setText(newColor2.toString());
            rgbNewColor2 = newColor2.getRGB();
        }
        else if(jComboBox1.getSelectedIndex() == 3) {
            newColor3 = jColorChooser1.getColor();
            colorChangedTo3.setBackground(jColorChooser1.getColor());
            colorChangedTo3.setText(newColor3.toString());
            rgbNewColor3 = newColor3.getRGB();
        }
        else if(jComboBox1.getSelectedIndex() == 4) {
            newColor4 = jColorChooser1.getColor();
            colorChangedTo4.setBackground(jColorChooser1.getColor());
            colorChangedTo4.setText(newColor4.toString());
            rgbNewColor4 = newColor4.getRGB();
        }
        else if(jComboBox1.getSelectedIndex() == 5) {
            newColor5 = jColorChooser1.getColor();
            colorChangedTo5.setBackground(jColorChooser1.getColor());
            colorChangedTo5.setText(newColor5.toString());
            rgbNewColor5 = newColor5.getRGB();
        }
        else if(jComboBox1.getSelectedIndex() == 6) {
            newColor6 = jColorChooser1.getColor();
            colorChangedTo6.setBackground(jColorChooser1.getColor());
            colorChangedTo6.setText(newColor6.toString());
            rgbNewColor6 = newColor6.getRGB();
        }
        else if(jComboBox1.getSelectedIndex() == 7) {
            newColor7 = jColorChooser1.getColor();
            colorChangedTo7.setBackground(jColorChooser1.getColor());
            colorChangedTo7.setText(newColor7.toString());
            rgbNewColor7 = newColor7.getRGB();
        }
        
        
    }//GEN-LAST:event_jTabbedPane1MouseMoved

    private void image1LabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_image1LabelMouseClicked
        // TODO add your handling code here:
        if (jComboBox1.getSelectedIndex() == 0) {
            System.out.println("0 Clicked on Icon at " + evt.getX() + ", " + evt.getY());
            rgbOldColor = imageStart.getRGB(evt.getX(), evt.getY());
            System.out.println("The color before using COLOR is: " + rgbOldColor);
            int red = (rgbOldColor >> 16) & 0x0ff;
            int green = (rgbOldColor >> 8) & 0x0ff;
            int blue = (rgbOldColor) & 0x0ff;
            oldColor = new Color(red, green, blue);
            System.out.println("The Color at is " + oldColor.toString());
            colorChangedFrom.setBackground(oldColor);
            colorChangedFrom.setText(oldColor.toString());
        } else if (jComboBox1.getSelectedIndex() == 1) {
            System.out.println("1 Clicked on Icon at " + evt.getX() + ", " + evt.getY());
            rgbOldColor1 = imageStart.getRGB(evt.getX(), evt.getY());
            System.out.println("The color before using COLOR is: " + rgbOldColor);
            int red = (rgbOldColor1 >> 16) & 0x0ff;
            int green = (rgbOldColor1 >> 8) & 0x0ff;
            int blue = (rgbOldColor1) & 0x0ff;
            oldColor1 = new Color(red, green, blue);
            System.out.println("The Color at is " + oldColor1.toString());
            colorChangedFrom1.setBackground(oldColor1);
            colorChangedFrom1.setText(oldColor1.toString());

        } else if (jComboBox1.getSelectedIndex() == 2) {
            System.out.println("2 Clicked on Icon at " + evt.getX() + ", " + evt.getY());
            rgbOldColor2 = imageStart.getRGB(evt.getX(), evt.getY());
            System.out.println("The color before using COLOR is: " + rgbOldColor2);
            int red = (rgbOldColor2 >> 16) & 0x0ff;
            int green = (rgbOldColor2 >> 8) & 0x0ff;
            int blue = (rgbOldColor2) & 0x0ff;
            oldColor2 = new Color(red, green, blue);
            System.out.println("The Color at is " + oldColor2.toString());
            colorChangedFrom2.setBackground(oldColor2);
            colorChangedFrom2.setText(oldColor2.toString());
        } else if (jComboBox1.getSelectedIndex() == 3) {
            System.out.println("3 Clicked on Icon at " + evt.getX() + ", " + evt.getY());
            rgbOldColor3 = imageStart.getRGB(evt.getX(), evt.getY());
            System.out.println("The color before using COLOR is: " + rgbOldColor3);
            int red = (rgbOldColor3 >> 16) & 0x0ff;
            int green = (rgbOldColor3 >> 8) & 0x0ff;
            int blue = (rgbOldColor3) & 0x0ff;
            oldColor3 = new Color(red, green, blue);
            System.out.println("The Color at is " + oldColor3.toString());
            colorChangedFrom3.setBackground(oldColor3);
            colorChangedFrom3.setText(oldColor3.toString());
        } else if (jComboBox1.getSelectedIndex() == 4) {
            System.out.println("4 Clicked on Icon at " + evt.getX() + ", " + evt.getY());
            rgbOldColor4 = imageStart.getRGB(evt.getX(), evt.getY());
            System.out.println("The color before using COLOR is: " + rgbOldColor4);
            int red = (rgbOldColor4 >> 16) & 0x0ff;
            int green = (rgbOldColor4 >> 8) & 0x0ff;
            int blue = (rgbOldColor4) & 0x0ff;
            oldColor4 = new Color(red, green, blue);
            System.out.println("The Color at is " + oldColor4.toString());
            colorChangedFrom4.setBackground(oldColor4);
            colorChangedFrom4.setText(oldColor4.toString());
        } else if (jComboBox1.getSelectedIndex() == 5) {
            System.out.println("5 Clicked on Icon at " + evt.getX() + ", " + evt.getY());
            rgbOldColor5 = imageStart.getRGB(evt.getX(), evt.getY());
            System.out.println("The color before using COLOR is: " + rgbOldColor5);
            int red = (rgbOldColor5 >> 16) & 0x0ff;
            int green = (rgbOldColor5 >> 8) & 0x0ff;
            int blue = (rgbOldColor5) & 0x0ff;
            oldColor5 = new Color(red, green, blue);
            System.out.println("The Color at is " + oldColor5.toString());
            colorChangedFrom5.setBackground(oldColor5);
            colorChangedFrom5.setText(oldColor5.toString());
        } else if (jComboBox1.getSelectedIndex() == 6) {
            System.out.println("6 Clicked on Icon at " + evt.getX() + ", " + evt.getY());
            rgbOldColor6 = imageStart.getRGB(evt.getX(), evt.getY());
            System.out.println("The color before using COLOR is: " + rgbOldColor6);
            int red = (rgbOldColor6 >> 16) & 0x0ff;
            int green = (rgbOldColor6 >> 8) & 0x0ff;
            int blue = (rgbOldColor6) & 0x0ff;
            oldColor6 = new Color(red, green, blue);
            System.out.println("The Color at is " + oldColor6.toString());
            colorChangedFrom6.setBackground(oldColor6);
            colorChangedFrom6.setText(oldColor6.toString());
        } else if (jComboBox1.getSelectedIndex() == 7) {
            System.out.println("7 Clicked on Icon at " + evt.getX() + ", " + evt.getY());
            rgbOldColor7 = imageStart.getRGB(evt.getX(), evt.getY());
            System.out.println("The color before using COLOR is: " + rgbOldColor7);
            int red = (rgbOldColor7 >> 16) & 0x0ff;
            int green = (rgbOldColor7 >> 8) & 0x0ff;
            int blue = (rgbOldColor7) & 0x0ff;
            oldColor7 = new Color(red, green, blue);
            System.out.println("The Color at is " + oldColor7.toString());
            colorChangedFrom7.setBackground(oldColor7);
            colorChangedFrom7.setText(oldColor7.toString());
        }
    }//GEN-LAST:event_image1LabelMouseClicked

    private void startButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startButtonActionPerformed
        // TODO add your handling code here:
        editFiles();
    }//GEN-LAST:event_startButtonActionPerformed

private void colorsLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_colorsLabelMouseClicked
    // TODO add your handling code here:
    if (jComboBox1.getSelectedIndex() == 0) {
        System.out.println("0 Clicked on Icon at " + evt.getX() + ", " + evt.getY());
        rgbOldColor = imageColorsBuff.getRGB(evt.getX(), evt.getY());
        System.out.println("The color before using COLOR is: " + rgbOldColor);
        int red = (rgbOldColor >> 16) & 0x0ff;
        int green = (rgbOldColor >> 8) & 0x0ff;
        int blue = (rgbOldColor) & 0x0ff;
        oldColor = new Color(red, green, blue);
        System.out.println("The Color at is " + oldColor.toString());
        colorChangedFrom.setBackground(oldColor);
        colorChangedFrom.setText(oldColor.toString());
    } else if (jComboBox1.getSelectedIndex() == 1) {
        System.out.println("1 Clicked on Icon at " + evt.getX() + ", " + evt.getY());
        rgbOldColor1 = imageColorsBuff.getRGB(evt.getX(), evt.getY());
        System.out.println("The color before using COLOR is: " + rgbOldColor);
        int red = (rgbOldColor1 >> 16) & 0x0ff;
        int green = (rgbOldColor1 >> 8) & 0x0ff;
        int blue = (rgbOldColor1) & 0x0ff;
        oldColor1 = new Color(red, green, blue);
        System.out.println("The Color at is " + oldColor1.toString());
        colorChangedFrom1.setBackground(oldColor1);
        colorChangedFrom1.setText(oldColor1.toString());

    } else if (jComboBox1.getSelectedIndex() == 2) {
        System.out.println("2 Clicked on Icon at " + evt.getX() + ", " + evt.getY());
        rgbOldColor2 = imageColorsBuff.getRGB(evt.getX(), evt.getY());
        System.out.println("The color before using COLOR is: " + rgbOldColor2);
        int red = (rgbOldColor2 >> 16) & 0x0ff;
        int green = (rgbOldColor2 >> 8) & 0x0ff;
        int blue = (rgbOldColor2) & 0x0ff;
        oldColor2 = new Color(red, green, blue);
        System.out.println("The Color at is " + oldColor2.toString());
        colorChangedFrom2.setBackground(oldColor2);
        colorChangedFrom2.setText(oldColor2.toString());
    }
    else if (jComboBox1.getSelectedIndex() == 3) {
        System.out.println("3 Clicked on Icon at " + evt.getX() + ", " + evt.getY());
        rgbOldColor3 = imageColorsBuff.getRGB(evt.getX(), evt.getY());
        System.out.println("The color before using COLOR is: " + rgbOldColor3);
        int red = (rgbOldColor3 >> 16) & 0x0ff;
        int green = (rgbOldColor3 >> 8) & 0x0ff;
        int blue = (rgbOldColor3) & 0x0ff;
        oldColor3 = new Color(red, green, blue);
        System.out.println("The Color at is " + oldColor3.toString());
        colorChangedFrom3.setBackground(oldColor3);
        colorChangedFrom3.setText(oldColor3.toString());
    }
    else if (jComboBox1.getSelectedIndex() == 4) {
        System.out.println("4 Clicked on Icon at " + evt.getX() + ", " + evt.getY());
        rgbOldColor4 = imageColorsBuff.getRGB(evt.getX(), evt.getY());
        System.out.println("The color before using COLOR is: " + rgbOldColor4);
        int red = (rgbOldColor4 >> 16) & 0x0ff;
        int green = (rgbOldColor4 >> 8) & 0x0ff;
        int blue = (rgbOldColor4) & 0x0ff;
        oldColor4 = new Color(red, green, blue);
        System.out.println("The Color at is " + oldColor4.toString());
        colorChangedFrom4.setBackground(oldColor4);
        colorChangedFrom4.setText(oldColor4.toString());
    }
    else if (jComboBox1.getSelectedIndex() == 5) {
        System.out.println("5 Clicked on Icon at " + evt.getX() + ", " + evt.getY());
        rgbOldColor5 = imageColorsBuff.getRGB(evt.getX(), evt.getY());
        System.out.println("The color before using COLOR is: " + rgbOldColor5);
        int red = (rgbOldColor5 >> 16) & 0x0ff;
        int green = (rgbOldColor5 >> 8) & 0x0ff;
        int blue = (rgbOldColor5) & 0x0ff;
        oldColor5 = new Color(red, green, blue);
        System.out.println("The Color at is " + oldColor5.toString());
        colorChangedFrom5.setBackground(oldColor5);
        colorChangedFrom5.setText(oldColor5.toString());
    }
    else if (jComboBox1.getSelectedIndex() == 6) {
        System.out.println("6 Clicked on Icon at " + evt.getX() + ", " + evt.getY());
        rgbOldColor6 = imageColorsBuff.getRGB(evt.getX(), evt.getY());
        System.out.println("The color before using COLOR is: " + rgbOldColor6);
        int red = (rgbOldColor6 >> 16) & 0x0ff;
        int green = (rgbOldColor6 >> 8) & 0x0ff;
        int blue = (rgbOldColor6) & 0x0ff;
        oldColor6 = new Color(red, green, blue);
        System.out.println("The Color at is " + oldColor6.toString());
        colorChangedFrom6.setBackground(oldColor6);
        colorChangedFrom6.setText(oldColor6.toString());
    }
    else if (jComboBox1.getSelectedIndex() == 7) {
        System.out.println("7 Clicked on Icon at " + evt.getX() + ", " + evt.getY());
        rgbOldColor7 = imageColorsBuff.getRGB(evt.getX(), evt.getY());
        System.out.println("The color before using COLOR is: " + rgbOldColor7);
        int red = (rgbOldColor7 >> 16) & 0x0ff;
        int green = (rgbOldColor7 >> 8) & 0x0ff;
        int blue = (rgbOldColor7) & 0x0ff;
        oldColor7 = new Color(red, green, blue);
        System.out.println("The Color at is " + oldColor7.toString());
        colorChangedFrom7.setBackground(oldColor7);
        colorChangedFrom7.setText(oldColor7.toString());
    }
    

}//GEN-LAST:event_colorsLabelMouseClicked

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField colorChangedFrom;
    private javax.swing.JTextField colorChangedFrom1;
    private javax.swing.JTextField colorChangedFrom2;
    private javax.swing.JTextField colorChangedFrom3;
    private javax.swing.JTextField colorChangedFrom4;
    private javax.swing.JTextField colorChangedFrom5;
    private javax.swing.JTextField colorChangedFrom6;
    private javax.swing.JTextField colorChangedFrom7;
    private javax.swing.JTextField colorChangedTo;
    private javax.swing.JTextField colorChangedTo1;
    private javax.swing.JTextField colorChangedTo2;
    private javax.swing.JTextField colorChangedTo3;
    private javax.swing.JTextField colorChangedTo4;
    private javax.swing.JTextField colorChangedTo5;
    private javax.swing.JTextField colorChangedTo6;
    private javax.swing.JTextField colorChangedTo7;
    private javax.swing.JLabel colorsLabel;
    private javax.swing.JLabel image1Label;
    private javax.swing.JColorChooser jColorChooser1;
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JFileChooser jFileChooser1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JTextField pathTextField;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JButton startButton;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    // End of variables declaration//GEN-END:variables

    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;

    private JDialog aboutBox;
    
    
    // This method returns an Image object from a buffered image
    public static Image toImage(BufferedImage bufferedImage) {
        return Toolkit.getDefaultToolkit().createImage(bufferedImage.getSource());
    }
    
    // This method returns a buffered image with the contents of an image
    public static BufferedImage toBufferedImage(Image image) {
        if (image instanceof BufferedImage) {
            return (BufferedImage) image;
        }

        // This code ensures that all the pixels in the image are loaded
        image = new ImageIcon(image).getImage();

        // Determine if the image has transparent pixels; for this method's
        // implementation, see Determining If an Image Has Transparent Pixels
        boolean hasAlpha = true;

        // Create a buffered image with a format that's compatible with the screen
        BufferedImage bimage = null;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        try {
            // Determine the type of transparency of the new buffered image
            int transparency = Transparency.OPAQUE;
            if (hasAlpha) {
                transparency = Transparency.BITMASK;
            }

            // Create the buffered image
            GraphicsDevice gs = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gs.getDefaultConfiguration();
            bimage = gc.createCompatibleImage(
                    image.getWidth(null), image.getHeight(null), transparency);
        } catch (HeadlessException e) {
            // The system does not have a screen
        }

        if (bimage == null) {
            // Create a buffered image using the default color model
            int type = BufferedImage.TYPE_INT_RGB;
            if (hasAlpha) {
                type = BufferedImage.TYPE_INT_ARGB;
            }
            bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), type);
        }

        // Copy image to buffered image
        Graphics g = bimage.createGraphics();

        // Paint the image onto the buffered image
        g.drawImage(image, 0, 0, null);
        g.dispose();

        return bimage;
    }
}
