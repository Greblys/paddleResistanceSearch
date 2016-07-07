package resistance;

import org.chocosolver.solver.variables.RealVar;
import org.chocosolver.solver.variables.VariableFactory;

public class Node {
	
	private int dayTimes = 0;
	private Node[] children = new Node[4];
	private RealVar v = VariableFactory.real("voltage", 0, 1024, 0.01, Resistance.getSolver());
	
	public Node(int dayTimes){
		this.dayTimes = dayTimes;
	}
	
	public void addChild(Node n){
		int i = 0;
		for(; i < 4 && children[i] != null; i++);
		children[i] = n;
	}
	
	public boolean isMorningIntact(){
		return (dayTimes & 8) == 8;
	}
	
	public boolean isNoonIntact(){
		return (dayTimes & 4) == 4;
	}
	
	public boolean isAfternoonIntact(){
		return (dayTimes & 2) == 2;
	}
	
	public boolean isEveningIntact(){
		return (dayTimes & 1) == 1;
	}
	
	public void setVoltage(float v){
		this.v = v;
	}
	
	public int getDayTimes(){
		return dayTimes;
	}
	
	public int getIntactCells(){
		int x = 0;
		for(int g = 1; g < 9; g *= 2)
			if((dayTimes & g) == g)
				x++;
		return x;
	}
	
	public boolean isChild(Node n){
		return (dayTimes & n.getDayTimes()) == n.getDayTimes() 
			&& this != n 
			&& getIntactCells() - n.getIntactCells() == 1;	
	}
	
	public Node[] getChildren(){
		return children;
	}
	
	public String toString(){
		return String.format("%b %b %b %b %.2f", isMorningIntact(), isNoonIntact(), isAfternoonIntact(), isEveningIntact(), v);
	}
}
