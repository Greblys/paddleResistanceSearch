package resistance;

import gnu.trove.map.hash.THashMap;

import org.chocosolver.solver.Solver;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.explanations.RuleStore;
import org.chocosolver.solver.search.solution.Solution;
import org.chocosolver.solver.variables.IVariableMonitor;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.VariableFactory;
import org.chocosolver.solver.variables.events.IEventType;
import org.chocosolver.solver.variables.events.IntEventType;

@SuppressWarnings("serial")
public class Node implements IVariableMonitor<IntVar> {
	
	private int dayTimes = 0;
	private Node[] children = new Node[4];
	private IntVar v;
	private IntVar diffSum;
	private IntVar[] diffs;
	
	public Node(int dayTimes){
		this.dayTimes = dayTimes;
		v = VariableFactory.integer("voltage "+dayTimes, 0, 1024, Resistance.getSolver());
		diffSum = VariableFactory.integer("children diffs "+dayTimes, 0, 3100, Resistance.getSolver());
		diffs = VariableFactory.integerArray("children diffs "+dayTimes, 3, 0, 1024, Resistance.getSolver());
		//v.addMonitor(new Monitor());
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
	
	public IntVar getVoltage(){
		return v;
	}
	
	public IntVar[] getDiffs(){
		return diffs;
	}
	
	public IntVar getDiffSum(){
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
	
	public int getChildrenSize(){
		return (isMorningIntact() ? 1 : 0) 
				+ (isAfternoonIntact() ? 1 : 0) 
				+ (isNoonIntact() ? 1 : 0) 
				+ (isEveningIntact() ? 1 : 0);
	}
	
	public String toString(Solution sol){
		return String.format(
				"%b %b %b %b %d", 
				isMorningIntact(), 
				isNoonIntact(), 
				isAfternoonIntact(), 
				isEveningIntact(), 
				sol.getIntVal(v)
		);
	}
	@Override
	public void onUpdate(IntVar x, IEventType e) throws ContradictionException {
		if(e == IntEventType.INSTANTIATE)
		System.out.println(String.format("%s %b %b %b %b", 
				x, 
				isMorningIntact(), 
				isNoonIntact(), 
				isAfternoonIntact(), 
				isEveningIntact()
		));
	}

	@Override
	public boolean why(RuleStore arg0, IntVar arg1, IEventType arg2, int arg3) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void duplicate(Solver arg0, THashMap<Object, Object> arg1) {
		// TODO Auto-generated method stub
	}
}
