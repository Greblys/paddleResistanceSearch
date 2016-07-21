package resistance;

import org.chocosolver.solver.search.solution.ISolutionRecorder;
import org.chocosolver.solver.search.solution.Solution;
import org.chocosolver.solver.trace.IMessage;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.RealVar;
import org.chocosolver.solver.variables.VariableFactory;

public class Node{
	
	private int dayTimes = 0;
	private Node[] children = new Node[4];
	private IntVar iv;
	private RealVar v;
	
	public Node(int dayTimes){
		this.dayTimes = dayTimes;
		v = VariableFactory.real("voltage", 0, 1024, 1, Resistance.getSolver());
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
	
	public RealVar getVoltage(){
		return v;
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
	
	public int getChildrenSize(){
		return (isMorningIntact() ? 1 : 0) 
				+ (isAfternoonIntact() ? 1 : 0) 
				+ (isNoonIntact() ? 1 : 0) 
				+ (isEveningIntact() ? 1 : 0);
	}
	
	public String toString(Solution sol){
		return String.format(
				"%b %b %b %b %s", 
				isMorningIntact(), 
				isNoonIntact(), 
				isAfternoonIntact(), 
				isEveningIntact(), 
				v
		);
	}
}
