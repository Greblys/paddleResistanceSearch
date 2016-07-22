package resistance;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import gnu.trove.map.hash.THashMap;

import org.chocosolver.solver.ResolutionPolicy;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.IntConstraintFactory;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.explanations.RuleStore;
import org.chocosolver.solver.search.solution.BestSolutionsRecorder;
import org.chocosolver.solver.search.solution.Solution;
import org.chocosolver.solver.search.strategy.IntStrategyFactory;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin;
import org.chocosolver.solver.search.strategy.selectors.variables.InputOrder;
import org.chocosolver.solver.trace.Chatterbox;
import org.chocosolver.solver.trace.IMessage;
import org.chocosolver.solver.variables.IVariableMonitor;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.VariableFactory;
import org.chocosolver.solver.variables.events.IEventType;
import org.chocosolver.solver.variables.events.IntEventType;

@SuppressWarnings("serial")
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


@SuppressWarnings("serial")
public class Resistance implements IVariableMonitor<IntVar>, IMessage{
	
	static Solver s = new Solver();
	int[] vals = new int[1000];
	
	IntVar[] R;
	IntVar r;
	IntVar maxr = VariableFactory.fixed(VariableFactory.MAX_INT_BOUND, s);
	IntVar maxV = VariableFactory.fixed(1024, s);
	Node[] states = new Node[16];	
	IntVar minDiff = VariableFactory.integer("minDiff", 0, 1024, s);
	IntVar[] diffs = VariableFactory.integerArray("global diffs", 43, 0, 1024, s);
	
	List<Node> l = new LinkedList<Node>();
	int diffsi = 0;
	
	Node head = null;
	
	Resistance(){
		
		for(int i = 0; i < vals.length; i++){
			vals[i] = i * 100;
		}
		
		R = VariableFactory.enumeratedArray("R", 4, vals, s);
		r = VariableFactory.enumerated("r", vals, s);
		
		for(int i = 1; i < 4; i++)
			s.post(IntConstraintFactory.arithm(R[i-1], "<", R[i]));
		
		
		//calculate voltages
		for(int i = 0; i < 16; i++){
			Node n = new Node(i);
			states[i] = n;
			
			if(i == 15)
				head = n;
			if(i > 0){
				// constraints to calculate v
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
				IntVar a = VariableFactory.integer("a", 0, VariableFactory.MAX_INT_BOUND, s);
				IntVar[] b = VariableFactory.integerArray("r+R", 2, 0, VariableFactory.MAX_INT_BOUND, s);
				IntVar c = VariableFactory.integer("c", 0, VariableFactory.MAX_INT_BOUND, s);
				
				s.post(IntConstraintFactory.arithm(b[0], "=", r));
				s.post(IntConstraintFactory.arithm(b[1], "=", dayR));
				s.post(IntConstraintFactory.times(maxV, r, a));
				s.post(IntConstraintFactory.sum(b,c));
				s.post(IntConstraintFactory.eucl_div(a, c, n.getVoltage()));
			} else {
				s.post(IntConstraintFactory.arithm(n.getVoltage(), "=", 0));
			}
		}
		
		for (int size = 1; size < 4; size++){
			int i = 0;
			while(i < 16){
				for(int j = 0; j < 16; j++){
					if(states[i].getChildrenSize() == size && states[j].getChildrenSize() == size && i != j){
						s.post(IntConstraintFactory.distance(
								states[i].getVoltage(), states[j].getVoltage(), "=", diffs[diffsi++]));
						i = j;
					}
				}
				i++;
			}
		}
		
		for(Node n1 : states){
			for(Node n2 : states){
				if(n1 != n2 && n1.isChild(n2)){
					n1.addChild(n2);
				}
			}
		}
		addTreeConstraints(head);
		
		s.post(IntConstraintFactory.minimum(minDiff, diffs));
	}
	
	void addTreeConstraints(Node n){
		if(n != null && !l.contains(n)){
			
			for(Node c : n.getChildren()){
				if(c != null){
					s.post(IntConstraintFactory.distance(c.getVoltage(), n.getVoltage(), "=", diffs[diffsi++]));
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
		IntVar[] vars = {r, R[0], R[1], R[2], R[3]};
		Random generator = new Random();
		//r.addMonitor(this);
		//R[1].addMonitor(this);
		s.set(new BestSolutionsRecorder(minDiff));
		//s.set(IntStrategyFactory.domOverWDeg(vars, generator.nextLong()));
		s.set(IntStrategyFactory.custom(new InputOrder<IntVar>(), new IntDomainMin(), vars));
		//s.set(new BestSolutionsRecorder(minDiff));
		//System.out.println(s.findSolution());
		s.findOptimalSolution(ResolutionPolicy.MAXIMIZE, minDiff);
		System.out.println(s.isFeasible());
		System.out.println(s.isSatisfied());
		
		for(Solution sol : s.getSolutionRecorder().getSolutions()){
			System.out.println(
					String.format("Holy grail: %d %d %d %d %d %d", 
					sol.getIntVal(R[0]),
					sol.getIntVal(R[1]),
					sol.getIntVal(R[2]),
					sol.getIntVal(R[3]),
					sol.getIntVal(r),
					sol.getIntVal(minDiff)
			));
			
			printTree(head, "", sol);
				
		}
	}
		
	@Override
	public void onUpdate(IntVar x, IEventType e) throws ContradictionException {
		if(e == IntEventType.INSTANTIATE)
		System.out.println(String.format("%s %s %s %s %s %s %s", 
				x, 
				R[0].getValue(), 
				R[1].getValue(), 
				R[2].getValue(), 
				R[3].getValue(),
				r.getValue(),
				minDiff.getValue()
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
		if(sol != null){
			//printTree(head, "", sol);
			return String.format("%d %d %d %d %d %d", 
					sol.getIntVal(R[0]),
					sol.getIntVal(R[1]),
					sol.getIntVal(R[2]),
					sol.getIntVal(R[3]),
					sol.getIntVal(r),
					sol.getIntVal(minDiff)
			);
		} else {
			return "";
		}
	}
	
	public static void main(String[] args) {
		Resistance rst = new Resistance();
		rst.solve();
	}
}
