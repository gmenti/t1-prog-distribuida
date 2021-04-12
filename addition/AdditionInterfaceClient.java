import java.rmi.Remote;
import java.rmi.RemoteException;

public interface AdditionInterfaceClient extends Remote {
	public int Result(int val) throws RemoteException;
}
