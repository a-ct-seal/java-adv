package info.kgeorgiy.ja.sitkina.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/** Public interface for Bank. */
public interface Bank extends Remote {
    /**
     * Creates a new person with specified name, surname and passport if it does not already exist.
     *
     * @param id person passport
     * @return created or existing account
     */
    Person createPerson(String name, String surname, String id) throws RemoteException;

    /**
     * Returns remote person by passport.
     *
     * @param passport person passport
     * @return person with specified identifier or {@code null} if such person does not exist
     */
    Person getRemotePerson(String passport) throws RemoteException;

    /**
     * Returns local person by passport.
     *
     * @param passport person passport
     * @return person with specified identifier or {@code null} if such person does not exist
     */
    Person getLocalPerson(String passport) throws RemoteException;

    /**
     * Returns all person accounts by person passport.
     *
     * @param passport person passport
     * @return list of person accounts with specified person passport or {@code null} if such person does not exist
     */
    List<Account> getAccounts(String passport) throws RemoteException;

    /**
     * Returns account with specified id owned by person with specified passport.
     *
     * @param passport person passport
     * @param id account id
     * @return specified account or {@code null} if such account does not exist
     */
    Account getAccount(String passport, String id) throws RemoteException;

    /**
     * Creates account with specified id owned by person with specified passport if it does not already exist.
     *
     * @param passport person passport
     * @param id account id
     * @return created or existing account
     * @throws IllegalRequestException if person with specified passport does not exist
     */
    Account createAccount(String passport, String id) throws IllegalRequestException, RemoteException;

    /**
     * Transfers amount from (passportFrom, idFrom) account to (passportTo, amount) account.
     * If accounts are equal, does nothing.
     *
     * @param passportFrom passport of owner of "from" account
     * @param idFrom id of "from" account
     * @param passportTo passport of owner of "to" account
     * @param idTo of "to" account
     * @param amount non-negative amount to transfer
     * @throws IllegalRequestException if amount is negative
     */

    void transfer(String passportFrom, String idFrom, String passportTo, String idTo, long amount)
            throws IllegalRequestException, RemoteException;
}
