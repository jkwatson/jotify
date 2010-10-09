package nl.pascaldevink.jotify.gui.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import nl.pascaldevink.jotify.gui.JotifyApplication;
import nl.pascaldevink.jotify.gui.swing.components.JotifyButton;
import nl.pascaldevink.jotify.gui.swing.components.JotifyCheckBox;
import nl.pascaldevink.jotify.gui.util.JotifyPreferences;

@SuppressWarnings("serial")
public class JotifyPreferencesDialog extends JDialog {
	private JPanel         lastfmPanel;
	private JLabel         lastfmUsernameLabel;
	private JLabel         lastfmPasswordLabel;
	private JTextField     lastfmUsernameField;
	private JPasswordField lastfmPasswordField;
	private JCheckBox      lastfmCheckBox;
	private JLabel         lastfmLabel;
	
	private JPanel         loginPanel;
	private JLabel         loginUsernameLabel;
	private JLabel         loginPasswordLabel;
	private JTextField     loginUsernameField;
	private JPasswordField loginPasswordField;
	private JCheckBox      loginCheckBox;
	
	private JButton        saveButton;
	private Font           fontDialogBold11;
	private JPanel         panel;
	
	private static JotifyPreferencesDialog dialog;
	
	/* Statically create a preferences dialog. */
	static {
		dialog = new JotifyPreferencesDialog();
	}
	
	/* Show a login dialog. */
	public static void showDialog(){
		dialog.updatePreferences();
		
		/* Center dialog on screen. */
		dialog.setLocationRelativeTo(null);
		
		/* Show dialog. */
		dialog.setVisible(true);
	}
	
	public static void hideDialog(){
		/* Close dialog. */
		dialog.setVisible(false);
	}
	
