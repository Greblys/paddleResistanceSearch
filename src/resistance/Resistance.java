package resistance;

import org.chocosolver.solver.ResolutionPolicy;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.IntConstraintFactory;
import org.chocosolver.solver.constraints.real.IntEqRealConstraint;
import org.chocosolver.solver.constraints.real.RealConstraint;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.RealVar;
import org.chocosolver.solver.variables.VariableFactory;

import com.sun.org.apache.xerces.internal.impl.dv.xs.DayDV;

public class Resistance {
	
	static Solver s = new Solver();
	IntVar[] R = VariableFactory.boundedArray("R", 4, 1, 1000000, s);
	RealVar[] realR = VariableFactory.real(R, 0.01); //same array in float
	IntVar r = VariableFactory.bounded("r", 1, 1000000, s);
	RealVar realr = VariableFactory.real(r, 0.01);
	RealVar sum = VariableFactory.real("sum", 1, 1000000, 0.01, s);
	
	Resistance(){
		Node head = null;
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
			
			boolean[] times = {n.isMorningIntact(), n.isNoonIntact(), n.isAfternoonIntact(), n.isEveningIntact()};
			RealVar dayR = VariableFactory.real("dayR", 0, 1000000, 0.01, s); //state resistance
			for(int j = 0; j < 4; j++)
				if(times[j]){
					RealConstraint rc = new RealConstraint(
							String.format("%d %d totalDayR", i, j),
							"{0}=1/{1}",
							dayR, realR[j]
					);
					s.post(rc);
				}
			/** @TODO if dayR == 0 ?? **/
			RealConstraint rc = new RealConstraint(
					String.format("%d totalDayR", i),
					"{0}=1/{0}",
					dayR
			);
			s.post(rc);
			RealConstraint rc2 = new RealConstraint(
					String.format("%d totalVoltage", i),
					"{0}=1024*{1}/({1}+{2})",
					n.getVoltage(), realr, dayR
			);
			s.post(rc2);
		}
		
		for(Node n1 : states)
			for(Node n2 : states){
				if(n1 != n2 && n1.isChild(n2))
					n1.addChild(n2);
			}
		
		//printTree(head, "");
		addTreeConstraints(head);
	}
	
	void addTreeConstraints(Node n){
		Node[] children = n.getChildren();
		for(int i = 1; i < 4; i++){
			if(children[i] != null){
				s.post(new RealConstraint(
					String.format("%d %d-%d totalVoltage", n.getDayTimes(), i, i-1),
					"{0}={0} + abs({1} - {2})",
					sum, children[i].getVoltage(), children[i-1].getVoltage()
				));
				s.post(new RealConstraint(
					String.format("%d %d-%d minDifference", n.getDayTimes(), i, i-1),
					"abs({1} - {2}) >= 30",
					children[i].getVoltage(), children[i-1].getVoltage()
				));
			}
		}
		
		for(Node child : n.getChildren())
			addTreeConstraints(child);
	}
	
	void printTree(Node node, String padding){
		if(node != null){
			System.out.print(padding);
			System.out.println(node);
			for(Node child : node.getChildren())
				printTree(child, padding + "    ");
		}
	}
	
	public static Solver getSolver(){
		return s;
	}
	
	public void solve(){
		s.findOptimalSolution(ResolutionPolicy.MAXIMIZE, sum, 0.01);
		System.out.println(String.format("%d %d %d %d %d", 
				R[0].getValue(), R[1].getValue(), R[2].getValue(), R[3].getValue(), r.getValue()
		));
	}
	
	public static void main(String[] args) {
		Resistance rst = new Resistance();
		rst.solve();
	}

}
