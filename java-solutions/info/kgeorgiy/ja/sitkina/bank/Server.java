package info.kgeorgiy.ja.sitkina.bank;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Server for demonstration application.
 */
public final class Server {
    private final static int DEFAULT_PORT = 8881;
    private Registry registry;
    private Bank bank;

    /**
     * Starts rmi registry and new bank instance.
     */
    public void start() {
        try {
            registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
        } catch (RemoteException e) {
            System.err.println("Cannot create rmi registry");
            e.printStackTrace();
            System.exit(1);
        }

        bank = new RemoteBank(DEFAULT_PORT);
        try {
            UnicastRemoteObject.exportObject(bank, DEFAULT_PORT);
            Naming.rebind("//localhost/bank", bank);
        } catch (final RemoteException e) {
            System.err.println("Cannot export object: " + e.getMessage());
            e.printStackTrace();
        } catch (final MalformedURLException e) {
            System.err.println("Malformed URL");
        }
    }

    /* package-private */ Bank getBank() {
        return bank;
    }

    /**
     * Kills rmi registry and bank.
     */
    public void close() {
        try {
            UnicastRemoteObject.unexportObject(registry, true);
            Naming.unbind("//localhost/bank");
        } catch (RemoteException | NotBoundException | MalformedURLException ignored) {
        }
    }
}
