package de.felixbruns.jotify.gui.swing;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;

import de.felixbruns.jotify.gui.JotifyApplication;
import de.felixbruns.jotify.gui.swing.components.JotifyButton;
import de.felixbruns.jotify.gui.swing.components.JotifyCheckBox;
import de.felixbruns.jotify.gui.swing.components.JotifyCloseButton;
import de.felixbruns.jotify.gui.swing.panels.JotifyLoginPanel;
import de.felixbruns.jotify.gui.util.JotifyLoginCredentials;

@SuppressWarnings("serial")
public class JotifyLoginDialog extends JFrame implements MouseListener, MouseMotionListener {
	/* Mouse position for dragging. */
	private Point mouse;
	
	/* Components. */
	private JPanel         panel;
	private JButton        closeButton;
	private JLabel         usernameLabel;
	private JTextField     usernameField;
	private JLabel         passwordLabel;
	private JPasswordField passwordField;
	private JButton        signInButton;
	private JCheckBox      rememberMeCheckBox;
	private JLabel         loadLabel;
	private JLabel         messageLabel;
	
	/* Semaphore stuff. */
	private Semaphore semaphore;
	
	private static JotifyLoginDialog dialog;
	
	/* Statically create a login dialog. */
	static {
		dialog = new JotifyLoginDialog();
	}
	
	/* Show a login dialog. */
	public static void showDialog(){
		/* Show dialog. */
		dialog.setVisible(true);
		
		/* Center dialog on screen. */
		dialog.setLocationRelativeTo(null);
	}
	
	public static void hideDialog(){
		/* Close dialog. */
		dialog.setVisible(false);
	}
	
	public static void showLoader(){
		dialog.loadLabel.setVisible(true);
	}
	
	public static void hideLoader(){		
		dialog.loadLabel.setVisible(false);
	}
	
	public static void showErrorMessage(String message){
		dialog.messageLabel.setForeground(new Color(255, 30, 0));
		dialog.messageLabel.setIcon(new ImageIcon(JotifyApplication.class.getResource("images/error.png")));
		dialog.messageLabel.setText("<html>" + message + "</html>");
		dialog.messageLabel.setVisible(true);
	}
	
	public static void showInformationMessage(String message){
		dialog.messageLabel.setForeground(new Color(0, 50, 111));
		dialog.messageLabel.setIcon(new ImageIcon(JotifyApplication.class.getResource("images/information.png")));
		dialog.messageLabel.setText("<html>" + message + "</html>");
		dialog.messageLabel.setVisible(true);
	}
	
	public static void hideMessage(){
		dialog.messageLabel.setVisible(false);
	}
	
	public static void updateDialog(){
		if(dialog.messageLabel.isVisible() || dialog.loadLabel.isVisible()){
			dialog.setSize(270, 340);
		}
		else{
			dialog.setSize(270, 290);
		}
	}
	
	public static void setLoginCredentials(JotifyLoginCredentials credentials){
		dialog.usernameField.setText(credentials.getUsername());
		dialog.passwordField.setText(credentials.getPassword());
		dialog.rememberMeCheckBox.setSelected(credentials.getRemember());
	}
	
	/* Retrieve login credentials from dialog, blocks until available. */
	public static void getLoginCredentials(JotifyLoginCredentials credentials){
		/* Enable "Sign in" button.*/
		dialog.signInButton.setEnabled(true);
		
		/* Wait for user to click "Sign in". */
		dialog.semaphore.acquireUninterruptibly();
		
		/* Set credentials. */
		credentials.setUsername(dialog.usernameField.getText());
		credentials.setPassword(dialog.passwordField.getPassword());
		credentials.setRemember(dialog.rememberMeCheckBox.isSelected());
	}
	
