all:			AdditionClient.class AdditionServer.class \
			AdditionInterfaceClient.class AdditionInterfaceServer.class

AdditionInterfaceClient.class:	AdditionInterfaceClient.java
			@javac AdditionInterfaceClient.java

AdditionInterfaceServer.class:	AdditionInterfaceServer.java
			@javac AdditionInterfaceServer.java

AdditionClient.class:	AdditionClient.java
			@javac AdditionClient.java

AdditionServer.class:	AdditionServer.java
			@javac AdditionServer.java

clean:
			@rm -f *.class *~

