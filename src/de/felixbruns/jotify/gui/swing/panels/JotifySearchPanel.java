package de.felixbruns.jotify.gui.swing.panels;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.concurrent.TimeoutException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import de.felixbruns.jotify.Jotify;
import de.felixbruns.jotify.gui.listeners.JotifyBroadcast;
import de.felixbruns.jotify.gui.listeners.SearchListener;
import de.felixbruns.jotify.gui.swing.components.JotifyButton;
import de.felixbruns.jotify.media.Result;

@SuppressWarnings("serial")
public class JotifySearchPanel extends JPanel implements SearchListener {
	private JotifyBroadcast broadcast;
	private JTextField      searchField;
	private JButton         searchButton;
	
	public JotifySearchPanel(final Jotify jotify){
		this.broadcast = JotifyBroadcast.getInstance();
		
		/* Flow content to the left. */
		this.setLayout(new FlowLayout(FlowLayout.LEFT));
		
		/* Create and add search field. */
		this.searchField = new JTextField();
		this.searchField.setPreferredSize(new Dimension(170, 20));
		this.searchField.setBorder(BorderFactory.createLineBorder(new Color(0, 0, 0, 127)));
		this.searchField.addKeyListener(new KeyAdapter(){
			public void keyPressed(KeyEvent e){
				if(e.getKeyCode() == KeyEvent.VK_ENTER){
					searchButton.doClick();
				}
			}
		});
		this.add(this.searchField);
		
		/* Create and add search button. */
		this.searchButton = new JotifyButton("Search");
		this.searchButton.setPreferredSize(new Dimension(80, 20));
		this.searchButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				/* Get query. */
				final String query = searchField.getText();
				
				/* Do a search if query is not empty. */
				if(!query.trim().isEmpty()){
					/* Disable search button, will be enable again in searchResultReceived method. */
					searchButton.setEnabled(false);
					
					/* Searching will be done in a thread, so the UI doesn't block. */
					new Thread("Searching-Thread"){
						public void run(){
							try{
								/* Do search via jotify API. */
								Result result = jotify.search(query);
								
								broadcast.fireSearchResultReceived(result);
							}
							catch (TimeoutException e){
								e.printStackTrace();
							}
						}
					}.start();
				}
			}
		});
		this.add(this.searchButton);
	}
	
	/* Set search query to provided value. */
	public void setSearchQuery(String query){
		this.searchField.setText(query);
	}
	
	public JButton getSearchButton(){
		return this.searchButton;
	}
	
	protected void paintComponent(Graphics graphics){
		/* Get bounds and 2D graphics. */
		Rectangle  bounds     = this.getBounds();
		Graphics2D graphics2D = (Graphics2D)graphics;
		
		/* Draw vertical gradient. */
		graphics2D.setPaint(new GradientPaint(
			0,             0, new Color(174, 174, 174),
			0, bounds.height, new Color(143, 143, 143)
		));
		graphics2D.fillRect(0, 0, bounds.width, bounds.height);
	}
	
	public void searchResultReceived(Result result){
		/* Enable search button again. */
		this.searchButton.setEnabled(true);
	}
	
	public void searchResultSelected(Result result){
		/* Update search field to show selected search results query. */
		this.searchField.setText(result.getQuery());
	}
}
