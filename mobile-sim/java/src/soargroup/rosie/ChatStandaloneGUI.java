package soargroup.rosie;

import javax.swing.JFrame;

import edu.umich.rosie.language.ChatPanel;
import soargroup.rosie.LcmMessagePasser;


public class ChatStandaloneGUI extends JFrame
{
    public ChatStandaloneGUI()
    {
		super("Rosie Chat");
		
    	this.setSize(800, 400);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        LcmMessagePasser messagePasser = new LcmMessagePasser("instructor");
        
    	ChatPanel chat = new ChatPanel(null, this, messagePasser);
    	this.add(chat);

    	//CommandPanel commandPanel = new CommandPanel(soarAgent);
    	
    	//JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, chat, commandPanel);
    	//splitPane.setDividerLocation(400);
    	//this.add(splitPane);
    	
    	this.setVisible(true);
    }

    public static void main(String[] args) {
        new ChatStandaloneGUI();
    }
}
