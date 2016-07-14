package resistance;

import java.util.Random;

import gnu.trove.map.hash.THashMap;

import org.chocosolver.solver.ResolutionPolicy;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.IntConstraintFactory;
import org.chocosolver.solver.constraints.real.IntEqRealConstraint;
import org.chocosolver.solver.constraints.real.RealConstraint;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.explanations.RuleStore;
import org.chocosolver.solver.search.solution.BestSolutionsRecorder;
import org.chocosolver.solver.search.solution.ISolutionRecorder;
import org.chocosolver.solver.search.solution.Solution;
import org.chocosolver.solver.search.strategy.IntStrategyFactory;
import org.chocosolver.solver.trace.Chatterbox;
import org.chocosolver.solver.trace.IMessage;
import org.chocosolver.solver.variables.IVariableMonitor;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.RealVar;
import org.chocosolver.solver.variables.VariableFactory;
import org.chocosolver.solver.variables.events.IEventType;
import org.chocosolver.solver.variables.events.IntEventType;

class Monitor implements IVariableMonitor<IntVar> {

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
	public void onUpdate(IntVar x, IEventType t)
			throws ContradictionException {
		// TODO Auto-generated method stub
		System.out.println(x);
	}
	
}


public class Resistance implements IVariableMonitor<IntVar>, IMessage{
	
	static Solver s = new Solver();
	IntVar[] R = VariableFactory.boundedArray("R", 4, 10, 1000000, s);
	IntVar r = VariableFactory.bounded("r", 10, 1000000, s);
	//IntVar intSum = VariableFactory.integer("intSum", 0, 1000000, s);
	//IntVar intSum = VariableFactory.castToIntVar(sum);
	IntVar maxr = VariableFactory.fixed(VariableFactory.MAX_INT_BOUND, s);
	IntVar maxV = VariableFactory.fixed(1024, s);
	IntVar sum = VariableFactory.integer("sum", 0, VariableFactory.MAX_INT_BOUND, s);
	Node[] states = new Node[16];	
	
	Node head = null;
	
	Resistance(){
		
		for(int i = 1; i < 4; i++)
			s.post(IntConstraintFactory.arithm(R[i-1], "<", R[i]));
		
		
		//calculate voltages
		for(int i = 0; i < 16; i++){
			Node n = new Node(i);
			states[i] = n;
			
			if(i == 15)
				head = n;
			if(i > 0){
				IntVar[] times = VariableFactory.integerArray("timesR", 4, 0, VariableFactory.MAX_INT_BOUND, s);
				boolean[] isTimes = { n.isMorningIntact(), n.isNoonIntact(), n.isAfternoonIntact(), n.isEveningIntact() };
				
				//Parallel resistors sum
				for(int j = 0; j < 4; j++)
					if(isTimes[j])
						s.post(IntConstraintFactory.eucl_div(maxr, R[j], times[j]));
					else
						s.post(IntConstraintFactory.arithm(times[j], "=", 0));
				
				IntVar inverseDayR = VariableFactory.integer("inverseDayR", 0, VariableFactory.MAX_INT_BOUND, s);
				IntVar dayR = VariableFactory.integer("dayR "+i, 0, VariableFactory.MAX_INT_BOUND, s); //state resistance
				s.post(IntConstraintFactory.sum(times, inverseDayR));
				s.post(IntConstraintFactory.eucl_div(maxr, inverseDayR, dayR));
				//dayR.addMonitor(this);
				IntVar a = VariableFactory.integer("a", 0, VariableFactory.MAX_INT_BOUND, s);
				IntVar[] b = VariableFactory.integerArray("r+R", 2, 0, VariableFactory.MAX_INT_BOUND, s);
				IntVar c = VariableFactory.integer("c", 0, VariableFactory.MAX_INT_BOUND, s);
				
				s.post(IntConstraintFactory.arithm(b[0], "=", r));
				s.post(IntConstraintFactory.arithm(b[1], "=", dayR));
				s.post(IntConstraintFactory.times(maxV, r, a));
				s.post(IntConstraintFactory.sum(b,c));
				s.post(IntConstraintFactory.eucl_div(a, c, n.getVoltage()));
			}
		}
		
		for(Node n1 : states)
			for(Node n2 : states){
				if(n1 != n2 && n1.isChild(n2))
					n1.addChild(n2);
			}
		
		//printTree(head, "");
		addTreeConstraints(head);
		IntVar[] diffSums = VariableFactory.integerArray("diffSums", 16, 0, 3100, s);
		for(int i = 0; i < 16; i++)
			s.post(IntConstraintFactory.arithm(diffSums[i], "=", states[i].getDiffSum()));
		s.post(IntConstraintFactory.sum(diffSums, sum));
	}
	
	void addTreeConstraints(Node n){
		if(n != null){
			Node[] children = n.getChildren();
			IntVar[] diffs = n.getDiffs();
			IntVar diffSum = n.getDiffSum();
			
			for(int i = 1; i < 4; i++)
				if(children[i] == null){
					s.post(IntConstraintFactory.arithm(diffs[i-1], "=", 0));
				} else {
					s.post(IntConstraintFactory.distance(children[i].getVoltage(), children[i-1].getVoltage(), "=", diffs[i-1]));
				}
				
			s.post(IntConstraintFactory.sum(diffs, diffSum));
			
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
		//intSum.addMonitor(this);
		//sum.addMonitor(m);
		//r.addMonitor(this);
		//for(IntVar a : R)
			//a.addMonitor(m);
		//R[0].addMonitor(new Monitor());
		//r.addMonitor(this);
		//s.post(new IntEqRealConstraint(intSum, sum, 0.5));
		//Chatterbox.showContradiction(s);
		
		//Chatterbox.showDecisions(s);
		Chatterbox.showSolutions(s, this);
		IntVar[] vars = {R[0], R[1], R[2], R[3], r};
		Random generator = new Random();
		s.set(IntStrategyFactory.domOverWDeg(vars, generator.nextLong()));
		//System.out.println(s.findSolution());
		s.findOptimalSolution(ResolutionPolicy.MAXIMIZE, sum);
		System.out.println(s.isFeasible());
		System.out.println(s.isSatisfied());
		
		for(Solution sol : s.getSolutionRecorder().getSolutions()){
			System.out.println(
					String.format("Holy grail: %d %d %d %d %d", 
					sol.getIntVal(R[0]),
					sol.getIntVal(R[1]),
					sol.getIntVal(R[2]),
					sol.getIntVal(R[3]),
					sol.getIntVal(r)
			));
			printTree(head, "", sol);
		}
	}
		
	@Override
	public void onUpdate(IntVar x, IEventType e) throws ContradictionException {
		if(e == IntEventType.INSTANTIATE)
		System.out.println(String.format("%s %s %s %s %s %s", 
				x, 
				R[0].getValue(), 
				R[1].getValue(), 
				R[2].getValue(), 
				R[3].getValue(),
				r.getValue()
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

	@Override
	public String print() {
		Solution sol = s.getSolutionRecorder().getLastSolution();
		
		System.out.println(
				String.format("Holy grail: %d %d %d %d %d %d", 
				sol.getIntVal(R[0]),
				sol.getIntVal(R[1]),
				sol.getIntVal(R[2]),
				sol.getIntVal(R[3]),
				sol.getIntVal(r),
				sol.getIntVal(sum)
		));
		//printTree(head, "", sol);
		return "";
	}
	
	public static void main(String[] args) {
		Resistance rst = new Resistance();
		rst.solve();
	}
}
