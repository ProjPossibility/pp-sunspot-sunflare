/*
 * Copyright (c) 2006 Sun Microsystems, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 **/
package org.sunspotworld.demo;

/*
 * TelemetryFrame.java
 *
 * GUI creating code to make a window to display accelerometer data gathered
 * from a remote SPOT. Provides the user interface to interact with the SPOT
 * and to control the telemetry data collected.
 *
 * author: Ron Goldman
 * date: May 2, 2006
 */

import com.sun.j3d.utils.behaviors.mouse.MouseRotate;
import com.sun.j3d.utils.geometry.ColorCube;
import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.geometry.Text2D;
import com.sun.j3d.utils.universe.SimpleUniverse;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import javax.swing.*;
import java.util.*;
import java.text.*;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Color3f;
import javax.vecmath.Vector3f;

/**
 * GUI creating code to make a window to display accelerometer data gathered
 * from a remote SPOT. Provides the user interface to interact with the SPOT
 * and to control the telemetry data collected.
 *
 * @author Ron Goldman
 */
public class TelemetryFrame extends JFrame implements Printable {
    
    private static String version = "1.0";
    private static String versionDate = "June 8, 2006";
    private static int numWindows = 0;
    private static AccelerometerListener listener = null;
    private static final Font footerFont = new Font("Serif", Font.PLAIN, 9);
    private static final DateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy  HH:mm z");
    
    private static Vector windows = new Vector();
    private static ImageIcon aboutIcon = null;
    
    private GraphView graphView;
    private JPanel axisView;
    private boolean sendData = false;
    private File file = null;
    private boolean fixedData = false;
    private boolean clearedData = true;
    
    private PrinterJob printJob = PrinterJob.getPrinterJob();
    private PageFormat pageFormat = printJob.defaultPage();
    
    private TransformGroup transformGroup = null;
    
    /**
     * Creates a new TelemetryFrame window.
     */
    public TelemetryFrame() {
        init(null);
    }
    
    /**
     * Creates a new TelemetryFrame window with an associated file.
     *
     * @param file the file to read/write accelerometer data from/to
     */
    public TelemetryFrame(File file) {
        init(file);
    }
    
    /**
     * Initialize the new TelemetryFrame
     */
    private void init(File file) {
        if (listener == null) {
            listener = new AccelerometerListener();         // only need one
            listener.start();
            aboutIcon = new ImageIcon(getClass().getResource("/org/sunspotworld/demo/racecar.gif"));
        }
        initComponents();
        
        // Now set up 3d canvas
        create3dCanvas();
        
        this.file = file;
        if (file != null) {
            this.setTitle(file.getName());
            fixedData = true;
            clearedData = false;
            twoGRadioButton.setEnabled(false);
            sixGRadioButton.setEnabled(false);
            sendDataButton.setEnabled(false);
            clearButton.setEnabled(false);
            calibrateButton.setEnabled(false);
        }
        pageFormat.setOrientation(PageFormat.LANDSCAPE);
    }
    
    private void create3dCanvas() {
        GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
        Canvas3D canvas3D = new Canvas3D( config );
        SimpleUniverse simpleU = new SimpleUniverse(canvas3D);
        BranchGroup scene = createSceneGraph();
        simpleU.getViewingPlatform().setNominalViewingTransform();       // This will move the ViewPlatform back a bit so the
        simpleU.addBranchGraph(scene);
        
        simulator.setLayout( new BorderLayout() );
        simulator.setOpaque( false );
        simulator.add("Center", canvas3D);   // <-- HERE IT IS - tada! j3d in swing
        
    }
    