	private JotifyPreferencesDialog(){
		/* Load icons. */
		List<Image> icons = new ArrayList<Image>();
		
		icons.add(new ImageIcon(JotifyApplication.class.getResource("images/icon_16.png")).getImage());
		icons.add(new ImageIcon(JotifyApplication.class.getResource("images/icon_32.png")).getImage());
		icons.add(new ImageIcon(JotifyApplication.class.getResource("images/icon_64.png")).getImage());
		icons.add(new ImageIcon(JotifyApplication.class.getResource("images/icon_128.png")).getImage());
		
		/* Set title, size, etc. */
		this.setTitle("Preferences");
		this.setModal(true);
		this.setIconImages(icons);
		this.setBounds(100, 100, 400, 310);
		this.setResizable(false);
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		/* Set panel and null layout. */
		this.panel = new JPanel();
		this.panel.setLayout(null);
		this.panel.setBackground(new Color(55, 55, 55));
		this.setContentPane(this.panel);
		
		this.fontDialogBold11 = new Font(Font.DIALOG, Font.BOLD, 11);
		
		/* Add login preferences panel. */
		this.loginPanel = new JPanel();
		this.loginPanel.setLayout(null);
		this.loginPanel.setBorder(new TitledBorder(
			new LineBorder(Color.WHITE), "Login",
			TitledBorder.DEFAULT_JUSTIFICATION,
			TitledBorder.DEFAULT_POSITION,
			null, Color.WHITE)
		);
		this.loginPanel.setOpaque(false);
		this.loginPanel.setBounds(10, 10, 370, 110);
		this.panel.add(this.loginPanel);
		
		this.loginCheckBox = new JotifyCheckBox("Remember me");
		this.loginCheckBox.setForeground(Color.WHITE);
		this.loginCheckBox.setFont(this.fontDialogBold11);
		this.loginCheckBox.setBounds(15, 20, 200, 20);
		this.loginPanel.add(this.loginCheckBox);
		
		this.loginUsernameLabel = new JLabel("Username:");
		this.loginUsernameLabel.setForeground(Color.WHITE);
		this.loginUsernameLabel.setFont(this.fontDialogBold11);
		this.loginUsernameLabel.setBounds(15, 45, 80, 20);
		this.loginPanel.add(this.loginUsernameLabel);
		
		this.loginUsernameField = new JTextField();
		this.loginUsernameField.setBounds(90, 45, 120, 20);
		this.loginUsernameField.setFont(this.fontDialogBold11);
		this.loginPanel.add(this.loginUsernameField);
		
		this.loginPasswordLabel = new JLabel("Password:");
		this.loginPasswordLabel.setForeground(Color.WHITE);
		this.loginPasswordLabel.setFont(this.fontDialogBold11);
		this.loginPasswordLabel.setBounds(15, 70, 80, 20);
		this.loginPanel.add(this.loginPasswordLabel);
		
		this.loginPasswordField = new JPasswordField();
		this.loginPasswordField.setBounds(90, 70, 120, 20);
		this.loginPasswordField.setFont(this.fontDialogBold11);
		this.loginPanel.add(this.loginPasswordField);
		
		/* Add last.fm preferences panel. */
		this.lastfmPanel = new JPanel();
		this.lastfmPanel.setLayout(null);
		this.lastfmPanel.setBorder(new TitledBorder(
			new LineBorder(Color.WHITE), "last.fm",
			TitledBorder.DEFAULT_JUSTIFICATION,
			TitledBorder.DEFAULT_POSITION,
			null, Color.WHITE)
		);
		this.lastfmPanel.setOpaque(false);
		this.lastfmPanel.setBounds(10, 125, 370, 110);
		this.panel.add(this.lastfmPanel);
		
		this.lastfmCheckBox = new JotifyCheckBox("Enable last.fm scrobbling");
		this.lastfmCheckBox.setForeground(Color.WHITE);
		this.lastfmCheckBox.setFont(this.fontDialogBold11);
		this.lastfmCheckBox.setBounds(15, 20, 200, 20);
		this.lastfmPanel.add(this.lastfmCheckBox);
		
		this.lastfmUsernameLabel = new JLabel("Username:");
		this.lastfmUsernameLabel.setForeground(Color.WHITE);
		this.lastfmUsernameLabel.setFont(this.fontDialogBold11);
		this.lastfmUsernameLabel.setBounds(15, 45, 80, 20);
		this.lastfmPanel.add(this.lastfmUsernameLabel);
		
		this.lastfmUsernameField = new JTextField();
		this.lastfmUsernameField.setBounds(90, 45, 120, 20);
		this.lastfmUsernameField.setFont(this.fontDialogBold11);
		this.lastfmPanel.add(this.lastfmUsernameField);
		
		this.lastfmPasswordLabel = new JLabel("Password:");
		this.lastfmPasswordLabel.setForeground(Color.WHITE);
		this.lastfmPasswordLabel.setFont(this.fontDialogBold11);
		this.lastfmPasswordLabel.setBounds(15, 70, 80, 20);
		this.lastfmPanel.add(this.lastfmPasswordLabel);
		
		this.lastfmPasswordField = new JPasswordField();
		this.lastfmPasswordField.setBounds(90, 70, 120, 20);
		this.lastfmPasswordField.setFont(this.fontDialogBold11);
		this.lastfmPanel.add(this.lastfmPasswordField);
		
		this.lastfmLabel = new JLabel(
			new ImageIcon(JotifyApplication.class.getResource("images/lastfm_red_small.gif"))
		);
		this.lastfmLabel.setBounds(275, 20, 80, 28);
		this.lastfmPanel.add(this.lastfmLabel);
		
		/* Add save button. */
		this.saveButton = new JotifyButton("Save");
		this.saveButton.setPreferredSize(new Dimension(80, 20));
		this.saveButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				savePreferences();
				
				JotifyPreferencesDialog.hideDialog();
			}
		});
		this.saveButton.setBounds(295, 250, 80, 20);
		this.panel.add(this.saveButton);
	}
	
	private void updatePreferences(){
		JotifyPreferences preferences = JotifyPreferences.getInstance();
		
		this.loginCheckBox.setSelected(preferences.getBoolean("login.remember", false));
		this.loginUsernameField.setText(preferences.getString("login.username", ""));
		this.loginPasswordField.setText(preferences.getString("login.password", ""));
		
		this.lastfmCheckBox.setSelected(preferences.getBoolean("lastfm.enabled", false));
		this.lastfmUsernameField.setText(preferences.getString("lastfm.username", ""));
		this.lastfmPasswordField.setText(preferences.getString("lastfm.password", ""));
	}
	
	private void savePreferences(){
		JotifyPreferences preferences = JotifyPreferences.getInstance();
		
		preferences.setBoolean("login.remember", this.loginCheckBox.isSelected());
		preferences.setString("login.username", this.loginUsernameField.getText());
		preferences.setString("login.password", new String(this.loginPasswordField.getPassword()));
		
		preferences.setBoolean("lastfm.enabled", this.lastfmCheckBox.isSelected());
		preferences.setString("lastfm.username", this.lastfmUsernameField.getText());
		preferences.setString("lastfm.password", new String(this.lastfmPasswordField.getPassword()));
		
		preferences.save();
	}
}
