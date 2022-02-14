package mini_c;

import java.util.LinkedList;
import java.util.HashMap;
import java.util.ListIterator;

public class Typing implements Pvisitor {

	// le rÃ©sultat du typage sera mis dans cette variable
	private File file;
	private HashMap<String, Structure> structs;
	// Pour mettre les variables déclarées. On le mettra à jour au fur et à mesure que l'on entrera dans un bloc.
	private HashMap<String, Typ> var_decled;

	// et renvoyÃ© par cette fonction
	File getFile() {
		if (file == null)
			throw new Error("typing not yet done!");
		return file;
	}

	// Une fonction pour comparer des Typ
	boolean compare_typ(Typ t1, Typ t2){
		if (t1 instanceof Tint){
			return (t2 instanceof Tint || t2 instanceof Ttypenull);
		}
		else if (t1 instanceof Tstructp){
			return (t2 instanceof Tstructp|| t2 instanceof Ttypenull || t2 instanceof Tvoidstar);
		}
		else if (t1 instanceof  Tvoidstar){
			return (t2 instanceof Tvoidstar || t2 instanceof Tstructp);
		}
		else if (t1 instanceof  Ttypenull){
			return (t2 instanceof Ttypenull || t2 instanceof Tint || t2 instanceof Tstructp);
		}
		return false;
	}

	// il faut complÃ©ter le visiteur ci-dessous pour rÃ©aliser le typage

	@Override
	public void visit(Pfile n) {
		// File file à construire
		LinkedList<Decl_fun> funs = new LinkedList<Decl_fun>();

		/* On ajoute les fonctions prédéfinies */
		// putchar
		LinkedList<Decl_var> fun_formals = new LinkedList<Decl_var>();
		fun_formals.add(new Decl_var(new Tint(), "c"));
		funs.add(new Decl_fun(new Tint(), "putchar", fun_formals, new Sskip()));
		// sbrk
		fun_formals = new LinkedList<Decl_var>();
		fun_formals.add(new Decl_var(new Tint(), "n"));
		funs.add(new Decl_fun(new Tvoidstar(), "sbrk", fun_formals, new Sskip()));
		/* On crée le file */
		file = new File(funs);
		// On met les structures déclarées dans structs
		structs = new HashMap<String, Structure>();
		var_decled = new HashMap<String, Typ>();
		// On va visiter tout les Pdecl (Pfun ou pstruct) de n.l
		for (Pdecl d : n.l) {
			// Si Pstruct, on ajoute la structure à un hashmap
			// Si Pfun, accept va ajouter la fonction à funs
			d.accept(this);
		}
		// On vérifie qu'il y a une fonction int main()
		boolean main = false;
		for (Decl_fun fun : funs){
			if (fun.fun_name.equals("main") && (compare_typ(fun.fun_typ, new Tint()) )){
				main = true;
				break;
			}
		}
		if (!main) throw new Error("Il n'y a pas de fonction int main()");
	}

	@Override
	public Tint visit(PTint n) {
		Tint x = new Tint();
		return x;
	}

	@Override
	public Tstructp visit(PTstruct n) {
		String s = n.id;
		if (!structs.containsKey(s)) throw new Error("Structure "+s+" inconnue");
		return new Tstructp(structs.get(s));
	}

	@Override
	public Econst visit(Pint n) {
		if (n.n==0) return new Econst(new Ttypenull(), 0);
		else return new Econst(new Tint(), n.n);
	}

	@Override
	public Eaccess_local visit(Pident n) {
		if (!var_decled.containsKey(n.id)) throw new Error("Variable "+n.id+" inconnue");
		Typ typ = var_decled.get(n.id);
		return new Eaccess_local(typ, n.id);
	}

	@Override
	public Eunop visit(Punop n) {
		Expr e1 = n.e1.accept(this);
		Tint typ_int = new Tint();
		if (n.op.equals(Unop.Uneg) && !compare_typ(e1.typ, typ_int)){
			throw new Error("Uneg attend un type Tint");
		}
		return new Eunop(new Tint(), n.op, e1);
	}

