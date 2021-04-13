import java.rmi.*;

public interface JogoInterface extends Remote {
  public int registra(String hostname) throws RemoteException;

  public int joga(int id) throws RemoteException;

  public int desiste(int id) throws RemoteException;

  public int finaliza(int id) throws RemoteException;
}
