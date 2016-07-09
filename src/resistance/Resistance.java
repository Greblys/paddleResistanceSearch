package resistance;

import org.chocosolver.solver.ResolutionPolicy;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.IntConstraintFactory;
import org.chocosolver.solver.constraints.real.IntEqRealConstraint;
import org.chocosolver.solver.constraints.real.RealConstraint;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.solution.BestSolutionsRecorder;
import org.chocosolver.solver.search.solution.ISolutionRecorder;
import org.chocosolver.solver.search.solution.Solution;
import org.chocosolver.solver.trace.Chatterbox;
import org.chocosolver.solver.trace.IMessage;
import org.chocosolver.solver.variables.IVariableMonitor;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.RealVar;
import org.chocosolver.solver.variables.VariableFactory;
import org.chocosolver.solver.variables.events.IEventType;

class Monitor implements IVariableMonitor<RealVar> {

	@Override
	public void onUpdate(RealVar x, IEventType arg1) throws ContradictionException {
		System.out.println(x);
		
	}
	
}

public class Resistance implements IVariableMonitor<IntVar> {
	
	static Solver s = new Solver();
	IntVar[] R = VariableFactory.boundedArray("R", 4, 1, 100000, s);
	RealVar[] realR = VariableFactory.real(R, 0.01); //same array in float
	IntVar r = VariableFactory.bounded("r", 1, 100000, s);
	RealVar realr = VariableFactory.real(r, 0.01);
	RealVar sum = VariableFactory.real("sum", 0, 50000, 0.01, s);
	//IntVar intSum = VariableFactory.integer("intSum", 0, 1000000, s);
	//IntVar intSum = VariableFactory.castToIntVar(sum);
	
	//BestSolutionsRecorder bsr = new BestSolutionsRecorder(intSum);
	Node head = null;
	
	Resistance(){
		Node[] states = new Node[16];
		//link two different representations of same array
		s.post(new IntEqRealConstraint(R, realR, 0));
		//link two different representations of same variable
		s.post(new IntEqRealConstraint(r, realr, 0));
		
		
		for(int i = 1; i < 4; i++)
			s.post(IntConstraintFactory.arithm(R[i-1], "<", R[i]));
		
		
		//calculate voltages
		for(int i = 0; i < 16; i++){
			Node n = new Node(i);
			states[i] = n;
			
			if(i == 15)
				head = n;
			if(i > 0){
				RealVar[] times = VariableFactory.realArray("timesR", 4, 0, 1, 0.1, s);
				boolean[] isTimes = { n.isMorningIntact(), n.isNoonIntact(), n.isAfternoonIntact(), n.isEveningIntact() };
				
				for(int j = 0; j < 4; j++)
					if(isTimes[j])
						s.post(new RealConstraint("time", "{0}=1/{1}", times[j], realR[j]));
					else
						s.post(new RealConstraint("time", "{0}=0", times[j]));

				RealVar dayR = VariableFactory.real("dayR", 0, 1000000, 0.01, s); //state resistance
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
			}
		}
		
		for(Node n1 : states)
			for(Node n2 : states){
				if(n1 != n2 && n1.isChild(n2))
					n1.addChild(n2);
			}
		
		//printTree(head, "");
		addTreeConstraints(head);
		
		s.post(new RealConstraint("total sum",
				"{0} = {1} + {2} + {3} + {4} + {5} + {6} + {7} + {8} + {9} + {10} + {11} + {12} + {13} + {14} + {15} + {16}",
				sum, 
				states[0].getDiffSum(), 
				states[1].getDiffSum(), 
				states[2].getDiffSum(),
				states[3].getDiffSum(),
				states[4].getDiffSum(),
				states[5].getDiffSum(),
				states[6].getDiffSum(),
				states[7].getDiffSum(),
				states[8].getDiffSum(),
				states[9].getDiffSum(),
				states[10].getDiffSum(),
				states[11].getDiffSum(),
				states[12].getDiffSum(),
				states[13].getDiffSum(),
				states[14].getDiffSum(),
				states[15].getDiffSum()
		));
	}
	
	void addTreeConstraints(Node n){
		if(n != null){
			Node[] children = n.getChildren();
			RealVar[] diffs = n.getDiffs();
			RealVar diffSum = n.getDiffSum();
			
			for(int i = 1; i < 4; i++){
				if(children[i] != null && children[i-1] != null){
					/*
					//to sort them
					s.post(new RealConstraint(
							"greater than", "{0} > {1}", children[i].getVoltage(), children[i-1].getVoltage()
					));
					*/
					
					s.post(new RealConstraint(
						String.format("%d %d-%d totalVoltage", n.getDayTimes(), i, i-1),
						"{0}=abs({1} - {2})",
						diffs[i-1], children[i].getVoltage(), children[i-1].getVoltage()
					));
					s.post(new RealConstraint(
						String.format("%d %d-%d minDifference", n.getDayTimes(), i, i-1),
						"abs({0} - {1}) >= 30",
						children[i].getVoltage(), children[i-1].getVoltage()
					));
				} else {
					s.post(new RealConstraint("diff", "{0}=0", diffs[i-1]));
				}
			}
			
			s.post(new RealConstraint("diffSum", "{0} = {1} + {2} + {3}", diffSum, diffs[0], diffs[1], diffs[2]));
			
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
		Monitor m = new Monitor();
		//intSum.addMonitor(this);
		//sum.addMonitor(m);
		//r.addMonitor(this);
		for(IntVar rs : R)
			rs.addMonitor(this);
		//r.addMonitor(this);
		//s.post(new IntEqRealConstraint(intSum, sum, 0.5));
		//Chatterbox.showContradiction(s);
		
		//Chatterbox.showDecisions(s);
		Chatterbox.showSolutions(s);
		//System.out.println(s.findSolution());
		s.findOptimalSolution(ResolutionPolicy.MAXIMIZE, sum, 0.01);
		System.out.println(s.isFeasible());
		System.out.println(s.isSatisfied());
		
		for(Solution sol : s.getSolutionRecorder().getSolutions()){
			System.out.println(
					String.format("Holy grail: %d %d %d %d %d %d", 
					sol.getIntVal(R[0]),
					sol.getIntVal(R[1]),
					sol.getIntVal(R[2]),
					sol.getIntVal(R[3]),
					sol.getIntVal(r)
			));
			printTree(head, "", sol);
		}
		
		
	}
	
	public static void main(String[] args) {
		Resistance rst = new Resistance();
		rst.solve();
	}

	@Override
	public void onUpdate(IntVar x, IEventType arg1) throws ContradictionException {
		System.out.println(x);
		
	}
}