	@Override
	public Expr visit(Passign n) {
		Expr e2 = n.e2.accept(this);
		Expr e1 = n.e1.accept(this);
		// e1 est assignée, on ne va pas la lire
		// if (e1.typ.getClass().equals(e2.typ.getClass())) throw new Error("Type incompatible entre les expressions pour assignation");
		if (e1 instanceof Eaccess_local) {
			Eaccess_local e1_cast = (Eaccess_local) e1;
			return new Eassign_local(e1.typ, e1_cast.i, e2);
		}
		else {
			Eaccess_field e1_cast = (Eaccess_field) e1;
			return new Eassign_field(e1.typ, e1_cast, e1_cast.f, e2);
		}

	}

	@Override
	public Ebinop visit(Pbinop n) {
		Expr e1, e2;
		e1 = n.e1.accept(this);
		e2 = n.e2.accept(this);
		Tint typ_int = new Tint();
		switch (n.op){
			case Beq, Bneq, Blt, Ble, Bgt, Bge :
				if (! compare_typ(e1.typ, e2.typ)) throw new Error(n.op+" attend des expressions de même type");
				break;
			case Badd, Bsub, Bmul, Bdiv :
				if (!(compare_typ(e1.typ, typ_int) && compare_typ(e2.typ, typ_int))) throw new Error(n.op+" attend des expressions de type int Badd, Bsub");
				break;
		}

		return new Ebinop(typ_int, n.op, e1, e2);
	}

	@Override
	public Eaccess_field visit(Parrow n) {
		Expr e = n.e.accept(this);

		if (e.typ instanceof Tstructp && ((Tstructp) e.typ).s.fields.containsKey(n.f)) {
			Field f = ((Tstructp) e.typ).s.fields.get(n.f);
			return new Eaccess_field(f.field_typ, e, f);
		}
		else throw new Error("Pas de champ "+n.f+" pour l'expression");
	}

	@Override
	public Ecall visit(Pcall n) {
		// On vérif que la fonction existe
		Decl_fun fun_call = null;
		for (Decl_fun fun : file.funs){
			if (fun.fun_name.equals(n.f)){
				fun_call = fun;
				break;
			}
		}
		if (fun_call==null) throw new Error("Appel à la fonction "+n.f+" qui est inconnue");
		// On vérifie le type des arguments tout en les typant
		LinkedList<Expr> el = new LinkedList<Expr>();
		// Moche, à corriger
		ListIterator<Decl_var> iter_decl = (ListIterator<Decl_var>) fun_call.fun_formals.iterator();
		ListIterator<Pexpr> iter_expr = (ListIterator<Pexpr>) n.l.iterator();

		while (iter_expr.hasNext()){
			if (!iter_decl.hasNext()) throw new Error("Mauvais nombre d'arguments passé à la fonction "+n.f);
			// Sinon, on type les arguments en entrée
			Expr e = iter_expr.next().accept(this);
			Decl_var declvar = iter_decl.next();
			if (! compare_typ(e.typ, declvar.t)) throw new Error("Type incompatible dans les arguments passés à la fonction "+n.f);
			// Si le typage est ok, on ajoute à el
			el.add(e);
		}
		if (iter_decl.hasNext()) throw new Error("Mauvais nombre d'arguments passé à la fonction " + n.f);
		return new Ecall(fun_call.fun_typ, n.f, el);
	}

	@Override
	public Esizeof visit(Psizeof n) {
		if (!structs.containsKey(n.id)) throw new Error("Structure "+n.id+" inconnue");

		return new Esizeof(new Tint(), structs.get(n.id));
	}

	@Override
	public Sskip visit(Pskip n) {
		return new Sskip();

	}

	@Override
	public Sexpr visit(Peval n) {
		Expr e = n.e.accept(this);

		return new Sexpr(e);
	}

	@Override
	public Sif visit(Pif n) {
		Expr e = n.e.accept(this); // accept de Pexpr doit return Expr
		Stmt s1 = n.s1.accept(this);
		Stmt s2 = n.s2.accept(this);

		return new Sif(e, s1, s2);

	}

	@Override
	public Swhile visit(Pwhile n) {
		Expr e = n.e.accept(this);
		Stmt s = n.s1.accept(this);

		return new Swhile(e,s);
	}