	/* Private constructor. To show a dialog use: showLoginDialog. */
	private JotifyLoginDialog(){
		/* Load icons. */
		List<Image> icons = new ArrayList<Image>();
		
		icons.add(new ImageIcon(JotifyApplication.class.getResource("images/icon_16.png")).getImage());
		icons.add(new ImageIcon(JotifyApplication.class.getResource("images/icon_32.png")).getImage());
		icons.add(new ImageIcon(JotifyApplication.class.getResource("images/icon_64.png")).getImage());
		icons.add(new ImageIcon(JotifyApplication.class.getResource("images/icon_128.png")).getImage());
		
		/* Create image panel. */
		this.panel = new JotifyLoginPanel();
		
		/* Add panel to content pane. */
		this.getContentPane().add(this.panel);
		
		/* Set title, size, etc. */
		this.setTitle("Jotify");
		this.setIconImages(icons);
		this.setBounds(100, 100, 270, 290);
		this.setUndecorated(true);
		this.setResizable(false);
		this.setDefaultCloseOperation(JDialog.EXIT_ON_CLOSE);
		
		/* Add mouse listeners for dragging. */
		this.addMouseListener(this);
		this.addMouseMotionListener(this);
		
		/* Set null layout. */
		this.panel.setLayout(null);
		
		/* Create fonts. */
		Font dialogBold11 = new Font(Font.DIALOG, Font.BOLD, 11);
		Font dialogBold12 = new Font(Font.DIALOG, Font.BOLD, 12);
		
		/* Create and add components. */
		this.closeButton = new JotifyCloseButton();
		this.closeButton.setBounds(255, 5, 10, 10);
		this.closeButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				System.exit(0);
			}
		});
		this.panel.add(this.closeButton);
		
		this.usernameLabel = new JLabel("Username");
		this.usernameLabel.setBounds(20, 120, 120, 20);
		this.usernameLabel.setForeground(new Color(0, 50, 111));
		this.usernameLabel.setFont(dialogBold11);
		this.panel.add(this.usernameLabel);
		
		this.usernameField = new JTextField();
		this.usernameField.setBounds(20, 140, 220, 20);
		this.usernameField.setBorder(BorderFactory.createBevelBorder(
			BevelBorder.LOWERED, new Color(0, 50, 111, 192), new Color(0, 50, 111, 192)
		));
		this.usernameField.addKeyListener(new KeyAdapter(){
			public void keyPressed(KeyEvent e){
				if(e.getKeyCode() == KeyEvent.VK_ENTER){
					signInButton.doClick();
				}
			}
		});
		this.panel.add(this.usernameField);
		
		this.passwordLabel = new JLabel("Password");
		this.passwordLabel.setBounds(20, 170, 120, 20);
		this.passwordLabel.setForeground(new Color(0, 50, 111));
		this.passwordLabel.setFont(dialogBold11);
		this.panel.add(this.passwordLabel);
		
		this.passwordField = new JPasswordField();
		this.passwordField.setBounds(20, 190, 220, 20);
		this.passwordField.setBorder(BorderFactory.createBevelBorder(
			BevelBorder.LOWERED, new Color(0, 50, 111, 192), new Color(0, 50, 111, 192)
		));
		this.passwordField.addKeyListener(new KeyAdapter(){
			public void keyPressed(KeyEvent e){
				if(e.getKeyCode() == KeyEvent.VK_ENTER){
					signInButton.doClick();
				}
			}
		});
		this.panel.add(this.passwordField);
		
		this.rememberMeCheckBox = new JotifyCheckBox("Remember me");
		this.rememberMeCheckBox.setBounds(20, 240, 120, 20);
		this.rememberMeCheckBox.setForeground(new Color(0, 50, 111));
		this.rememberMeCheckBox.setFont(dialogBold11);
		this.panel.add(this.rememberMeCheckBox);
		
		this.signInButton = new JotifyButton("Sign in");
		this.signInButton.setBounds(160, 240, 80, 20);
		this.signInButton.setFont(dialogBold12);
		this.signInButton.setEnabled(false);
		this.signInButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				if(!usernameField.getText().isEmpty() && passwordField.getPassword().length != 0){
					semaphore.release();
					
					signInButton.setEnabled(false);
				}
			}
		});
		this.panel.add(this.signInButton);
		
		this.loadLabel = new JLabel();
		this.loadLabel.setBounds(120, 280, 32, 32);
		this.loadLabel.setVisible(false);
		this.loadLabel.setIcon(new ImageIcon(JotifyApplication.class.getResource("images/load.gif")));
		this.panel.add(this.loadLabel);
		
		this.messageLabel = new JLabel();
		this.messageLabel.setBounds(20, 280, 230, 40);
		this.messageLabel.setVisible(false);
		this.messageLabel.setFont(dialogBold11);
		this.messageLabel.setBorder(new EmptyBorder(5, 5, 5, 5));
		this.panel.add(this.messageLabel);
		
		/* Create semaphore and aquire a permit. */
		this.semaphore = new Semaphore(1);
		this.semaphore.acquireUninterruptibly();
	}
	
	public void mouseClicked(MouseEvent e){
		/* Nothing. */
	}
	
	public void mouseEntered(MouseEvent e){
		/* Nothing. */
	}
	
	public void mouseExited(MouseEvent e){
		/* Nothing. */
	}
	
	public void mousePressed(MouseEvent e){
		/* Save mouse position, so we can calculate a delta later. */
		this.mouse = e.getLocationOnScreen();
	}
	
	public void mouseReleased(MouseEvent e){
		/* Set mouse position to null, we don't need it now. */
		this.mouse = null;
	}
	
	public void mouseDragged(MouseEvent e){
		/* If we got a pressed mouse (should always be true). */
		if(this.mouse != null){
			/* Get current location. */
			Point location = this.getLocationOnScreen();
			
			/* Calculate delta and translate. */
			location.translate(e.getXOnScreen() - this.mouse.x, e.getYOnScreen() - this.mouse.y);
			
			/* Set new location. */
			this.setLocation(location);
			
			/* Save new mouse position. */
			this.mouse = e.getLocationOnScreen();
		}
	}
	
	public void mouseMoved(MouseEvent e){
		/* Nothing. */
	}
}
