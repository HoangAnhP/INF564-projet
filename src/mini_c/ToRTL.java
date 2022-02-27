package mini_c;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.ListIterator;

public class ToRTL extends EmptyVisitor {
	private RTLgraph rtlGraph; // graphe en cours de construction
	private RTLfun rtlFun;
	private RTLfile rtlFile;
	private Label l;
	private Register r;
	private HashMap<String,Register> var_reg;
	private HashMap<String,Register> arg_reg;
	
	ToRTL(){
		this.rtlGraph = new RTLgraph();
		this.var_reg = new HashMap<>();
		this.arg_reg = new HashMap<>();
	}
	
	public RTLfile translate(File file) {
		file.accept(this);
		return this.rtlFile;
	}
}
 