    // make a scene with a cube and a label
    private BranchGroup createSceneGraph() {
        
        BranchGroup objRoot = new BranchGroup();
        TransformGroup root_group = new TransformGroup(  );
        transformGroup = new TransformGroup();

        root_group.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE); // allow the mouse behavior to rotate the scene
        root_group.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);

        transformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE); // all object to move
        transformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);

        objRoot.addChild( root_group );  // this is the local origin  - everyone hangs off this - moving this move every one
        
        root_group.addChild(transformGroup);
        
        transformGroup.addChild( new ColorCube(0.2) );  // add a color cube
        transformGroup.addChild( new Label3D( 0.2f, 0.2f, 0.0f, "SunSPOT") ); // add a label to the scene
                
        MouseRotate mouseRotate = new MouseRotate( root_group );  // add the mouse behavior
        mouseRotate.setSchedulingBounds( new BoundingSphere() );
        objRoot.addChild( mouseRotate);
        return objRoot;
    }
    
    /**
     * Title: Label3D
     * Description: Places a Text2D in a scene, allows the text to be seen from the back
     * Copyright:    Copyright (c) 2001
     * Company:      Meissner Software Development, LLC
     * @author Karl Meissner
     * @version 1.0
     */
    
    class Label3D
            extends TransformGroup {
        
        public Label3D( float x, float y, float z, String msg ) {
            super();
            
            // place it in the scene graph
            Transform3D offset = new Transform3D();
            offset.setTranslation( new Vector3f( x, y, z ));
            this.setTransform( offset );
            
            // face it in the scene graph
            Transform3D rotation = new Transform3D();
            TransformGroup rotation_group = new TransformGroup( rotation );
            this.addChild( rotation_group );
            
            // make a texture mapped polygon
            Text2D msg_poly = new Text2D( msg, new
                    Color3f( 1.0f, 1.0f, 1.0f),
                    "Helvetica", 18, Font.PLAIN );
            
            
            // set it to draw both the front and back of the poly
            PolygonAttributes msg_attributes = new PolygonAttributes();
            msg_attributes.setCullFace( PolygonAttributes.CULL_NONE );
            msg_attributes.setBackFaceNormalFlip( true );
            msg_poly.getAppearance().setPolygonAttributes( msg_attributes );
            
            // attach it
            rotation_group.addChild( msg_poly );
        }
        
    }
    
    /**
     * Set the GraphView to display accelerometer values for this window.
     */
    private void setGraphView(GraphView gv) {
        graphView = gv;
        graphView.setGUI(this);
        graphViewScrollPane.setViewportView(gv);
        gv.setTransformGroup(transformGroup);
        gv.setViewport(graphViewScrollPane.getViewport());
        gv.setMaxGLabel(maxGLabel);
        Integer fieldWidth = (Integer)filterWidthField.getValue();
        graphView.setFilterWidth(fieldWidth.intValue() - 1);
        final GraphView gview = gv;
        axisView = new JPanel(){
            public Dimension getPreferredSize() {
                return new Dimension(GraphView.AXIS_WIDTH, gview.getPreferredSize().height);
            }
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(Color.BLACK);
                gview.paintYaxis(g);
            }
        };
        axisView.setBackground(Color.WHITE);
        y_axisPanel.add(axisView);
        gv.setAxisPanel(axisView);
        if (fixedData) {
            if (graphView.is2G()) {
                twoGRadioButton.setSelected(true);
            } else {
                sixGRadioButton.setSelected(true);
            }
            twoGRadioButton.setEnabled(false);
            sixGRadioButton.setEnabled(false);
        }
    }
    
    /**
     * Check that new window has a unique name.
     *
     * @param str proposed new window name
     * @return true if current name is unique, false if it is the same as another window
     */
    private static boolean checkTitle(String str) {
        boolean results = true;
        for (Enumeration e = windows.elements() ; e.hasMoreElements() ;) {
            TelemetryFrame fr = (TelemetryFrame)e.nextElement();
            if (str.equals(fr.getTitle())) {
                results = false;
                break;
            }
        }
        return results;
    }
    
    /**
     * Connect this window with a graph display and a file.
     */
    private static TelemetryFrame frameGraph(File file, GraphView graphView) {
        TelemetryFrame telemetryFrame = new TelemetryFrame(file);
        telemetryFrame.setGraphView(graphView);
        telemetryFrame.setVisible(true);
        String str = telemetryFrame.getTitle();
        if (!checkTitle(str)) {
            int i = 1;
            while (true) {
                if (checkTitle(str + "-" + i)) {
                    telemetryFrame.setTitle(str + "-" + i);
                    break;
                } else {
                    i++;
                }
            }
        }
        windows.add(telemetryFrame);
        numWindows++;
        return telemetryFrame;
    }
    
    /**
     * Display the current connection status to a remote SPOT.
     * Called by the AccelerometerListener whenever the radio connection status changes.
     *
     * @param conn true if now connected to a remote SPOT
     * @param msg the String message to display, includes the
     */
    public void setConnectionStatus(boolean conn, String msg) {
        connStatusLabel.setText(msg);
        pingButton.setEnabled(conn);
        reconnButton.setEnabled(conn);
        if (!fixedData) {
            if (listener.is2GScale()) {
                twoGRadioButton.setSelected(true);
            } else {
                sixGRadioButton.setSelected(true);
            }
            twoGRadioButton.setEnabled(conn);
            sixGRadioButton.setEnabled(conn);
            sendDataButton.setEnabled(conn);
            clearButton.setEnabled(conn);
            calibrateButton.setEnabled(conn);
        }
    }
    
    /**
     * Select a (new) file to save the accelerometer data in.
     */
    private void doSaveAs() {
        JFileChooser chooser;
        if (file != null) {
            chooser = new JFileChooser(file.getParent());
        } else {
            chooser = new JFileChooser(System.getProperty("user.dir"));
        }
        int returnVal = chooser.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            file = chooser.getSelectedFile();
            if (file.exists()) {
                int n = JOptionPane.showConfirmDialog(this, "The file: " + file.getName() +
                        " already exists. Do you wish to replace it?",
                        "File Already Exists",
                        JOptionPane.YES_NO_OPTION);
                if (n != JOptionPane.YES_OPTION) {
                    return;                             // cancel the Save As command
                }
            }
            setTitle(file.getName());
            doSave();
        }
    }
    
    /**
     * Save the current accelerometer data to the file associated with this window.
     */
    private void doSave() {
        if (graphView.writeData(file)) {
            saveMenuItem.setEnabled(false);
        }
    }
    
    /**
     * Routine to print out each page of the current graph with a footer.
     *
     * @param g the graphics context to use to print
     * @param pageFormat how big is each page
     * @param pageIndex the page to print
     */
    public int print(Graphics g, PageFormat pageFormat, int pageIndex) {
        double xscale = 0.5;
        double yscale = 0.75;
        int mx = 40;
        int my = 30;
        double x0 = pageFormat.getImageableX() + mx;
        double y0 = pageFormat.getImageableY() + my;
        double axisW = GraphView.AXIS_WIDTH * xscale;
        double w = pageFormat.getImageableWidth() - axisW - 2 * mx;
        double h = pageFormat.getImageableHeight() - 2 * my;
        int pagesNeeded = (int) (xscale * graphView.getMaxWidth() / w);
        if (pageIndex > pagesNeeded) {
            return(NO_SUCH_PAGE);
        } else {
            Graphics2D g2d = (Graphics2D)g;
            // first print our footer
            int y = (int) (y0 + h + 18);
            g2d.setPaint(Color.black);
            g2d.setFont(footerFont);
            g2d.drawString(dateFormat.format(new Date()).toString(), (int) (x0 + 5), y);
            if (file != null) {
                String name = file.getName();
                g2d.drawString(name, (int) (x0 + w/2 - 2 * name.length() / 2), y);
            }
            g2d.drawString((pageIndex + 1) + "/" + (pagesNeeded + 1), (int) (x0 + w - 20), y);
            
            // now print the Y-axis
            axisView.setDoubleBuffered(false);
            g2d.translate(x0, y0);
            g2d.scale(xscale, yscale);
            axisView.paint(g2d);
            axisView.setDoubleBuffered(true);
            
            // now have graph view print the next page
            // note: while the values to translate & setClip work they seem wrong. Why 2 * axisW ???
            graphView.setDoubleBuffered(false);
            g2d.translate(2 * axisW + 1 - (w * pageIndex) / xscale, 0);
            g2d.setClip((int)((w * pageIndex) / xscale + 2), 0, (int)(w / xscale), (int)(h / yscale));
            graphView.paint(g2d);
            graphView.setDoubleBuffered(true);
            
            return(PAGE_EXISTS);
        }
    }
    
    /**
     * Routine to bring the user selected window to the front.
     *
     * @param evt the menu command with the name of the selected window
     */
    private void windowSelected(ActionEvent evt) {
        boolean found = false;
        String str = evt.getActionCommand();
        for (Enumeration e = windows.elements() ; e.hasMoreElements() ;) {
            TelemetryFrame fr = (TelemetryFrame)e.nextElement();
            if (str.equals(fr.getTitle())) {
                fr.setVisible(true);
                found = true;
                break;
            }
        }
    }
    
    
    /**
     * Cleanly exit.
     */
    private void doQuit() {
        listener.doQuit();
        System.exit(0);
    }
    
    // GUI code generated using NetBeans GUI editor:
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        fullscaleGroup = new javax.swing.ButtonGroup();
        xZoomGroup = new javax.swing.ButtonGroup();
        yZoomGroup = new javax.swing.ButtonGroup();
        smoothGroup = new javax.swing.ButtonGroup();
        jPanel1 = new javax.swing.JPanel();
        y_axisPanel = new javax.swing.JPanel();
        graphViewScrollPane = new javax.swing.JScrollPane();
        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        axisPanel = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        xZoomButton1 = new javax.swing.JRadioButton();
        xZoomButton2 = new javax.swing.JRadioButton();
        xZoomButton3 = new javax.swing.JRadioButton();
        xZoomButton4 = new javax.swing.JRadioButton();
        jPanel6 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        yZoomButton1 = new javax.swing.JRadioButton();
        yZoomButton2 = new javax.swing.JRadioButton();
        yZoomButton3 = new javax.swing.JRadioButton();
        yZoomButton4 = new javax.swing.JRadioButton();
        jPanel7 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        xCheckBox = new javax.swing.JCheckBox();
        yCheckBox = new javax.swing.JCheckBox();
        zCheckBox = new javax.swing.JCheckBox();
        gCheckBox = new javax.swing.JCheckBox();
        jPanel4 = new javax.swing.JPanel();
        gPanel = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        twoGRadioButton = new javax.swing.JRadioButton();
        sixGRadioButton = new javax.swing.JRadioButton();
        jLabel9 = new javax.swing.JLabel();
        maxGLabel = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        connStatusLabel = new javax.swing.JLabel();
        smoothPanel = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        noSmoothingButton = new javax.swing.JRadioButton();
        boxcarSmoothingButton = new javax.swing.JRadioButton();
        triangleSmoothingButton = new javax.swing.JRadioButton();
        jLabel4 = new javax.swing.JLabel();
        filterWidthField = new javax.swing.JFormattedTextField();
        buttonPanel = new javax.swing.JPanel();
        clearButton = new javax.swing.JButton();
        calibrateButton = new javax.swing.JButton();
        sendDataButton = new javax.swing.JButton();
        pingButton = new javax.swing.JButton();
        reconnButton = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        dataPanel = new javax.swing.JPanel();
        label_velx = new javax.swing.JLabel();
        label_velx_data = new javax.swing.JLabel();
        label_vely = new javax.swing.JLabel();
        label_vely_data = new javax.swing.JLabel();
        label_velz = new javax.swing.JLabel();
        label_velz_data = new javax.swing.JLabel();
        label_distx = new javax.swing.JLabel();
        label_distx_data = new javax.swing.JLabel();
        label_disty = new javax.swing.JLabel();
        label_disty_data = new javax.swing.JLabel();
        label_distz = new javax.swing.JLabel();
        label_distz_data = new javax.swing.JLabel();
        simulator = new javax.swing.JPanel();
        jMenuBar1 = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        aboutMenuItem = new javax.swing.JMenuItem();
        jSeparator5 = new javax.swing.JSeparator();
        newMenuItem = new javax.swing.JMenuItem();
        openMenuItem = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JSeparator();
        closeMenuItem = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JSeparator();
        saveMenuItem = new javax.swing.JMenuItem();
        saveAsMenuItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        pagesetupMenuItem = new javax.swing.JMenuItem();
        printMenuItem = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JSeparator();
        quitMenuItem = new javax.swing.JMenuItem();
        windowMenu = new javax.swing.JMenu();

        getContentPane().setLayout(new java.awt.BorderLayout(0, 5));

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Sun SPOTs Telemetry Demo");
        setName("spotTelemetry");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowActivated(java.awt.event.WindowEvent evt) {
                formWindowActivated(evt);
            }
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
            public void windowDeactivated(java.awt.event.WindowEvent evt) {
                formWindowDeactivated(evt);
            }
        });

        jPanel1.setLayout(new java.awt.GridBagLayout());

        y_axisPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 0, 0));

        y_axisPanel.setAlignmentX(1.0F);
        y_axisPanel.setAlignmentY(0.0F);
        y_axisPanel.setMaximumSize(new java.awt.Dimension(65, 3725));
        y_axisPanel.setMinimumSize(new java.awt.Dimension(65, 125));
        y_axisPanel.setPreferredSize(new java.awt.Dimension(65, 525));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel1.add(y_axisPanel, gridBagConstraints);

        graphViewScrollPane.setBorder(null);
        graphViewScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        graphViewScrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        graphViewScrollPane.setAutoscrolls(true);
        graphViewScrollPane.setMaximumSize(new java.awt.Dimension(32767, 7725));
        graphViewScrollPane.setMinimumSize(new java.awt.Dimension(350, 125));
        graphViewScrollPane.setPreferredSize(new java.awt.Dimension(800, 525));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel1.add(graphViewScrollPane, gridBagConstraints);

        jPanel2.setMaximumSize(new java.awt.Dimension(5, 7725));
        jPanel2.setMinimumSize(new java.awt.Dimension(5, 125));
        jPanel2.setPreferredSize(new java.awt.Dimension(5, 525));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(jPanel2, gridBagConstraints);

        getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);

        jPanel3.setLayout(new java.awt.GridBagLayout());

        jPanel3.setAlignmentX(0.0F);
        jPanel3.setAlignmentY(0.0F);
        jPanel3.setMaximumSize(new java.awt.Dimension(32767, 147));
        jPanel3.setMinimumSize(new java.awt.Dimension(605, 103));
        jPanel3.setPreferredSize(new java.awt.Dimension(650, 103));
        axisPanel.setLayout(new java.awt.GridLayout(3, 1));

        axisPanel.setAlignmentX(0.0F);
        axisPanel.setAlignmentY(0.0F);
        axisPanel.setMaximumSize(new java.awt.Dimension(350, 90));
        axisPanel.setMinimumSize(new java.awt.Dimension(275, 84));
        axisPanel.setPreferredSize(new java.awt.Dimension(295, 90));
        jPanel5.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jPanel5.setAlignmentX(0.0F);
        jPanel5.setMinimumSize(new java.awt.Dimension(350, 28));
        jPanel5.setPreferredSize(new java.awt.Dimension(350, 28));
        jLabel5.setText(" Zoom x-axis:");
        jPanel5.add(jLabel5);

        xZoomGroup.add(xZoomButton1);
        xZoomButton1.setText("0.5x");
        xZoomButton1.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        xZoomButton1.setMargin(new java.awt.Insets(0, 0, 0, 0));
        xZoomButton1.setMaximumSize(new java.awt.Dimension(52, 18));
        xZoomButton1.setMinimumSize(new java.awt.Dimension(52, 18));
        xZoomButton1.setPreferredSize(new java.awt.Dimension(52, 18));
        xZoomButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                xZoomButton1ActionPerformed(evt);
            }
        });

        jPanel5.add(xZoomButton1);

        xZoomGroup.add(xZoomButton2);
        xZoomButton2.setSelected(true);
        xZoomButton2.setText("1x");
        xZoomButton2.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        xZoomButton2.setMargin(new java.awt.Insets(0, 0, 0, 0));
        xZoomButton2.setMaximumSize(new java.awt.Dimension(45, 18));
        xZoomButton2.setMinimumSize(new java.awt.Dimension(45, 18));
        xZoomButton2.setPreferredSize(new java.awt.Dimension(45, 18));
        xZoomButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                xZoomButton2ActionPerformed(evt);
            }
        });

        jPanel5.add(xZoomButton2);

        xZoomGroup.add(xZoomButton3);
        xZoomButton3.setText("2x");
        xZoomButton3.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        xZoomButton3.setMargin(new java.awt.Insets(0, 0, 0, 0));
        xZoomButton3.setMaximumSize(new java.awt.Dimension(45, 18));
        xZoomButton3.setMinimumSize(new java.awt.Dimension(45, 18));
        xZoomButton3.setPreferredSize(new java.awt.Dimension(45, 18));
        xZoomButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                xZoomButton3ActionPerformed(evt);
            }
        });

        jPanel5.add(xZoomButton3);

        xZoomGroup.add(xZoomButton4);
        xZoomButton4.setText("4x");
        xZoomButton4.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        xZoomButton4.setMargin(new java.awt.Insets(0, 0, 0, 0));
        xZoomButton4.setMaximumSize(new java.awt.Dimension(45, 18));
        xZoomButton4.setMinimumSize(new java.awt.Dimension(45, 18));
        xZoomButton4.setPreferredSize(new java.awt.Dimension(45, 18));
        xZoomButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                xZoomButton4ActionPerformed(evt);
            }
        });

        jPanel5.add(xZoomButton4);

        axisPanel.add(jPanel5);

        jPanel6.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jPanel6.setAlignmentX(0.0F);
        jPanel6.setMinimumSize(new java.awt.Dimension(350, 28));
        jPanel6.setPreferredSize(new java.awt.Dimension(350, 28));
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel2.setText("y-axis:");
        jLabel2.setMaximumSize(new java.awt.Dimension(90, 16));
        jLabel2.setMinimumSize(new java.awt.Dimension(90, 16));
        jLabel2.setPreferredSize(new java.awt.Dimension(90, 16));
        jPanel6.add(jLabel2);

        yZoomGroup.add(yZoomButton1);
        yZoomButton1.setSelected(true);
        yZoomButton1.setText("1x  ");
        yZoomButton1.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        yZoomButton1.setMargin(new java.awt.Insets(0, 0, 0, 0));
        yZoomButton1.setMaximumSize(new java.awt.Dimension(52, 18));
        yZoomButton1.setMinimumSize(new java.awt.Dimension(52, 18));
        yZoomButton1.setPreferredSize(new java.awt.Dimension(52, 18));
        yZoomButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                yZoomButton1ActionPerformed(evt);
            }
        });

        jPanel6.add(yZoomButton1);

        yZoomGroup.add(yZoomButton2);
        yZoomButton2.setText("2x");
        yZoomButton2.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        yZoomButton2.setMargin(new java.awt.Insets(0, 0, 0, 0));
        yZoomButton2.setMaximumSize(new java.awt.Dimension(45, 18));
        yZoomButton2.setMinimumSize(new java.awt.Dimension(45, 18));
        yZoomButton2.setPreferredSize(new java.awt.Dimension(45, 18));
        yZoomButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                yZoomButton2ActionPerformed(evt);
            }
        });

        jPanel6.add(yZoomButton2);

        yZoomGroup.add(yZoomButton3);
        yZoomButton3.setText("4x");
        yZoomButton3.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        yZoomButton3.setMargin(new java.awt.Insets(0, 0, 0, 0));
        yZoomButton3.setMaximumSize(new java.awt.Dimension(45, 18));
        yZoomButton3.setMinimumSize(new java.awt.Dimension(45, 18));
        yZoomButton3.setPreferredSize(new java.awt.Dimension(45, 18));
        yZoomButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                yZoomButton3ActionPerformed(evt);
            }
        });

        jPanel6.add(yZoomButton3);

        yZoomGroup.add(yZoomButton4);
        yZoomButton4.setText("8x");
        yZoomButton4.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        yZoomButton4.setMargin(new java.awt.Insets(0, 0, 0, 0));
        yZoomButton4.setMaximumSize(new java.awt.Dimension(45, 18));
        yZoomButton4.setMinimumSize(new java.awt.Dimension(45, 18));
        yZoomButton4.setPreferredSize(new java.awt.Dimension(45, 18));
        yZoomButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                yZoomButton4ActionPerformed(evt);
            }
        });

        jPanel6.add(yZoomButton4);

        axisPanel.add(jPanel6);

        jPanel7.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jPanel7.setMinimumSize(new java.awt.Dimension(350, 28));
        jPanel7.setPreferredSize(new java.awt.Dimension(350, 28));
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel1.setText("Show accel:");
        jLabel1.setMaximumSize(new java.awt.Dimension(90, 16));
        jLabel1.setMinimumSize(new java.awt.Dimension(90, 16));
        jLabel1.setPreferredSize(new java.awt.Dimension(90, 16));
        jPanel7.add(jLabel1);
        jLabel1.getAccessibleContext().setAccessibleName("Show accel: ");

        xCheckBox.setForeground(new java.awt.Color(0, 150, 0));
        xCheckBox.setSelected(true);
        xCheckBox.setText("aX ");
        xCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        xCheckBox.setMargin(new java.awt.Insets(0, 1, 0, 0));
        xCheckBox.setMaximumSize(new java.awt.Dimension(52, 18));
        xCheckBox.setMinimumSize(new java.awt.Dimension(52, 18));
        xCheckBox.setPreferredSize(new java.awt.Dimension(52, 18));
        xCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                xCheckBoxActionPerformed(evt);
            }
        });

        jPanel7.add(xCheckBox);

        yCheckBox.setForeground(java.awt.Color.blue);
        yCheckBox.setSelected(true);
        yCheckBox.setText("aY");
        yCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        yCheckBox.setMargin(new java.awt.Insets(0, 0, 0, 0));
        yCheckBox.setMaximumSize(new java.awt.Dimension(45, 18));
        yCheckBox.setMinimumSize(new java.awt.Dimension(45, 18));
        yCheckBox.setPreferredSize(new java.awt.Dimension(45, 18));
        yCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                yCheckBoxActionPerformed(evt);
            }
        });

        jPanel7.add(yCheckBox);

        zCheckBox.setForeground(java.awt.Color.red);
        zCheckBox.setSelected(true);
        zCheckBox.setText("aZ");
        zCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        zCheckBox.setMargin(new java.awt.Insets(0, 0, 0, 0));
        zCheckBox.setMaximumSize(new java.awt.Dimension(45, 18));
        zCheckBox.setMinimumSize(new java.awt.Dimension(45, 18));
        zCheckBox.setPreferredSize(new java.awt.Dimension(45, 18));
        zCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zCheckBoxActionPerformed(evt);
            }
        });

        jPanel7.add(zCheckBox);

        gCheckBox.setFont(new java.awt.Font("Lucida Grande", 1, 13));
        gCheckBox.setForeground(new java.awt.Color(255, 140, 0));
        gCheckBox.setSelected(true);
        gCheckBox.setText("|a|");
        gCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        gCheckBox.setIconTextGap(2);
        gCheckBox.setMargin(new java.awt.Insets(0, 0, 0, 0));
        gCheckBox.setMaximumSize(new java.awt.Dimension(45, 18));
        gCheckBox.setMinimumSize(new java.awt.Dimension(45, 18));
        gCheckBox.setPreferredSize(new java.awt.Dimension(45, 18));
        gCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gCheckBoxActionPerformed(evt);
            }
        });

        jPanel7.add(gCheckBox);

        axisPanel.add(jPanel7);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.5;
        jPanel3.add(axisPanel, gridBagConstraints);

        jPanel4.setLayout(new java.awt.GridLayout(2, 1));

        jPanel4.setPreferredSize(new java.awt.Dimension(455, 64));
        gPanel.setLayout(new java.awt.GridBagLayout());

        gPanel.setAlignmentX(0.0F);
        gPanel.setMaximumSize(new java.awt.Dimension(32767, 28));
        gPanel.setMinimumSize(new java.awt.Dimension(345, 28));
        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel8.setText("Scale: ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 0, 0);
        gPanel.add(jLabel8, gridBagConstraints);

        fullscaleGroup.add(twoGRadioButton);
        twoGRadioButton.setSelected(true);
        twoGRadioButton.setText("2G ");
        twoGRadioButton.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        twoGRadioButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
        twoGRadioButton.setVerticalTextPosition(javax.swing.SwingConstants.TOP);
        twoGRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                twoGRadioButtonActionPerformed(evt);
            }
        });

        gPanel.add(twoGRadioButton, new java.awt.GridBagConstraints());

        fullscaleGroup.add(sixGRadioButton);
        sixGRadioButton.setText("6G    ");
        sixGRadioButton.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        sixGRadioButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
        sixGRadioButton.setVerticalTextPosition(javax.swing.SwingConstants.TOP);
        sixGRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sixGRadioButtonActionPerformed(evt);
            }
        });

        gPanel.add(sixGRadioButton, new java.awt.GridBagConstraints());

        jLabel9.setText("  Max acceleration |a| = ");
        gPanel.add(jLabel9, new java.awt.GridBagConstraints());

        maxGLabel.setText("0.0");
        maxGLabel.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        maxGLabel.setMaximumSize(new java.awt.Dimension(110, 16));
        maxGLabel.setMinimumSize(new java.awt.Dimension(50, 16));
        maxGLabel.setPreferredSize(new java.awt.Dimension(70, 16));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        gPanel.add(maxGLabel, gridBagConstraints);

        jLabel7.setText("   ");
        gPanel.add(jLabel7, new java.awt.GridBagConstraints());

        connStatusLabel.setText("Not connected");
        connStatusLabel.setMaximumSize(new java.awt.Dimension(392, 16));
        connStatusLabel.setMinimumSize(new java.awt.Dimension(152, 16));
        connStatusLabel.setPreferredSize(new java.awt.Dimension(152, 16));
        connStatusLabel.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                connStatusLabelPropertyChange(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        gPanel.add(connStatusLabel, gridBagConstraints);

        jPanel4.add(gPanel);

        smoothPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        smoothPanel.setAlignmentX(0.0F);
        smoothPanel.setAlignmentY(0.0F);
        smoothPanel.setMaximumSize(new java.awt.Dimension(32767, 26));
        jLabel3.setText("Smooth data: ");
        smoothPanel.add(jLabel3);

        smoothGroup.add(noSmoothingButton);
        noSmoothingButton.setSelected(true);
        noSmoothingButton.setText("No ");
        noSmoothingButton.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        noSmoothingButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
        noSmoothingButton.setVerticalTextPosition(javax.swing.SwingConstants.TOP);
        noSmoothingButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                noSmoothingButtonActionPerformed(evt);
            }
        });

        smoothPanel.add(noSmoothingButton);

        smoothGroup.add(boxcarSmoothingButton);
        boxcarSmoothingButton.setText("Boxcar ");
        boxcarSmoothingButton.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        boxcarSmoothingButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
        boxcarSmoothingButton.setVerticalTextPosition(javax.swing.SwingConstants.TOP);
        boxcarSmoothingButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                boxcarSmoothingButtonActionPerformed(evt);
            }
        });

        smoothPanel.add(boxcarSmoothingButton);

        smoothGroup.add(triangleSmoothingButton);
        triangleSmoothingButton.setText("Triangle");
        triangleSmoothingButton.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        triangleSmoothingButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
        triangleSmoothingButton.setVerticalTextPosition(javax.swing.SwingConstants.TOP);
        triangleSmoothingButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                triangleSmoothingButtonActionPerformed(evt);
            }
        });

        smoothPanel.add(triangleSmoothingButton);

        jLabel4.setText("    Filter Width:");
        smoothPanel.add(jLabel4);

        filterWidthField.setColumns(2);
        filterWidthField.setText("5");
        filterWidthField.setAlignmentY(1.0F);
        filterWidthField.setMaximumSize(new java.awt.Dimension(32, 22));
        filterWidthField.setMinimumSize(new java.awt.Dimension(32, 22));
        filterWidthField.setValue(new Integer(5));
        filterWidthField.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                filterWidthFieldPropertyChange(evt);
            }
        });

        smoothPanel.add(filterWidthField);

        jPanel4.add(smoothPanel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        jPanel3.add(jPanel4, gridBagConstraints);

        buttonPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        buttonPanel.setAlignmentX(0.0F);
        buttonPanel.setAlignmentY(0.0F);
        buttonPanel.setMaximumSize(new java.awt.Dimension(536, 39));
        buttonPanel.setMinimumSize(new java.awt.Dimension(500, 39));
        buttonPanel.setPreferredSize(new java.awt.Dimension(500, 39));
        clearButton.setText("Clear Graph");
        clearButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearButtonActionPerformed(evt);
            }
        });

        buttonPanel.add(clearButton);

        calibrateButton.setText("Calibrate");
        calibrateButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                calibrateButtonActionPerformed(evt);
            }
        });

        buttonPanel.add(calibrateButton);

        sendDataButton.setText("Collect Data");
        sendDataButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendDataButtonActionPerformed(evt);
            }
        });

        buttonPanel.add(sendDataButton);

        pingButton.setText("Ping Spot");
        pingButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pingButtonActionPerformed(evt);
            }
        });

        buttonPanel.add(pingButton);

        reconnButton.setText("Reconnect");
        reconnButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reconnButtonActionPerformed(evt);
            }
        });

        buttonPanel.add(reconnButton);

        jLabel6.setText("   ");
        buttonPanel.add(jLabel6);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 1.0;
        jPanel3.add(buttonPanel, gridBagConstraints);

        dataPanel.setLayout(new java.awt.GridLayout(6, 2, 5, 5));

        dataPanel.setMinimumSize(new java.awt.Dimension(100, 109));
        label_velx.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        label_velx.setText("VelX:");
        dataPanel.add(label_velx);

        label_velx_data.setText("0");
        dataPanel.add(label_velx_data);

        label_vely.setText("VelY:");
        dataPanel.add(label_vely);

        label_vely_data.setText("0");
        dataPanel.add(label_vely_data);

        label_velz.setText("VelZ:");
        dataPanel.add(label_velz);

        label_velz_data.setText("0");
        dataPanel.add(label_velz_data);

        label_distx.setText("DistX:");
        dataPanel.add(label_distx);

        label_distx_data.setText("0");
        dataPanel.add(label_distx_data);

        label_disty.setText("DistY:");
        dataPanel.add(label_disty);

        label_disty_data.setText("0");
        dataPanel.add(label_disty_data);

        label_distz.setText("DistZ:");
        dataPanel.add(label_distz);

        label_distz_data.setText("0");
        dataPanel.add(label_distz_data);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.gridheight = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        jPanel3.add(dataPanel, gridBagConstraints);

        getContentPane().add(jPanel3, java.awt.BorderLayout.SOUTH);

        simulator.setMinimumSize(new java.awt.Dimension(10, 200));
        simulator.setPreferredSize(new java.awt.Dimension(600, 400));
        getContentPane().add(simulator, java.awt.BorderLayout.EAST);

        fileMenu.setText("File");
        aboutMenuItem.setText("About...");
        aboutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutMenuItemActionPerformed(evt);
            }
        });

        fileMenu.add(aboutMenuItem);

        fileMenu.add(jSeparator5);

        newMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_MASK));
        newMenuItem.setText("New");
        newMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newMenuItemActionPerformed(evt);
            }
        });

        fileMenu.add(newMenuItem);

        openMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        openMenuItem.setText("Open...");
        openMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openMenuItemActionPerformed(evt);
            }
        });

        fileMenu.add(openMenuItem);

        fileMenu.add(jSeparator3);

        closeMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_W, java.awt.event.InputEvent.CTRL_MASK));
        closeMenuItem.setText("Close");
        closeMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeMenuItemActionPerformed(evt);
            }
        });

        fileMenu.add(closeMenuItem);

        fileMenu.add(jSeparator4);

        saveMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        saveMenuItem.setText("Save");
        saveMenuItem.setEnabled(false);
        saveMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveMenuItemActionPerformed(evt);
            }
        });

        fileMenu.add(saveMenuItem);

        saveAsMenuItem.setText("Save As...");
        saveAsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveAsMenuItemActionPerformed(evt);
            }
        });

        fileMenu.add(saveAsMenuItem);

        fileMenu.add(jSeparator1);

        pagesetupMenuItem.setText("Page Setup...");
        pagesetupMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pagesetupMenuItemActionPerformed(evt);
            }
        });

        fileMenu.add(pagesetupMenuItem);

        printMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.CTRL_MASK));
        printMenuItem.setText("Print...");
        printMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printMenuItemActionPerformed(evt);
            }
        });

        fileMenu.add(printMenuItem);

        fileMenu.add(jSeparator2);

        quitMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.event.InputEvent.CTRL_MASK));
        quitMenuItem.setText("Quit");
        quitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                quitMenuItemActionPerformed(evt);
            }
        });

        fileMenu.add(quitMenuItem);

        jMenuBar1.add(fileMenu);

        windowMenu.setText("Windows");
        windowMenu.addMenuListener(new javax.swing.event.MenuListener() {
            public void menuCanceled(javax.swing.event.MenuEvent evt) {
            }
            public void menuDeselected(javax.swing.event.MenuEvent evt) {
            }
            public void menuSelected(javax.swing.event.MenuEvent evt) {
                windowMenuMenuSelected(evt);
            }
        });

        jMenuBar1.add(windowMenu);

        setJMenuBar(jMenuBar1);

        pack();
    }// </editor-fold>//GEN-END:initComponents
    
    private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed
        JOptionPane.showMessageDialog(this,
                "Sun SPOTs Telemetry Demo (Version " + version + ")\n\nA demo showing how to collect data from a SPOT and \nsend it to a desktop application to be displayed.\n\nAuthor: Ron Goldman, Sun Labs\nDate: " + versionDate,
                "About Telemetry Demo",
                JOptionPane.INFORMATION_MESSAGE,
                aboutIcon);
    }//GEN-LAST:event_aboutMenuItemActionPerformed
    
    private void pagesetupMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pagesetupMenuItemActionPerformed
        // Ask user for page format (e.g., portrait/landscape)
        pageFormat = printJob.pageDialog(pageFormat);
    }//GEN-LAST:event_pagesetupMenuItemActionPerformed
    
    private void reconnButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reconnButtonActionPerformed
        listener.reconnect();
    }//GEN-LAST:event_reconnButtonActionPerformed
    
    private void windowMenuMenuSelected(javax.swing.event.MenuEvent evt) {//GEN-FIRST:event_windowMenuMenuSelected
        windowMenu.removeAll();
        for (Enumeration e = windows.elements() ; e.hasMoreElements() ;) {
            JMenuItem it = windowMenu.add(((TelemetryFrame)e.nextElement()).getTitle());
            it.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    windowSelected(evt);
                }
            });
        }
    }//GEN-LAST:event_windowMenuMenuSelected
    
    private void xZoomButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_xZoomButton4ActionPerformed
        graphView.setZoomX(8);
    }//GEN-LAST:event_xZoomButton4ActionPerformed
    
    private void xZoomButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_xZoomButton3ActionPerformed
        graphView.setZoomX(4);
    }//GEN-LAST:event_xZoomButton3ActionPerformed
    
    private void xZoomButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_xZoomButton2ActionPerformed
        graphView.setZoomX(2);
    }//GEN-LAST:event_xZoomButton2ActionPerformed
    
    private void xZoomButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_xZoomButton1ActionPerformed
        graphView.setZoomX(1);
    }//GEN-LAST:event_xZoomButton1ActionPerformed
    
    private void filterWidthFieldPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_filterWidthFieldPropertyChange
        Integer fieldWidth = (Integer)filterWidthField.getValue();
        int w = fieldWidth.intValue();
        if (w <= 0) {
            w = 2;
        }
        if ((w % 2) == 0) {
            w++;
            filterWidthField.setValue(new Integer(w));
        }
        if (graphView != null) {
            graphView.setFilterWidth(w - 1);
        }
    }//GEN-LAST:event_filterWidthFieldPropertyChange
    
    private void connStatusLabelPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_connStatusLabelPropertyChange
        if (connStatusLabel.getText().startsWith("Connected")) {
            if (listener.is2GScale()) {
                twoGRadioButton.setSelected(true);
            } else {
                sixGRadioButton.setSelected(true);
            }
        }
    }//GEN-LAST:event_connStatusLabelPropertyChange
    
    private void formWindowActivated(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowActivated
        listener.setGUI(this);
        if (clearedData) {
            listener.clear();
        }
        if (listener.is2GScale()) {
            twoGRadioButton.setSelected(true);
        } else {
            sixGRadioButton.setSelected(true);
        }
    }//GEN-LAST:event_formWindowActivated
    
    private void formWindowDeactivated(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowDeactivated
        listener.setGUI(null);
    }//GEN-LAST:event_formWindowDeactivated
    
    private void triangleSmoothingButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_triangleSmoothingButtonActionPerformed
        graphView.setSmooth(true);
        graphView.setFiltertype(false);
    }//GEN-LAST:event_triangleSmoothingButtonActionPerformed
    
    private void boxcarSmoothingButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_boxcarSmoothingButtonActionPerformed
        graphView.setSmooth(true);
        graphView.setFiltertype(true);
    }//GEN-LAST:event_boxcarSmoothingButtonActionPerformed
    
    private void noSmoothingButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_noSmoothingButtonActionPerformed
        graphView.setSmooth(false);
    }//GEN-LAST:event_noSmoothingButtonActionPerformed
    
    private void yZoomButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_yZoomButton4ActionPerformed
        graphView.setZoomY(8);
    }//GEN-LAST:event_yZoomButton4ActionPerformed
    
    private void yZoomButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_yZoomButton3ActionPerformed
        graphView.setZoomY(4);
    }//GEN-LAST:event_yZoomButton3ActionPerformed
    
    private void yZoomButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_yZoomButton2ActionPerformed
        graphView.setZoomY(2);
    }//GEN-LAST:event_yZoomButton2ActionPerformed
    
    private void yZoomButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_yZoomButton1ActionPerformed
        graphView.setZoomY(1);
    }//GEN-LAST:event_yZoomButton1ActionPerformed
    
    private void quitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_quitMenuItemActionPerformed
        doQuit();
    }//GEN-LAST:event_quitMenuItemActionPerformed
    
    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        windows.remove(this);
        if (--numWindows <= 0) {
            doQuit();
        }
    }//GEN-LAST:event_formWindowClosed
    
    private void printMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_printMenuItemActionPerformed
        printJob.setPrintable(this, pageFormat);
        if (printJob.printDialog()) {
            try {
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                printJob.print();
            } catch(PrinterException pe) {
                System.out.println("Error printing: " + pe);
            } finally {
                setCursor(Cursor.getDefaultCursor());
            }
        }
    }//GEN-LAST:event_printMenuItemActionPerformed
    
    private void saveAsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveAsMenuItemActionPerformed
        doSaveAs();
    }//GEN-LAST:event_saveAsMenuItemActionPerformed
    
    private void saveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveMenuItemActionPerformed
        if (file == null) {
            doSaveAs();
        } else {
            doSave();
        }
    }//GEN-LAST:event_saveMenuItemActionPerformed
    
    private void closeMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeMenuItemActionPerformed
        setVisible(false);
        dispose();
    }//GEN-LAST:event_closeMenuItemActionPerformed
    
    private void openMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openMenuItemActionPerformed
        JFileChooser chooser;
        if (file != null) {
            chooser = new JFileChooser(file.getParent());
        } else {
            chooser = new JFileChooser(System.getProperty("user.dir"));
        }
        int returnVal = chooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            GraphView gView = new GraphView();
            if (gView.readTelemetryFile(file)) {
                frameGraph(file, gView);
            }
        }
    }//GEN-LAST:event_openMenuItemActionPerformed
    
    private void newMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newMenuItemActionPerformed
        frameGraph(null, new GraphView());
    }//GEN-LAST:event_newMenuItemActionPerformed
    
    private void xCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_xCheckBoxActionPerformed
        graphView.setShowX(xCheckBox.isSelected());
    }//GEN-LAST:event_xCheckBoxActionPerformed
    
    private void yCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_yCheckBoxActionPerformed
        graphView.setShowY(yCheckBox.isSelected());
    }//GEN-LAST:event_yCheckBoxActionPerformed
    
    private void zCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zCheckBoxActionPerformed
        graphView.setShowZ(zCheckBox.isSelected());
    }//GEN-LAST:event_zCheckBoxActionPerformed
    
    private void gCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gCheckBoxActionPerformed
        graphView.setShowG(gCheckBox.isSelected());
    }//GEN-LAST:event_gCheckBoxActionPerformed
    
    private void pingButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pingButtonActionPerformed
        listener.doPing();
    }//GEN-LAST:event_pingButtonActionPerformed
    
    private void sixGRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sixGRadioButtonActionPerformed
        listener.doSetScale(6);
    }//GEN-LAST:event_sixGRadioButtonActionPerformed
    
    private void twoGRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_twoGRadioButtonActionPerformed
        listener.doSetScale(2);
    }//GEN-LAST:event_twoGRadioButtonActionPerformed
    
    private void calibrateButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_calibrateButtonActionPerformed
        listener.doCalibrate();
    }//GEN-LAST:event_calibrateButtonActionPerformed
    
    private void sendDataButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sendDataButtonActionPerformed
        sendData = !sendData;
        listener.doSendData(sendData, graphView);
        sendDataButton.setText(sendData ? "Stop Data" : "Collect Data");
        saveMenuItem.setEnabled(true);
        clearedData = false;
    }//GEN-LAST:event_sendDataButtonActionPerformed
    
    private void clearButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearButtonActionPerformed
        if (saveMenuItem.isEnabled()) {
            int n = JOptionPane.showConfirmDialog(this, "The current data has not been saved to a file. " +
                    "Do you wish to delete it?",
                    "Data Not Saved",
                    JOptionPane.YES_NO_OPTION);
            if (n != JOptionPane.YES_OPTION) {
                return;                             // cancel the Clear command
            }
        }
        
        if (sendData) {                             // if currently sending data, then stop
            listener.doSendData(false, graphView);
        }
        graphView.clearGraph();
        listener.clear();
        clearedData = true;
        saveMenuItem.setEnabled(sendData);
    }//GEN-LAST:event_clearButtonActionPerformed
    
    /**
     * Create an initial new window and display it.
     *
     * @param args the command line arguments: first should be serial port to use to connect to base station SPOT
     */
    public static void main(String args[]) {
        if (args.length > 0) {
            System.setProperty("SERIAL_PORT", args[0]);         // set things up to contact the base station
        }
        TelemetryFrame.frameGraph(null, new GraphView());       // create an empty window to collect telemetry
    }
    
    public void set_label_distx_data(String data) {
        label_distx_data.setText(data);
    }
    public void set_label_disty_data(String data) {
        label_disty_data.setText(data);
    }
    public void set_label_distz_data(String data) {
        label_distz_data.setText(data);
    }
    public void set_label_velx_data(String data) {
        label_velx_data.setText(data);
    }
    public void set_label_vely_data(String data) {
        label_vely_data.setText(data);
    }
    public void set_label_velz_data(String data) {
        label_velz_data.setText(data);
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JPanel axisPanel;
    private javax.swing.JRadioButton boxcarSmoothingButton;
    private javax.swing.JPanel buttonPanel;
    private javax.swing.JButton calibrateButton;
    private javax.swing.JButton clearButton;
    private javax.swing.JMenuItem closeMenuItem;
    private javax.swing.JLabel connStatusLabel;
    private javax.swing.JPanel dataPanel;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JFormattedTextField filterWidthField;
    private javax.swing.ButtonGroup fullscaleGroup;
    private javax.swing.JCheckBox gCheckBox;
    private javax.swing.JPanel gPanel;
    private javax.swing.JScrollPane graphViewScrollPane;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JLabel label_distx;
    private javax.swing.JLabel label_distx_data;
    private javax.swing.JLabel label_disty;
    private javax.swing.JLabel label_disty_data;
    private javax.swing.JLabel label_distz;
    private javax.swing.JLabel label_distz_data;
    private javax.swing.JLabel label_velx;
    private javax.swing.JLabel label_velx_data;
    private javax.swing.JLabel label_vely;
    private javax.swing.JLabel label_vely_data;
    private javax.swing.JLabel label_velz;
    private javax.swing.JLabel label_velz_data;
    private javax.swing.JLabel maxGLabel;
    private javax.swing.JMenuItem newMenuItem;
    private javax.swing.JRadioButton noSmoothingButton;
    private javax.swing.JMenuItem openMenuItem;
    private javax.swing.JMenuItem pagesetupMenuItem;
    private javax.swing.JButton pingButton;
    private javax.swing.JMenuItem printMenuItem;
    private javax.swing.JMenuItem quitMenuItem;
    private javax.swing.JButton reconnButton;
    private javax.swing.JMenuItem saveAsMenuItem;
    private javax.swing.JMenuItem saveMenuItem;
    private javax.swing.JButton sendDataButton;
    private javax.swing.JPanel simulator;
    private javax.swing.JRadioButton sixGRadioButton;
    private javax.swing.ButtonGroup smoothGroup;
    private javax.swing.JPanel smoothPanel;
    private javax.swing.JRadioButton triangleSmoothingButton;
    private javax.swing.JRadioButton twoGRadioButton;
    private javax.swing.JMenu windowMenu;
    private javax.swing.JCheckBox xCheckBox;
    private javax.swing.JRadioButton xZoomButton1;
    private javax.swing.JRadioButton xZoomButton2;
    private javax.swing.JRadioButton xZoomButton3;
    private javax.swing.JRadioButton xZoomButton4;
    private javax.swing.ButtonGroup xZoomGroup;
    private javax.swing.JCheckBox yCheckBox;
    private javax.swing.JRadioButton yZoomButton1;
    private javax.swing.JRadioButton yZoomButton2;
    private javax.swing.JRadioButton yZoomButton3;
    private javax.swing.JRadioButton yZoomButton4;
    private javax.swing.ButtonGroup yZoomGroup;
    private javax.swing.JPanel y_axisPanel;
    private javax.swing.JCheckBox zCheckBox;
    // End of variables declaration//GEN-END:variables
    
}
