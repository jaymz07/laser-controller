/* Java Program for generating analog X,Y signals for a laser projector using the analog soundcard output.
 * Drawing on graphical window reflects the laser output.
 * This class defines a window, gets some points from mouse input and passes these points to a seperate
 * thread defined by LaserOutputThread.java, which handles all audio output.
 */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;
import java.io.*;
import java.net.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class GraphicalLaserOutput implements MouseListener, MouseMotionListener, KeyListener {

    JFrame frame;
    DrawPanel drawPanel;
    ControlPanel controlPanelWindow;
    SinePanel sinePanelWindow;
    FilterPanel filterPanelWindow;

    Graphics page;

    int width = 500;
    int height = 500;

    double sineIncrement = 0.01;
    int sineSamples = 200;
    int rotationSpeed = 50;

    ArrayList<Point> drawPoints = new ArrayList<Point>();

    LaserOutputThread audioThread;

    String mode = "draw";
    double sinPhase = 0.0;

    boolean rotating=false;

    public void run() {

        /*---------some initial points--------------
        drawPoints.add(new Point(20,20));
        drawPoints.add(new Point(40,40));
        drawPoints.add(new Point(60,60));
        drawPoints.add(new Point(60,120));
        */

        /*--------Initialize seperate thread for audio output with an initial array to output--------------*/
        audioThread = new LaserOutputThread(drawPoints);

        /*---------Make window-----------*/
        frame = new JFrame("Parametric Signal Generator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        controlPanelWindow = new ControlPanel("Control Panel");
        controlPanelWindow.pack();
        controlPanelWindow.setLocation(500,0);
        controlPanelWindow.setVisible(true);

        sinePanelWindow = new SinePanel("Sine Animation Controls");
        sinePanelWindow.pack();
        sinePanelWindow.setLocation(500,250);
        sinePanelWindow.setVisible(false);
        
<<<<<<< HEAD
        //Instantiate window for controlling the crossover filter feature.
        filterPanelWindow = new FilterPanel("Signal Filtering");
        filterPanelWindow.pack();
        filterPanelWindow.setLocation(500,500);
        filterPanelWindow.setVisible(true);
=======
        filterPanelWindow = new FilterPanel("Signal Filtering");
        filterPanelWindow.pack();
        filterPanelWindow.setLocation(500,250);
        filterPanelWindow.setVisible(false);
>>>>>>> 46c1bacccd937386b79e6a48f3db1ebe731abb6e

        //drawPanel actually writes to the screen. frame is just a container
        drawPanel = new DrawPanel();
        frame.getContentPane().add(BorderLayout.CENTER, drawPanel);

        //window options
        frame.setVisible(true);
        frame.setResizable(false);
        frame.setSize(width,height);

        //Allow keboard and mouse input for the window
        //also tells frame to call method overrides for the listeners
        frame.addMouseListener(this);
        frame.addMouseMotionListener( this );
        frame.addKeyListener ( this ) ;

        audioThread.start();

        frame.repaint();
    }

    /*--------Nested class defining object to be painted to screen.-------
    	  Simply overrides paintComponent() originally defined in JPanel*/
    class DrawPanel extends JPanel {

        //Overrides method defined in JPanel superclass. Called when repaint() is called from frame.
        public void paintComponent(Graphics g) {
            page = g;
            //page = iOut.getGraphics();

            page.setColor(Color.BLACK);
            page.fillRect(0,0,width,height);

            page.setColor(Color.GREEN);

            int pointRadius = 10;

            ArrayList<Point> points = null;

            if(mode == "draw")
                points = drawPoints;
            if(mode == "sine") {
                points = sinGraphScreen(0.0);
            }

            if(points.size() > 0) {
                Point pt = points.get(0);
                page.fillOval((int)(pt.x) - pointRadius/2,(int)(pt.y) - pointRadius/2,pointRadius,pointRadius);
                for(int i =1; i< points.size(); i++) {
                    Point ptLast = pt;
                    pt = points.get(i);
                    page.fillOval((int)(pt.x) - pointRadius/2,(int)(pt.y) - pointRadius/2,pointRadius,pointRadius);
                    page.drawLine((int)(ptLast.x),(int)(ptLast.y),(int)(pt.x),(int)(pt.y));

                }
                if(mode == "draw") {
                    audioThread.lightning= false;
                    ArrayList<Point> ptsNormalized = normalizePoints(points);
                    setNewDrawPath(ptsNormalized);
                    System.out.println(points.size() + " points drawn.");
                }
            }
            if(mode == "sine")
            {
                ArrayList<Point> signalOut = new ArrayList<Point>();

                for(double phase = sineIncrement; phase < 2*Math.PI; phase += sineIncrement)
                    for(Point point: sinSignal(phase))
                        signalOut.add(point);
                setNewDrawPath(signalOut);
                sinePanelWindow.setVisible(true);
                System.out.println("Audio thread length: "+signalOut.size());
            }
            else
                sinePanelWindow.setVisible(false);
        }
    }

//Add labels to JSlider class
    public class LabelSlider extends JSlider {
        public String label;
        public LabelSlider(String label, int orientation, int min, int max, int value) {
            super(orientation, min, max, value);
            super.setPaintLabels(true);
            this.label = label;
        }
    }

//-----------Defines control window---------------------------------
    public class ControlPanel extends Frame implements WindowListener, ActionListener, ChangeListener, ItemListener {
        JFileChooser fileChooser = new JFileChooser();
        public ControlPanel(String title) {
            super(title);
            setLayout(new FlowLayout());
            addWindowListener(this);

            //make buttons
            ArrayList<Button> buttons = new ArrayList<Button>();
	    
            buttons.add(new Button("Sine Mode"));
            buttons.add(new Button("Draw Mode"));
            buttons.add(new Button("Open Drawing"));
	    
            JPanel buttonPanel = new JPanel(new GridLayout(0,1));
            for(Button b : buttons) {
                b.addActionListener(this);
                buttonPanel.add(b);
            }
            add(buttonPanel);

            //make checkboxes
            ArrayList<JCheckBox> cBoxes = new ArrayList<JCheckBox>();
            cBoxes.add(new JCheckBox("Rotate"));
            cBoxes.get(cBoxes.size()-1).setName("Rotate");
            cBoxes.add(new JCheckBox("Invert"));
            cBoxes.get(cBoxes.size()-1).setName("Invert");
            
            for(JCheckBox b : cBoxes) {
                b.addItemListener(this);
                b.setAlignmentY(Component.BOTTOM_ALIGNMENT);
                add(b);
            }

            //Make sliders
            ArrayList<LabelSlider> sliders = new ArrayList<LabelSlider>();
	    
            sliders.add(new LabelSlider("Rotation Speed",JSlider.VERTICAL,1,10,5));
	    sliders.add(new LabelSlider("Resamples",JSlider.VERTICAL,1,15,5));
	    
            JPanel sliderPanel = new JPanel(new GridLayout(1,0));
            for(LabelSlider s : sliders) {
                s.addChangeListener(this);
                sliderPanel.add(s);
                JLabel label = new JLabel(s.label,JLabel.CENTER);
                label.setAlignmentX(s.getAlignmentX());
                label.setAlignmentY(Component.BOTTOM_ALIGNMENT);
                sliderPanel.add(label);
            }
            add(sliderPanel);
        }

        public void windowClosing(WindowEvent e) {
            dispose();
            System.exit(0);
        }
        //Neccesary overides to implement appropriate listeners
        public void windowOpened(WindowEvent e) {}
        public void windowActivated(WindowEvent e) {}
        public void windowIconified(WindowEvent e) {}
        public void windowDeiconified(WindowEvent e) {}
        public void windowDeactivated(WindowEvent e) {}
        public void windowClosed(WindowEvent e) {}

        //Button events
        public void actionPerformed(ActionEvent e) {
            String actionString = e.getActionCommand();
            System.out.println(actionString);
            if(actionString == "Sine Mode")
                mode = "sine";
            else if(actionString == "Draw Mode")
                mode = "draw";
            else if(actionString == "Open Drawing") {
                openDrawingFromPrompt();
            }
            frame.repaint();
        }

        //Sider Events
        public void stateChanged(ChangeEvent e) {
            LabelSlider source = (LabelSlider)e.getSource();
            if (!source.getValueIsAdjusting()) {
                if(source.label.equals("Rotation Speed"))
                    rotationSpeed = (int)source.getValue();
		if(source.label.equals("Resamples"))
		    audioThread.maxDist = 0.1/(int)source.getValue();
            }
            System.out.println(source.label + " = " + (int)source.getValue());
            frame.repaint();
        }

        //Check box events
        public void itemStateChanged(ItemEvent e) {
            JCheckBox source = (JCheckBox)e.getItemSelectable();
            if(source.getName().equals("Rotate")) {
                if(e.getStateChange() == 2)
                    rotating = false;
                else
                    rotating = true;
            }
            if(source.getName().equals("Invert")) {
                if(e.getStateChange() == 2)
                    audioThread.invertSignal = false;
                else
                    audioThread.invertSignal = true;
                audioThread.recalc=true;
            }
            frame.repaint();
        }
        
        //Load drawing from file selected by user.
        public void openDrawingFromPrompt() {
            int returnVal = fileChooser.showOpenDialog(this);
            if(returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
		Scanner sc = null;
		try {
		  sc = new Scanner(file);
		}
		catch(FileNotFoundException e) { System.out.println("File Does not Exist!"); return; }
		System.out.println("Reading from file: " + file);
                ArrayList<Point> ptsIn = new ArrayList<Point>();
                double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE, minX = Double.MAX_VALUE, minY= Double.MAX_VALUE;
                while(sc.hasNextLine()) {
                    String line = sc.nextLine();
                    //Skip comments
                    if(line.charAt(0) == '#')
		      continue;
		    //Compatability with comma separated files and tab separated files.
		    String [] commaSplit = line.split(",");
		    String [] tabSplit   = line.split("\t");
                    String [] vals= null;
                    if(commaSplit.length > tabSplit.length)
		      vals = commaSplit;
		    else
		      vals = tabSplit;
                    if(vals.length != 2) {
			System.out.println("Invalid File! Must be tab separated coordinate points.");
                        break;
                    }
                    double x = Double.parseDouble(vals[0]);
                    double y = Double.parseDouble(vals[1]);
                    minX = Math.min(minX,x);
                    minY = Math.min(minY,y);
                    maxX = Math.max(maxX,x);
                    maxY = Math.max(maxY,y);
                    ptsIn.add(new Point(x,y));
                }
                double range = Math.max(maxX- minX, maxY- minY);
                for(Point p : ptsIn) {
                    p.x = (p.x - minX)/range*width;
                    p.y = height - (p.y - minY)/range*height;
                }
                drawPoints = ptsIn;
            }
        }

    }

//----------------Defines window for controlling sine mode---------------------------------
    public class SinePanel extends Frame implements WindowListener, ActionListener, ChangeListener, ItemListener {
        ArrayList<Button> buttons = new ArrayList<Button>();
        ArrayList<JCheckBox> cBoxes = new ArrayList<JCheckBox>();
        ArrayList<LabelSlider> sliders = new ArrayList<LabelSlider>();


        public SinePanel(String title) {
            super(title);
            setLayout(new FlowLayout());
            addWindowListener(this);

            //buttons.add(new Button("Button!"));
            for(Button b : buttons) {
                b.addActionListener(this);
                add(b);
            }
            JPanel sliderPanel = new JPanel(new GridLayout(1,0));

            sliders.add(new LabelSlider("Speed",JSlider.VERTICAL,1,10,6));
            sliders.add(new LabelSlider("Samples",JSlider.VERTICAL,50,500,200));

            for(LabelSlider s : sliders) {
                s.addChangeListener(this);
                sliderPanel.add(s);
                JLabel label = new JLabel(s.label,JLabel.CENTER);
                label.setAlignmentX(s.getAlignmentX());
                label.setAlignmentY(Component.BOTTOM_ALIGNMENT);
                sliderPanel.add(label);
            }
            add(sliderPanel);

            cBoxes.add(new JCheckBox("Lightning"));
            cBoxes.get(cBoxes.size()-1).setName("Lightning");

            for(JCheckBox b : cBoxes)  {
                b.addItemListener(this);
                b.setSelected(false);
                add(b);
            }
        }
        //Button events:
        public void actionPerformed(ActionEvent e) { }

        public void windowClosing(WindowEvent e) {
            System.exit(0);
        }
        //Neccesary overides for "implements WindowListener"
        public void windowOpened(WindowEvent e) {}
        public void windowActivated(WindowEvent e) {}
        public void windowIconified(WindowEvent e) {}
        public void windowDeiconified(WindowEvent e) {}
        public void windowDeactivated(WindowEvent e) {}
        public void windowClosed(WindowEvent e) {}

        //Slider events
        public void stateChanged(ChangeEvent e) {
            LabelSlider source = (LabelSlider)e.getSource();
            if (!source.getValueIsAdjusting()) {
                if(source.label.equals("Speed"))
                    sineIncrement = (double)source.getValue()/1000;
                if(source.label.equals("Samples"))
                    sineSamples = (int)source.getValue();
                System.out.println((int)source.getValue());
                frame.repaint();
            }
            System.out.println(source.label);
        }

        //Check box events
        public void itemStateChanged(ItemEvent e) {
            JCheckBox source = (JCheckBox)e.getItemSelectable();
            if(source.getName().equals("Lightning")) {
                if(e.getStateChange() == 2)
                    audioThread.lightning = false;
                else
                    audioThread.lightning = true;
            }
        }

    }
    
    public class FilterPanel extends Frame implements WindowListener, ActionListener, ChangeListener, ItemListener {
        ArrayList<Button> buttons = new ArrayList<Button>();
        ArrayList<JCheckBox> cBoxes = new ArrayList<JCheckBox>();
        ArrayList<LabelSlider> slidersX = new ArrayList<LabelSlider>();
        ArrayList<LabelSlider> slidersY = new ArrayList<LabelSlider>();


        public FilterPanel(String title) {
            super(title);
            setLayout(new FlowLayout());
            addWindowListener(this);

            //buttons.add(new Button("Button!"));
            for(Button b : buttons) {
                b.addActionListener(this);
                add(b);
            }
            JPanel sliderPanelX = new JPanel(new GridLayout(1,0));
            JPanel sliderPanelY = new JPanel(new GridLayout(1,0));

            slidersX.add(new LabelSlider("Low Freq(X)",JSlider.VERTICAL,0,100,100));
            slidersX.add(new LabelSlider("High Freq(X)",JSlider.VERTICAL,0,100,100));
            slidersX.add(new LabelSlider("Crossover(X)",JSlider.VERTICAL,10,1000,200));
            
            slidersY.add(new LabelSlider("Low Freq(Y)",JSlider.VERTICAL,0,100,100));
            slidersY.add(new LabelSlider("High Freq(Y)",JSlider.VERTICAL,0,100,100));
            slidersY.add(new LabelSlider("Crossover(Y)",JSlider.VERTICAL,10,1000,200));

            for(LabelSlider s : slidersX) {
                s.addChangeListener(this);
                sliderPanelX.add(s);
                JLabel label = new JLabel(s.label,JLabel.CENTER);
                label.setAlignmentX(s.getAlignmentX());
                label.setAlignmentY(Component.BOTTOM_ALIGNMENT);
                sliderPanelX.add(label);
            }
            add(sliderPanelX);
            for(LabelSlider s : slidersY) {
                s.addChangeListener(this);
                sliderPanelY.add(s);
                JLabel label = new JLabel(s.label,JLabel.CENTER);
                label.setAlignmentX(s.getAlignmentX());
                label.setAlignmentY(Component.BOTTOM_ALIGNMENT);
                sliderPanelY.add(label);
                sliderPanelY.setAlignmentY(Component.BOTTOM_ALIGNMENT);
            }
            add(sliderPanelY);
	    
	    cBoxes.add(new JCheckBox("Enable Filter"));
            cBoxes.get(cBoxes.size()-1).setName("Enable Filter");
	    
            for(JCheckBox b : cBoxes)  {
                b.addItemListener(this);
                b.setSelected(false);
                add(b);
            }
        }
        //Button events:
        public void actionPerformed(ActionEvent e) { }

        public void windowClosing(WindowEvent e) {
            System.exit(0);
        }
        //Neccesary overides for "implements WindowListener"
        public void windowOpened(WindowEvent e) {}
        public void windowActivated(WindowEvent e) {}
        public void windowIconified(WindowEvent e) {}
        public void windowDeiconified(WindowEvent e) {}
        public void windowDeactivated(WindowEvent e) {}
        public void windowClosed(WindowEvent e) {}

        //Slider events
        public void stateChanged(ChangeEvent e) {
            LabelSlider source = (LabelSlider)e.getSource();
            if (!source.getValueIsAdjusting()) {
                if(source.label.equals("Low Freq(X)"))
                    audioThread.lowPassX = (double)source.getValue()/100;
                if(source.label.equals("High Freq(X)"))
                    audioThread.highPassX = (double)source.getValue()/100;
                if(source.label.equals("Crossover(X)"))
                    audioThread.filterFreqX = (double)source.getValue();
                if(source.label.equals("Low Freq(Y)"))
                    audioThread.lowPassY = (double)source.getValue()/100;
                if(source.label.equals("High Freq(Y)"))
                    audioThread.highPassY = (double)source.getValue()/100;
                if(source.label.equals("Crossover(Y)"))
                    audioThread.filterFreqY = (double)source.getValue();
                System.out.println((int)source.getValue());
                frame.repaint();
            }
            System.out.println(source.label);
        }

        //Check box events
        public void itemStateChanged(ItemEvent e) {
            JCheckBox source = (JCheckBox)e.getItemSelectable();
            if(source.getName().equals("Enable Filter")) {
                if(e.getStateChange() == 2)
                    audioThread.filter = false;
                else
                    audioThread.filter = true;
                audioThread.recalc=true;
            }
        }

    }

    /*---------Generate Sinusoid Graph-----------*/
    public ArrayList<Point> sinGraphScreen(double phase)
    {
        int numPoints = 200;
        double numPeriods = 2.0;
        ArrayList<Point> out = new ArrayList<Point>();
        for(int i = 0 ; i<=numPoints; i++)
        {
            double x = ((double)i) / numPoints;
            out.add(new Point(5+x*(width-10),5 + (1+Math.sin((x-sinPhase)*2*Math.PI*numPeriods))*(height - 10)/2));
        }
        return out;
    }
    public ArrayList<Point> sinSignal(double phase)
    {
        int numPoints = sineSamples;
        double numPeriods = 1.0;
        ArrayList<Point> out = new ArrayList<Point>();
        for(int i = 0 ; i<numPoints; i++)
        {
            double x = ((double)i) / numPoints;
            out.add(new Point(2*x-1, Math.sin((x-phase)*2*Math.PI*numPeriods)));
        }
        for(int i = numPoints ; i>=0; i--)
        {
            double x = ((double)i) / numPoints;
            out.add(new Point(2*x-1, Math.sin((x-phase)*2*Math.PI*numPeriods)));
        }
        return out;
    }

    //--------------Rotation animation----------------
    public ArrayList<Point> getRotatedPoints(ArrayList<Point> input, double angle) {
        ArrayList<Point> out = new ArrayList<Point>();
        for(int i =0; i< input.size(); i++)
            out.add(new Point(input.get(i).x*Math.cos(angle), input.get(i).y));
        return out;
    }

    public ArrayList<Point> rotationAnimation(ArrayList<Point> input) {
        ArrayList<Point> out = new ArrayList<Point>();
        double rotationIncrement = (double)rotationSpeed/10000/audioThread.maxDist;
        for(double angle = 0.0; angle < Math.PI*2; angle += rotationIncrement) {
	    ArrayList<Point> rotated = getRotatedPoints(input,angle);
            for(Point pt : rotated)
                out.add(pt);
	}
        return out;
    }

//----------Update Other thread with current drawing
    public void setNewDrawPath(ArrayList<Point> ptsToDraw) {
	if(rotating && mode.equals("draw")) {
	  audioThread.points = rotationAnimation(ptsToDraw);
	  audioThread.recalc=true;
	}
	else {
	  audioThread.points = ptsToDraw;
	  audioThread.recalc=true;
	}
	//change this number so that audio thread knows to interrupt current buffer (kinda ghetto...)
        audioThread.outNumber++;
    }

    public ArrayList<Point> normalizePoints(ArrayList<Point> in) {
        ArrayList<Point> out = new ArrayList<Point>();
        for(Point point : in)
            out.add(new Point((2*point.x - width)/width, (2*point.y - height)/height));
        return out;
    }

    /*----------Main function. Calls constructor of whole class-----------*/
    public static void main (String[] args ) {
        for(String s : args)
            System.out.println(s);
        (new GraphicalLaserOutput()).run() ;
    }

    /*----------Keyboard Input. Event generated function calls.----------------------*/

    public void keyTyped ( KeyEvent e ) { }
    public void keyPressed ( KeyEvent e) {
        //System.out.println("KEY");
        if(e.getKeyCode()== KeyEvent.VK_C) {

            drawPoints = new ArrayList<Point>();
        }
        if(e.getKeyCode()== KeyEvent.VK_BACK_SPACE) {

            if(drawPoints.size() > 0)
                drawPoints.remove(drawPoints.size() - 1);
        }
        if(e.getKeyCode()== KeyEvent.VK_Q) {

            drawPoints=new ArrayList<Point>();
        }
        if(e.getKeyCode()== KeyEvent.VK_S) {
            mode = "sine";
        }
        if(e.getKeyCode()== KeyEvent.VK_D) {
            mode = "draw";
        }
        frame.repaint();
    }
    public void keyReleased ( KeyEvent e ) {}

    /*-----------Mouse Input. Event generated function calls------------------------*/
    public void mouseEntered( MouseEvent e ) {
        // called when the pointer enters the applet's rectangular area
    }
    public void mouseExited( MouseEvent e ) {}  // called when the pointer leaves the applet's rectangular area
    public void mouseClicked( MouseEvent e ) {}
    public void mousePressed( MouseEvent e ) {  // called after a button is pressed down
        int xP=e.getX();
        int yP=e.getY();
        drawPoints.add(new Point(xP,yP));
        frame.repaint();
    }
    public void mouseReleased( MouseEvent e ) {}  // called after a button is released
    public void mouseMoved( MouseEvent e )    {}  // called during motion when no buttons are down
    public void mouseDragged( MouseEvent e )  {   // called during motion with buttons down
        int xP=e.getX();
        int yP=e.getY();
        if(xP >= 0 && xP < width && yP >= 0 && yP < height)  //Since dragging can occur outside window
            if(drawPoints.size() == 0 || Math.abs(drawPoints.get(drawPoints.size()-1).x - xP) > 1 || Math.abs(drawPoints.get(drawPoints.size()-1).y - yP) > 1)
                drawPoints.add(new Point(xP,yP));
        frame.repaint();
    }
}


