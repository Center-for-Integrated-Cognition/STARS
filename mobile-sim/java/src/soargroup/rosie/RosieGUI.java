package soargroup.rosie;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JSplitPane;

import soargroup.rosie.actuation.MobileActuationConnector;
import soargroup.rosie.perception.MobilePerceptionConnector;
import edu.umich.rosie.AgentMenu;
import edu.umich.rosie.language.ChatPanel;
import edu.umich.rosie.language.InternalMessagePasser;
//import edu.umich.rosie.language.ScriptPanel;
import edu.umich.rosie.language.InstructorMessagePanel;
import edu.umich.rosie.language.LanguageConnector;
import edu.umich.rosie.soar.SoarClient;
import april.util.GetOpt;
import april.util.StringUtil;

public class RosieGUI extends JFrame
{
	private SoarClient soarClient;

	private JButton startStopButton;
	private JButton stopRobotButton;

	private MobilePerceptionConnector perception;
	private MobileActuationConnector actuation;
	private LanguageConnector language;

    public RosieGUI(Properties props)
    {
		super("Rosie Chat");

    	//this.setSize(800, 650);
    	this.setSize(1000, 450);
    	getContentPane().setLayout(new BoxLayout(this.getContentPane(), BoxLayout.LINE_AXIS));
        addWindowListener(new WindowAdapter() {
        	public void windowClosing(WindowEvent w) {
                System.out.println("WINDOW CLOSING");
        		soarClient.kill();
        	}
     	});
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    	soarClient = new SoarClient(props);

    	actuation = new MobileActuationConnector(soarClient, props);
    	soarClient.addConnector(actuation);

    	perception = new MobilePerceptionConnector(soarClient, props);
    	soarClient.addConnector(perception);
    	
    	InternalMessagePasser messagePasser = new InternalMessagePasser();
    	
    	language = new LanguageConnector(soarClient, props, messagePasser);
    	soarClient.addConnector(language);

    	ChatPanel chat = new ChatPanel(soarClient, this, messagePasser);

    	setupMenu();

    	soarClient.createAgent();

    	add(chat);

		//if(props.getProperty("script-file") != null){
		//	ScriptPanel scriptPanel = new ScriptPanel(chat);
		//	scriptPanel.loadScriptFile(props.getProperty("script-file"));
		//	add(scriptPanel);
		//} else {
			add(new InstructorMessagePanel(chat, props));
		//}


//    	CommandPanel commandPanel = new CommandPanel(soarClient);
//
//    	JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, chat, commandPanel);
//    	splitPane.setDividerLocation(400);
//    	this.add(splitPane);

    	this.setVisible(true);
    }

    private void setupMenu(){
    	JMenuBar menuBar = new JMenuBar();

    	startStopButton = new JButton("START");
        startStopButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent arg0) {
				if(soarClient.isRunning()){
					startStopButton.setText("START");
					soarClient.stop();
				} else {
					startStopButton.setText("STOP");
					soarClient.start();
				}
			}
        });
        menuBar.add(startStopButton);

    	menuBar.add(new AgentMenu(soarClient));

    	stopRobotButton = new JButton("STOP");
    	stopRobotButton.setBackground(Color.red);
        stopRobotButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent arg0) {
				actuation.processStopCommand(null);
			}
        });
        menuBar.add(stopRobotButton);

    	this.setJMenuBar(menuBar);
    }


    public static void main(String[] args) {

        GetOpt opts = new GetOpt();

        opts.addBoolean('h', "help", false, "Show this help screen");
        opts.addBoolean('d', "debug", true, "Show the soar debugger");
        opts.addString('c', "config", null, "The config file for Rosie");

        if (!opts.parse(args)) {
            System.err.println("ERR: Error parsing args - "+opts.getReason());
            System.exit(1);
        }
        if (opts.getBoolean("help")) {
            opts.doHelp();
            System.exit(0);
        }
		ArrayList<String> xargs = opts.getExtraArgs();
		if(xargs.size() == 0){
			System.err.println("Requires the rosie agent config file as an argument");
			System.exit(1);
		}
		String configFile = xargs.get(0);

        // Load the properties file
        Properties props = new Properties();
        try {
			props.load(new FileReader(configFile));
		} catch (IOException e) {
			System.out.println("File not found: " + configFile);
			e.printStackTrace();
			return;
		}

        new RosieGUI(props);
    }
}
