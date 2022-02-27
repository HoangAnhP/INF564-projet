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
	private HashMap<String, Register> var_reg;
	private HashMap<String, Register> arg_reg;

	ToRTL() {
		this.rtlGraph = new RTLgraph();
		this.var_reg = new HashMap<>();
		this.arg_reg = new HashMap<>();
	}

	public RTLfile translate(File file) {
		file.accept(this);
		return this.rtlFile;
	}

	public void visit(Unop n) {
	}

	public void visit(Binop n) {
	}

	public void visit(String n) {
	}

	public void visit(Tint n) {
	}

	public void visit(Tstructp n) {
	}

	public void visit(Tvoidstar n) {
	}

	public void visit(Ttypenull n) {
	}

	public void visit(Structure n) {
	}

	public void visit(Field n) {
	}

	@Override
	public void visit(Decl_var n) {
		r = new Register();
		this.rtlFun.locals.add(r);
		this.var_reg.put(n.name, r);
	}

	@Override
	public void visit(Expr n) {
		n.accept(this);
	}

	public void visit(Econst n) {
		Rconst nRc = new Rconst(n.i, this.r, this.l);
		this.l = this.rtlGraph.add(nRc);
	}

	public void visit(Eaccess_local n) {
		Register rn = new Register();
		if(this.var_reg.get(n.i) == null)
			rn = this.arg_reg.get(n.i);
		else
			rn = this.var_reg.get(n.i);
		
		Rmbinop nRmb = new Rmbinop(Mbinop.Mmov, rn, this.r, this.l);
		this.l = this.rtlGraph.add(nRmb);
	}

	 @Override
	public void visit(Eaccess_field n) {
		Register rn = new Register();
		Rload nRl = new Rload(rn, n.f.hashCode(), this.r, this.l);
		this.l = this.rtlGraph.add(nRl);
		this.r = rn;
		visit(n.e);
	}

	@Override
	public void visit(Eassign_local n) {
		Register rn = new Register();
		if(this.var_reg.get(n.i) == null)
			rn = this.arg_reg.get(n.i);
		else 
			rn = this.var_reg.get(n.i);
		
		Rmbinop nRmb = new Rmbinop(Mbinop.Mmov, this.r, rn, this.l);
		this.l = this.rtlGraph.add(nRmb);
		visit(n.e);
	}
	
	@Override
	public void visit(Eassign_field n) {
		Register rn = new Register();
		Rstore nRl = new Rstore(rn,  this.r, n.f.hashCode(), this.l);
		this.l = this.rtlGraph.add(nRl);
		visit(n.e1);
		this.r = rn;
		visit(n.e2);
	}

	public void visit(Eunop n) {
	}

	public void visit(Ebinop n) {
	}

	public void visit(Ecall n) {
	}

	public void visit(Esizeof n) {
		Rconst nRc = new Rconst(n.s.fields.size(), this.r, this.l);
		this.l = this.rtlGraph.add(nRc);
	}

	public void visit(Sskip n) {
	}

	public void visit(Sexpr n) {
		n.accept(this);
	}

	public void visit(Sif n) {
	}

	public void visit(Swhile n) {
	}

	public void visit(Sblock n) {
	}

	@Override
	public void visit(Sreturn n) {
		this.l = this.rtlFun.exit;
		this.r = this.rtlFun.result;
		visit(n.e);
	}

	@Override
	public void visit(Decl_fun n) {
		
	}

	@Override
	public void visit(File n) {
		this.rtlFile = new RTLfile();
		int i;
		for(i = 0; i < n.funs.size(); i++) {
			Decl_fun func = n.funs.get(i);
			func.accept(this);
			this.rtlFile.funs.add(this.rtlFun);
			this.var_reg = new HashMap<>();
			this.arg_reg = new HashMap<>();
		}
	}
}
 
