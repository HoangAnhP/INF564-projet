JAVAC := javac -cp lib/java-cup-11a-runtime.jar
PGM := java -cp lib/java-cup-11a.jar:src mini_c.Main
f := test.c

all: src/mini_c/*.java
	$(JAVAC) src/mini_c/*.java

test-typing:
	$(PGM) $(f)

test:
	$(PGM) $(f)
