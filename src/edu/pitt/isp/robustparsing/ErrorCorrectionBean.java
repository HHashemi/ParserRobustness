package edu.pitt.isp.robustparsing;

import java.io.Serializable;

public class ErrorCorrectionBean implements Serializable{
	private String type ;
	private int start ;
	private int end;
	private String corr_to;
	
	public ErrorCorrectionBean(){	
	}
	
	public ErrorCorrectionBean(String type, int start, int end, String corr_to){
		this.type = type;
		this.start = start;
		this.end = end;
		this.corr_to = corr_to;
	}
	
	public String getType(){
		return type;
	}
	
	public void setType(String type){
		this.type = type;
	}
	
	public int getStart(){
		return start;
	}
	
	public void setStart(int start){
		this.start = start;
	}
	
	public int getEnd(){
		return end;
	}
	
	public void setEnd(int end){
		this.end = end;
	}
	
	public String getCorr(){
		return corr_to;
	}
	
	public void setCorr(String corr_to){
		this.corr_to = corr_to;
	}
	
	public String toString(){
		return type + ", " + start + ", " + end + ", " + corr_to ;
	}
}
