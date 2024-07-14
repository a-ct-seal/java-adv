package info.kgeorgiy.ja.sitkina.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;

/** Public interface for Account. */
public interface Account extends Remote {
    /** Returns account identifier (without person passport).
     *
     * @return account id.
     * */
    String getId() throws RemoteException;

    /** Returns amount of money in the account.
     *
     * @return amount of money in the account
     * */
    long getAmount() throws RemoteException;

    /** Sets amount of money in the account.
     *
     * @param amount amount to set.
     * @throws IllegalRequestException if amount is negative
     * */
    void setAmount(long amount) throws RemoteException, IllegalRequestException;

    /** Adds amount of money in the account.
     *
     * @param toAdd non-negative amount to add to account
     * @throws IllegalRequestException if toAdd is negative
     * */
    void add(long toAdd) throws RemoteException, IllegalRequestException;

    /** Subtracts amount of money in the account.
     *
     * @param toSubtract non-negative amount to subtract from account
     * @throws IllegalRequestException if toSubtract is negative or if current account amount is less than toSubtract
     * */
    void subtract(long toSubtract) throws RemoteException, IllegalRequestException;
}
