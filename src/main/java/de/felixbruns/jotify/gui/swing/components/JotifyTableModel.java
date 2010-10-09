package de.felixbruns.jotify.gui.swing.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import de.felixbruns.jotify.gui.util.TimeFormatter;
import de.felixbruns.jotify.media.Track;

@SuppressWarnings("serial")
public class JotifyTableModel<T extends Track> extends AbstractTableModel {
	protected List<Column> columns;
	protected List<T>      rows;
	
	public JotifyTableModel(){
		this.columns = new ArrayList<Column>();
		this.rows    = new ArrayList<T>();
		
		/* Add columns. */		
		this.columns.add(new Column("Track",      String.class));
		this.columns.add(new Column("Artist",     String.class));
		this.columns.add(new Column("Time",       String.class));
		this.columns.add(new Column("Popularity", JotifyPopularityIndicator.class));
		this.columns.add(new Column("Album",      String.class));
	}
	
	public int getColumnCount(){
		return this.columns.size();
	}
	
	public int getRowCount(){
		return this.rows.size();
	}
	
	public void add(T element){
		int row = this.rows.size();
		
		if(this.rows.add(element)){
			this.fireTableRowsInserted(row, row);
		}
	}
	
	public void addAll(Collection<T> elements){
		int row = this.rows.size();
		
		if(this.rows.addAll(elements)){
			this.fireTableRowsInserted(row, row + elements.size() - 1);
		}
	}
	
	public void remove(T element){
		int row = this.rows.indexOf(element);
		
		this.remove(row);
	}
	
	public void remove(int row){
		if(this.rows.remove(row) != null){
			this.fireTableRowsDeleted(row, row);
		}
	}
	
	public void removeAll(){
		this.rows.clear();
		
		this.fireTableDataChanged();
	}
	
	public T get(int row){
		return this.rows.get(row);
	}
	
	public List<T> getSubList(int from, int to){
		return this.rows.subList(from, to);
	}
	
	public List<T> getAll(){
		return this.rows;
	}
	
	public Object getValueAt(int row, int col){
	  if (this.rows.size() < row) {
	    return null;
	  }
	  
		T element = this.rows.get(row);
		
		switch(col){
			case 0:
				return element.getTitle();
			case 1:
				if(element.getArtist() != null){
					return element.getArtist().getName();
				}
				else{
					return null;
				}
			case 2:
				return TimeFormatter.formatSeconds(element.getLength() / 1000);
			case 3:
				return element.getPopularity();
			case 4:
				if(element.getAlbum() != null){
					return element.getAlbum().getName();
				}
				else{
					return null;
				}
			default:
				return null;
		}
	}
	
	public Class<?> getColumnClass(int col){
		return this.columns.get(col).getColumnClass();
	}
	
	public String getColumnName(int col){
		return this.columns.get(col).getName();
	}
	
	public int findColumn(String name){
		return this.columns.indexOf(name);
	}
	
	public boolean isCellEditable(int row, int col){
		return false;
	}
	
	private class Column {
		private String   name;
		private Class<?> columnClass;
		
		public Column(String name, Class<?> columnClass){
			this.name        = name;
			this.columnClass = columnClass;
		}
		
		public String getName(){
			return this.name;
		}
		
		public Class<?> getColumnClass(){
			return this.columnClass;
		}
	}
}
