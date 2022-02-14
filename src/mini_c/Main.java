package mini_c;

import java.io.IOException;
import java.io.InputStream;

public class Main {
	
	static boolean parse_only = false;
	static boolean type_only = false;
	static String file = null; //null
	static String output = null;
	
	static void usage() {
		System.err.println("mini-c [--parse-only] [--type-only] file.c");
		System.exit(1);
	}
	
	public static void main(String[] args) throws Exception {
		int index = 0;
		for (String arg: args){
			// System.out.println(arg);
			if (arg.equals("--parse-only"))
				parse_only= true;
			else if (arg.equals("--type-only"))
				type_only = true;
			else if (arg.equals("-o")) {
				try{
					output = args[index+1];
					if(!ouput.endsWith(".s"))
						usage();
				}
				catch(ArrayIndexOutOfBoundsException e){
					//usage();
				}
			}
			else {
				if (file != null) usage();
				if (!arg.endsWith(".c")) usage();
				if (arg.endsWith(".c") && index == args.length() - 1)
					file = arg;
				if (output == null)
					output = arg.substring(0, arg.length() - 1) + "s";
			}
			index++;
		}

		if (file == null) file = "test.c"; // pour faciliter les tests
		
        java.io.Reader reader = new java.io.FileReader(file);
        Lexer lexer = new Lexer(reader);
        System.out.println("parsing...");
        MyParser parser = new MyParser(lexer);
        try {
        	Pfile f = (Pfile) parser.parse().value;
            if (parse_only) System.exit(0);
            System.out.println("typing...");
            Typing typer = new Typing();
            typer.visit(f);
            File tf = typer.getFile();
            if (type_only) System.exit(0);
        } catch (Error e) {
        	System.out.println("error: " + e.getMessage());
        	System.exit(1);
        } catch (Exception e) {
        	System.out.println("error: " + e.getMessage());
        	System.exit(1);
		}
	}
	
	static void cat(InputStream st) throws IOException {
		while (st.available() > 0) {
      			System.out.print((char)st.read());
    		}
	}
	
}
