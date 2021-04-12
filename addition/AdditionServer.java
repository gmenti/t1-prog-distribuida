import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.LocateRegistry;

public class AdditionServer extends UnicastRemoteObject implements AdditionInterfaceServer {
	private static volatile int result;
	private static volatile boolean changed;
	private static volatile String remoteHostName;

	public AdditionServer() throws RemoteException {
	}
	
	public static void main(String[] args) throws RemoteException {
		if (args.length != 1) {
			System.out.println("Usage: java AdditionServer <server ip>");
			System.exit(1);
		}

		try {
			System.setProperty("java.rmi.server.hostname", args[0]);
			LocateRegistry.createRegistry(52369);
			System.out.println("java RMI registry created.");
		} catch (RemoteException e) {
			System.out.println("java RMI registry already exists.");
		}

		try {
			String server = "rmi://" + args[0] + ":52369/Hello";
			Naming.rebind(server, new AdditionServer());
			System.out.println("Addition Server is ready.");
		} catch (Exception e) {
			System.out.println("Addition Serverfailed: " + e);
		}
		
		while (true) {
			if (changed == true) {
				changed = false;

				String connectLocation = "rmi://" + remoteHostName + ":52369/Hello2";

				AdditionInterfaceClient hello2 = null;
				try {
					System.out.println("Calling client back at : " + connectLocation);
					hello2 = (AdditionInterfaceClient) Naming.lookup(connectLocation);
				} catch (Exception e) {
					System.out.println ("Callback failed: ");
					e.printStackTrace();
				}

				try {
					hello2.Result(result);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException ex) {}
		}
	}
	
	public int Add(int a, int b) {
		result = a + b;
		changed = true;
		try {
			remoteHostName = getClientHost();
		} catch (Exception e) {
			System.out.println ("Failed to get client IP");
			e.printStackTrace();
		}
		
		return 1;
	}
}
