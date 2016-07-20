package resistance;

import java.util.LinkedList;
import java.util.List;

import gnu.trove.map.hash.THashMap;

import org.chocosolver.solver.ResolutionPolicy;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.IntConstraintFactory;
import org.chocosolver.solver.constraints.real.IntEqRealConstraint;
import org.chocosolver.solver.constraints.real.RealConstraint;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.explanations.RuleStore;
import org.chocosolver.solver.search.solution.Solution;
import org.chocosolver.solver.search.strategy.IntStrategyFactory;
import org.chocosolver.solver.search.strategy.RealStrategyFactory;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin;
import org.chocosolver.solver.search.strategy.selectors.values.RealDomainMin;
import org.chocosolver.solver.search.strategy.selectors.variables.InputOrder;
import org.chocosolver.solver.trace.Chatterbox;
import org.chocosolver.solver.trace.IMessage;
import org.chocosolver.solver.variables.IVariableMonitor;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.RealVar;
import org.chocosolver.solver.variables.VariableFactory;
import org.chocosolver.solver.variables.events.IEventType;

class Monitor implements IVariableMonitor<RealVar> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void onUpdate(RealVar x, IEventType arg1) throws ContradictionException {
		System.out.println(x);
		
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

public class Resistance implements IVariableMonitor<IntVar>, IMessage {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	static Solver s = new Solver();
	RealVar[] realR = VariableFactory.realArray("R", 4, 0.1, 100, 0.1, s); //same array in float
	RealVar realr = VariableFactory.real("r", 0.1, 100, 0.1, s);
	RealVar[] diffs = VariableFactory.realArray("diffs", 32, 0, 1024, 0.1, s);
	RealVar minDiff = VariableFactory.real("minDiff", 0, 1024, 0.1, s);
	int diffsi = 0;
	List<Node> l = new LinkedList<Node>();
	
	//BestSolutionsRecorder bsr = new BestSolutionsRecorder(intSum);
	Node head = null;
	
	Resistance(){
		Node[] states = new Node[16];
		
		for(int i = 1; i < 4; i++)
			s.post(new RealConstraint("R1 < R2...", "{0}<{1}", realR[i-1], realR[i]));
		
		//calculate voltages
		for(int i = 0; i < 16; i++){
			Node n = new Node(i);
			states[i] = n;
			
			if(i == 15)
				head = n;
			if(i > 0){
				RealVar[] times = VariableFactory.realArray("timesR " + i, 4, 0, 100000, 0.1, s);
				boolean[] isTimes = { n.isMorningIntact(), n.isNoonIntact(), n.isAfternoonIntact(), n.isEveningIntact() };
				
				for(int j = 0; j < 4; j++)
					if(isTimes[j])
						s.post(new RealConstraint("time " + i + " " + j, "{0}=1/{1}", times[j], realR[j]));
					else
						s.post(new RealConstraint("time " + i + " " + j, "{0}=0", times[j]));

				RealVar dayR = VariableFactory.real("dayR " + i, 0, 100000, 0.1, s); //state resistance
				RealConstraint rc = new RealConstraint(
						String.format("%d totalDayR", i),
						"{0}=1/({1} + {2} + {3} + {4})",
						dayR, times[0], times[1], times[2], times[3]
				);
				s.post(rc);
				RealConstraint rc2 = new RealConstraint(
						String.format("%d totalVoltage", i),
						"{0}=1024*{1}/({1}+{2})",
						n.getVoltage(), realr, dayR
				);
				s.post(rc2);
			} else {
				s.post(new RealConstraint("0 voltage", "{0}=0", n.getVoltage()));
			}
		}
		
		int diffsi = 0;
		
		for (int size = 1; size < 4; size++){
			int i = 0;
			while(i < 16){
				for(int j = 0; j < 16; j++){
					if(states[i].getChildrenSize() == size && states[j].getChildrenSize() == size && i != j){
						s.post(new RealConstraint("sibling voltge diff", 
							"{0} = abs({1} - {2})", diffs[diffsi++], states[i].getVoltage(), states[j].getVoltage()));
						i = j;
					}
				}
				i++;
			}
		}
		
		for(Node n1 : states)
			for(Node n2 : states){
				if(n1 != n2 && n1.isChild(n2))
					n1.addChild(n2);
					
			}
		
		//printTree(head, "");
		addTreeConstraints(head);
		
		s.post(new RealConstraint("minDiff", 
				"{0} = min({1}, {2}, {3}, {4}, {5}, {6}, {7}, {8}, {9}, {10}, {11}, {12}, {13}, {14}, {15}, {16}, {17}, {18}, {19},"
				+ "{20}, {21}, {22}, {23}, {24}, {25}, {26}, {27}, {28}, {29}, {30}, {31}, {32})", 
				minDiff, diffs[0], diffs[1], diffs[2], diffs[3], diffs[4], diffs[5], diffs[6], diffs[7], diffs[8], diffs[9], diffs[10],
				diffs[11], diffs[12], diffs[13], diffs[14], diffs[15], diffs[16], diffs[17], diffs[18], diffs[19], diffs[20], diffs[21],
				diffs[22], diffs[23], diffs[24], diffs[25], diffs[26], diffs[27], diffs[28], diffs[29], diffs[30], diffs[31]
		));
	}
	
