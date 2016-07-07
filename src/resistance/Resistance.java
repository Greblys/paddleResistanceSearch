package resistance;

import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.IntConstraintFactory;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.RealVar;
import org.chocosolver.solver.variables.VariableFactory;

public class Resistance {
	
	static Solver s = new Solver();
	IntVar[] R = VariableFactory.boundedArray("R", 4, 1, 1000000, s);
	IntVar r = VariableFactory.bounded("r", 1, 1000000, s);
	RealVar sum = VariableFactory.real("sum", 1, 1000000, 0.01, s);
	
	Resistance(){
		Node head = null;
		Node[] states = new Node[16];
		
		//calculate voltages
		for(int i = 0; i < 16; i++){
			Node n = new Node(i);
			states[i] = n;
			
			if(i == 15)
				head = n;
			
			boolean[] times = {n.isMorningIntact(), n.isNoonIntact(), n.isAfternoonIntact(), n.isEveningIntact()};
			float R = 0; //state resistance
			for(int j = 0; j < 4; j++)
				if(times[j])
					R += 1.0/rs[j];
			if(R > 0)
				R = 1/R;
			
			float v = 1024 * r / (r + R);
			n.setVoltage(v);			
		}
		
		for(Node n1 : states)
			for(Node n2 : states){
				if(n1 != n2 && n1.isChild(n2))
					n1.addChild(n2);
			}
		
		printTree(head, "");
	}
	
	void printTree(Node node, String padding){
		if(node != null){
			System.out.print(padding);
			System.out.println(node);
			for(Node child : node.getChildren())
				printTree(child, padding + "    ");
		}
	}
	
	void addConstraints(){
		for(int i = 1; i < 4; i++)
			s.post(IntConstraintFactory.arithm(R[i-1], "<", R[i]));
		
		
			
			
	}
	
	public static Solver getSolver(){
		return s;
	}
	
	public static void main(String[] args) {
		Resistance rst = new Resistance();
	}

}
