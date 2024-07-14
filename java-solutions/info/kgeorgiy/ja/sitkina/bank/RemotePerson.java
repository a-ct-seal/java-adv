package info.kgeorgiy.ja.sitkina.bank;

import java.rmi.RemoteException;
import java.util.List;

/**
 * Class for Remote Person. Uses rmi when getting info about accounts, all modifications are visible through over remote objects.
 */
public class RemotePerson extends AbstractPerson {
    private final RemoteBank bank;

    /** Constructs Person object with specified parameters.
     *
     * @param name name of a person
     * @param surname surname of a person
     * @param passport passport of a person
     * @param bank bank to take info from
     */
    public RemotePerson(String name, String surname, String passport, RemoteBank bank) {
        super(name, surname, passport);
        this.bank = bank;
    }

    @Override
    public Account getAccount(String id) throws RemoteException {
        return bank.getAccount(passport, id);
    }

    @Override
    public List<? extends Account> getAccounts() throws RemoteException {
        return bank.getAccounts(passport);
    }

    @Override
    public Account createAccount(String id) throws IllegalRequestException, RemoteException {
        return bank.createAccount(passport, id);
    }
}
