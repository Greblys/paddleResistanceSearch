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
	private IntVar iv = VariableFactory.integer("intVoltage", 0, 1024, Resistance.getSolver());
	private RealVar v = VariableFactory.real(iv, 0.01);
	private IntVar idiffSum = VariableFactory.integer("children diffs", 0, 3100, Resistance.getSolver());
	private RealVar diffSum = VariableFactory.real(idiffSum, 0.01);
	private IntVar[] idiffs = VariableFactory.integerArray("children diffs", 3, 0, 1024, Resistance.getSolver());
	private RealVar[] diffs = VariableFactory.real(idiffs, 0.01);
	
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
	
	public RealVar getVoltage(){
		return v;
	}
	
	public RealVar[] getDiffs(){
		return diffs;
	}
	
	public RealVar getDiffSum(){
		return diffSum;
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
	
	public String toString(Solution sol){
		return String.format(
				"%b %b %b %b %.2f %.2f %.2f %.2f %.2f", 
				isMorningIntact(), 
				isNoonIntact(), 
				isAfternoonIntact(), 
				isEveningIntact(), 
				sol.getRealBounds(v)[0],
				sol.getRealBounds(diffs[0])[0],
				sol.getRealBounds(diffs[1])[0],
				sol.getRealBounds(diffs[2])[0],
				sol.getRealBounds(diffSum)[0]
		);
	}
}