	void addTreeConstraints(Node n){
		if(n != null && !l.contains(n)){
			
			for(Node c : n.getChildren()){
				if(c != null){
					s.post(new RealConstraint(
							"child parent diff", "{0} = abs({1} - {2})", diffs[diffsi++], c.getVoltage(), n.getVoltage()));
				}
			}
			
			l.add(n);
			for(Node child : n.getChildren())
				addTreeConstraints(child);
		}
	}
	
	void printTree(Node node, String padding, Solution sol){
		if(node != null){
			System.out.print(padding);
			System.out.println(node.toString(sol));
			for(Node child : node.getChildren())
				printTree(child, padding + "    ", sol);
		}
	}
	
	public static Solver getSolver(){
		return s;
	}
	
	public void solve(){
		//Chatterbox.showContradiction(s);
		//Chatterbox.showDecisions(s);
		Chatterbox.showSolutions(s, this);
		//System.out.println(s.findSolution());
		RealVar[] vars = {realr, realR[0], realR[1], realR[2], realR[3]};
		//s.set(RealStrategyFactory.custom(new InputOrder<RealVar>(), new RealDomainMin(), vars));
		s.set(RealStrategyFactory.cyclic_middle(vars));
		s.findOptimalSolution(ResolutionPolicy.MAXIMIZE, minDiff, 0.1);
		System.out.println(s.isFeasible());
		System.out.println(s.isSatisfied());
		
		/*
		for(Solution sol : s.getSolutionRecorder().getSolutions()){
			System.out.println(
					String.format("Holy grail: %s %d %d %d %d %d", 
					sol.getRealBounds(minDiff),
					sol.getIntVal(R[0]),
					sol.getIntVal(R[1]),
					sol.getIntVal(R[2]),
					sol.getIntVal(R[3]),
					sol.getIntVal(r)
			));
			printTree(head, "", sol);
		}
		*/
		
		
	}
	
	public static void main(String[] args) {
		Resistance rst = new Resistance();
		rst.solve();
	}

	@Override
	public void onUpdate(IntVar x, IEventType arg1) throws ContradictionException {
		System.out.println(x);
		
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

	@Override
	public String print() {
		System.out.println(String.format("Solution: %s %s %s %s %s",
				minDiff,
				realR[0], 
				realR[1], 
				realR[2], 
				realR[3]
		));
		
		//printTree(head, "", s.getSolutionRecorder().getLastSolution());
		return "";
	}
}
