package resistance;

import java.util.Random;

import javax.swing.plaf.basic.BasicProgressBarUI;

import gnu.trove.map.hash.THashMap;

import org.chocosolver.solver.ICause;
import org.chocosolver.solver.ResolutionPolicy;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.IntConstraintFactory;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.explanations.RuleStore;
import org.chocosolver.solver.search.loop.Learn;
import org.chocosolver.solver.search.loop.MoveBinaryDFS;
import org.chocosolver.solver.search.loop.PropagateBasic;
import org.chocosolver.solver.search.solution.BestSolutionsRecorder;
import org.chocosolver.solver.search.solution.Solution;
import org.chocosolver.solver.search.strategy.IntStrategyFactory;
import org.chocosolver.solver.search.strategy.decision.IntDecision;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainRandomBound;
import org.chocosolver.solver.search.strategy.selectors.variables.DomOverWDeg;
import org.chocosolver.solver.trace.Chatterbox;
import org.chocosolver.solver.trace.IMessage;
import org.chocosolver.solver.variables.IVariableMonitor;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.VariableFactory;
import org.chocosolver.solver.variables.events.IEventType;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.solver.search.loop.SearchLoop;

@SuppressWarnings("serial")
public class Resistance implements IVariableMonitor<IntVar>, IMessage, Learn, ICause{
	
	static Solver s = new Solver();
	IntVar[] R = VariableFactory.boundedArray("R", 4, 1, 100, s);
	IntVar r = VariableFactory.bounded("r", 1, 100, s);
	IntVar maxr = VariableFactory.fixed(VariableFactory.MAX_INT_BOUND, s);
	IntVar maxV = VariableFactory.fixed(1024, s);
	Node[] states = new Node[16];	
	IntVar minDiff = VariableFactory.integer("minDiff", 0, 1024, s);
	IntVar[] diffs = VariableFactory.integerArray("global diffs", 11, 0, 1024, s);
	int[][] map = new int[11][2];
	IntVar[] searchVars = {R[0], R[1], R[2], R[3], r};
	int mini = 0; //store minimum difference index
	
	
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
				s.post(IntConstraintFactory.arithm(states[i].getVoltage(), "=", 0));
			}
		}
		
		int diffsi = 0;
		
		for (int size = 1; size < 4; size++){
			int i = 0;
			while(i < 16){
				for(int j = 0; j < 16; j++){
					if(states[i].getChildrenSize() == size && states[j].getChildrenSize() == size && i != j){
						map[diffsi][0] = i;
						map[diffsi][1] = j;
						s.post(IntConstraintFactory.distance(
								states[i].getVoltage(), states[j].getVoltage(), "=", diffs[diffsi++]));
						i = j;
					}
				}
				i++;
			}
		}
		
		s.post(IntConstraintFactory.minimum(minDiff, diffs));
		
		for(Node n1 : states)
			for(Node n2 : states){
				if(n1 != n2 && n1.isChild(n2))
					n1.addChild(n2);
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
		
		Chatterbox.showSolutions(s, this);
		Random generator = new Random();
		
		//minDiff.addMonitor(this);
		//R[1].addMonitor(this);
		
		//r.addMonitor(this);
		s.set(IntStrategyFactory.domOverWDeg(searchVars, generator.nextLong(), new IntDomainRandomBound(generator.nextLong())));
		s.set(new BestSolutionsRecorder(minDiff));
		//DomOverWDeg dow = new DomOverWDeg(searchVars, generator.nextLong(), new IntDomainMin());
		//SearchLoop sl = new SearchLoop(s, new PropagateBasic(), this, new MoveBinaryDFS(dow));
		//s.set(sl);
		//s.set(IntStrategyFactory.custom(this, IntStrategyFactory.min_value_selector(), vars));
		//LNSFactory.rlns(s, vars, 1000, generator.nextLong(), new BacktrackCounter(s, 1000000));
		s.findOptimalSolution(ResolutionPolicy.MAXIMIZE, minDiff);
		System.out.println(s.isFeasible());
		System.out.println(s.isSatisfied());
		
		for(Solution sol : s.getSolutionRecorder().getSolutions()){
			System.out.println(
					String.format("%d %d %d %d %d", 
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
	public boolean why(RuleStore rs, IntVar v, IEventType et, int val) {
		
		return false;
	}

	@Override
	public void duplicate(Solver arg0, THashMap<Object, Object> arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String print() {
		Solution sol = s.getSolutionRecorder().getLastSolution();
		if(sol != null)
			//printTree(head, "", sol);
			return String.format("%d %d %d %d %d %d", 
					sol.getIntVal(R[0]),
					sol.getIntVal(R[1]),
					sol.getIntVal(R[2]),
					sol.getIntVal(R[3]),
					sol.getIntVal(r),
					sol.getIntVal(minDiff)
			);
		else
			return "";
	}
	
	public static void main(String[] args) {
		Resistance rst = new Resistance();
		rst.solve();
	}

	@Override
	public void forget(SearchLoop sl) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void record(SearchLoop sl) {
		IntDecision d = (IntDecision) sl.getLastDecision();
		IntVar v = d.getDecisionVariables();
		System.out.println(mini);
		System.out.println(v.getUB());
		if(diffs[mini].getValue() != minDiff.getValue()){
			int min = minDiff.getValue();
			int i = 0;
			for(;i < diffs.length; i++){
				if(min == diffs[i].getValue())
					break;
			}
			mini = i;
		}
		
		if (mini < 11){
			int s1 = map[mini][0];
			int s2 = map[mini][1];
			if(v == r
					|| v == R[0] && (states[s1].isMorningIntact() || states[s2].isMorningIntact())
					|| v == R[1] && (states[s1].isNoonIntact() || states[s2].isNoonIntact())
					|| v == R[2] && (states[s1].isAfternoonIntact() || states[s2].isAfternoonIntact())
					|| v == R[3] && (states[s1].isEveningIntact() || states[s2].isEveningIntact())){
				
				//good
			} else {
				System.out.println("labas");
				
			}
		} else {
			mini = 0;
		}
	}
}