	@Override
	public Sblock visit(Pbloc n) {
		LinkedList<Decl_var> dl = new LinkedList<Decl_var>();
		LinkedList<Stmt> sl = new LinkedList<Stmt>();
		// On va mettre les déclarations en doublons ici pour les récupérer à la fin
		HashMap<String, Typ> already_decl = new HashMap<String, Typ>();
		LinkedList<String> new_decl = new LinkedList<String>();

		// On fait les declaration de variable et on les ajoute à var_decled
		// On commence par ajouter les nouvelles déclarations dans new_decl pour vérifier qu'il n'y a pas de double déclaration
		for (Pdeclvar pdvar : n.vl){
			if (new_decl.contains(pdvar.id)) throw new Error("Double déclaration d'une même variable au sein du même bloc");
			new_decl.add(pdvar.id);
		}
		// On ajoute les déclarations de variable
		for (Pdeclvar pdvar : n.vl) {
			Typ t = pdvar.typ.accept(this);
			dl.add(new Decl_var(t, pdvar.id));
			// On vérifie qu'il n'existe pas déjà une variable du même nom
			if (var_decled.containsKey(pdvar.id)){
				// On récupère l'ancienne et on la sauvegarde
				already_decl.put(pdvar.id, var_decled.get(pdvar.id));
				var_decled.remove(pdvar.id);
			}
			var_decled.put(pdvar.id,t);

		}
		// On type les instructions avec la connaissance de ces variables déclarées
		for (Pstmt pstm: n.sl) {
			sl.add(pstm.accept(this));
		}

		// On retire les variables déclarées dans ce bloc à la fin et on restaure les doublons
		for (Pdeclvar pdvar : n.vl){
			var_decled.remove(pdvar.id);
		}
		for (String id : already_decl.keySet()) {
			var_decled.put(id, already_decl.get(id));
		}
		return new Sblock(dl, sl);
	}

	@Override
	public Sreturn visit(Preturn n) {
		Expr e = n.e.accept(this);
		return new Sreturn(e);
	}

	@Override
	public void visit(Pstruct n) {
		// On vérifie que la structure n'existe pas déjà
		String str_name = n.s;
		if (structs.containsKey(str_name)) throw new Error("Déclaration d'une structure déjà existante");
		// On construit la structure et l'ajoute au HashMap structs
		Structure struct;
		struct = new Structure(str_name);
		// On ajoute la structure au HashMap structs (maintenant comme ça struct pourra posséder des champs de lui-même)
		structs.put(str_name, struct);
		// On remplit le HashMap struct.fields
		for (Pdeclvar declvar : n.fl) {
			// On vérifie qu'on a pas des déclarations de champ en double
			if (struct.fields.containsKey(declvar.id)) throw new Error("Le champ "+declvar.id+" est déclaré plusieurs fois dans la structure "+str_name);
			Field field = new Field(declvar.id, declvar.typ.accept(this));
			struct.fields.put(declvar.id, field);
		}
	}

	@Override
	public void visit(Pfun n) {
		Decl_fun fun;
		Typ fun_typ = n.ty.accept(this);
		String fun_name = n.s;
		// On vérifie qu'il n'y a pas des fonctions déclarées en double
		for (Decl_fun decl : file.funs){
			if (decl.fun_name.equals(fun_name)) throw new Error("Fonction redeclarée");
		}

		LinkedList<Decl_var> fun_formals = new LinkedList<Decl_var>();
		// On ajoute les déclarations de variable
		for (Pdeclvar pdeclvar : n.pl){
			Typ t = pdeclvar.typ.accept(this);
			// On vérifie que la variable déclarée possède un nom différent des précédentes
			for (Decl_var var : fun_formals) {
				if (var.name.equals(pdeclvar.id)) throw new Error("La variable "+pdeclvar.id+" est déclarée plusieurs fois dans la fonction "+fun_name);
			}
			fun_formals.add(new Decl_var(t, pdeclvar.id));
			// On ajoute aussi les variables déclarées à var_decl pour pouvoir typer les Stmt correctement
			// TO DO variable globale à remettre
			var_decled.put(pdeclvar.id, t);
		}

		// Temporairement pour que la fonction qu'on déclare soit connue et qu'elle puisse donc s'appeler elle même dans fun_body
		Stmt fun_body = new Sskip();
		// On ajoute la function à funs
		fun = new Decl_fun(fun_typ, fun_name, fun_formals, fun_body);
		file.funs.add(fun);

		// On type correctement ses Pstmt
		fun_body = n.b.accept(this); // n.b est un Pbloc donc un Pstmt
		// On peut modifier correctement la fonction
		file.funs.getLast().fun_body = fun_body;

		// Maintenant que le statement est typé, on retire les variables locales déclarées
		for (Pdeclvar pdeclvar : n.pl){
			var_decled.remove(pdeclvar.id);
		}
	}

}
