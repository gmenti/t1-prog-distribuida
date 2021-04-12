all:			Jogador.class Jogo.class \
			JogadorInterface.class JogoInterface.class

JogadorInterface.class:	JogadorInterface.java
			@javac JogadorInterface.java

JogoInterface.class:	JogoInterface.java
			@javac JogoInterface.java

Jogador.class:	Jogador.java
			@javac Jogador.java

Jogo.class:	Jogo.java
			@javac Jogo.java

clean:
			@rm -f *.class *~

