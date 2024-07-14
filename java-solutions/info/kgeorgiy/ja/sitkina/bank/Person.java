package info.kgeorgiy.ja.sitkina.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/** Public interface for Person. */
public interface Person extends Remote {

    /** Returns name of the person.
     *
     * @return name of the person.
     * */
    String getName() throws RemoteException;

    /** Returns surname of the person.
     *
     * @return surname of the person.
     * */
    String getSurname() throws RemoteException;

    /** Returns passport of the person.
     *
     * @return passport of the person.
     * */
    String getPassportNumber() throws RemoteException;

    /** Returns account of the person by id.
     *
     * @param id account id.
     * @return account of the person with specified id. If it doesn't exist, the null is returned.
     * */
    Account getAccount(String id) throws RemoteException;

    /** Returns account of the person by id.
     *
     * @param id account id.
     * @return account of the person with specified id. If it doesn't exist, the null is returned.
     * @throws IllegalRequestException if there is no access to person with current passport
     * */
    Account createAccount(String id) throws IllegalRequestException, RemoteException;

    /** Returns list of accounts of this person.
     *
     * @return list of accounts of this person.
     * */
    List<? extends Account> getAccounts() throws RemoteException;
}
