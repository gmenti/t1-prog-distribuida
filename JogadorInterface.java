import java.rmi.*;

public interface JogadorInterface extends Remote {
  public void inicia() throws RemoteException;

  public void bonifica() throws RemoteException;

  public void verifica() throws RemoteException;
}