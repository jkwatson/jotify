package nl.pascaldevink.jotify.gui.swing.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import nl.pascaldevink.jotify.gui.swing.plaf.JotifyPopularityIndicatorUI;

@SuppressWarnings("serial")
public class JotifyPopularityIndicator extends JPanel implements TableCellRenderer {
	private float popularity;
	
	public JotifyPopularityIndicator(){
		this.setOpaque(false);
		this.setForeground(Color.WHITE);
		this.setMaximumSize(new Dimension(100, 10));
		this.setUI(new JotifyPopularityIndicatorUI());
		
		this.popularity = 0.0f;
	}
	
	public float getPopularity(){
		return this.popularity;
	}
	
	public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c){
		if(v instanceof Float){
			this.popularity = (Float)v;
		}
		
		/* Set colors depending on selection. */
		if(s){
			this.setForeground(t.getSelectionForeground());
			this.setBackground(t.getSelectionBackground());
		}
		else{
			this.setForeground(t.getForeground());
			this.setBackground(t.getBackground());
		}
		
		return this;
	}
}
