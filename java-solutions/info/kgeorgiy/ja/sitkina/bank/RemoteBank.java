package info.kgeorgiy.ja.sitkina.bank;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * RemoteBank implementation.
 */
public class RemoteBank implements Bank {
    private final int port;
    private final ConcurrentMap<String, RemotePerson> people = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ConcurrentHashMap<String, RemoteAccount>> accounts = new ConcurrentHashMap<>();

    /** Creates new RemoteBank on specified port.
     *
     * @param port port to use for RemoteBank
     */
    public RemoteBank(final int port) {
        this.port = port;
    }

    @Override
    public Person createPerson(String name, String surname, String id) throws RemoteException {
        final RemotePerson person = new RemotePerson(name, surname, id, this);
        if (people.putIfAbsent(id, person) == null) {
            UnicastRemoteObject.exportObject(person, port);
            accounts.put(id, new ConcurrentHashMap<>());
            return person;
        } else {
            return getRemotePerson(id);
        }
    }

    @Override
    public Person getRemotePerson(final String passport) {
        return people.get(passport);
    }

    @Override
    public Person getLocalPerson(final String passport) throws RemoteException {
        if (!people.containsKey(passport)) {
            return null;
        }
        RemotePerson person = people.get(passport);
        ConcurrentMap<String, LocalAccount> accounts = new ConcurrentHashMap<>(
                person.getAccounts().stream()
                        .map(remoteAccount -> {
                            try {
                                return new LocalAccount(remoteAccount.getId(), remoteAccount.getAmount());
                            } catch (RemoteException e) {
                                // accounts are local, RemoteException never thrown
                                throw new IllegalStateException(e);
                            }
                        })
                        .collect(Collectors.toMap(LocalAccount::getId, localAccount -> localAccount)));
        return new LocalPerson(
                person.getName(),
                person.getSurname(),
                person.getPassportNumber(),
                accounts);
    }

    @Override
    public List<Account> getAccounts(String passport) throws RemoteException {
        if (!accounts.containsKey(passport)) {
            return null;
        }
        return accounts.get(passport).values().stream().map(remoteAccount -> (Account) remoteAccount).toList();
    }

    @Override
    public Account getAccount(String passport, String id) {
        if (!accounts.containsKey(passport) ||
                !accounts.get(passport).containsKey(id)) {
            return null;
        }
        return accounts.get(passport).get(id);
    }

    @Override
    public Account createAccount(String passport, String id) throws IllegalRequestException, RemoteException {
        if (!accounts.containsKey(passport)) {
            throw new IllegalRequestException("no person with passport " + passport);
        }
        RemoteAccount account = new RemoteAccount(id);
        if (accounts.get(passport).putIfAbsent(id, account) == null) {
            UnicastRemoteObject.exportObject(account, port);
            return account;
        } else {
            return getAccount(passport, id);
        }
    }

    @Override
    public void transfer(String passportFrom, String idFrom, String passportTo, String idTo, long amount)
            throws IllegalRequestException, RemoteException {
        if (amount < 0) {
            throw new IllegalRequestException("Amount must be positive");
        }
        Account from = getAccount(passportFrom, idFrom);
        Account to = getAccount(passportTo, idTo);
        Account first, second;
        if (Objects.equals(passportFrom, passportTo) && Objects.equals(idFrom, idTo)) {
            return;
        }
        if (passportTo.compareTo(passportFrom) < 0 ||
                Objects.equals(passportTo, passportFrom) && idTo.compareTo(idFrom) < 0) {
            first = getAccount(passportTo, idTo);
            second = getAccount(passportFrom, idFrom);
        } else {
            first = getAccount(passportFrom, idFrom);
            second = getAccount(passportTo, idTo);
        }
        synchronized (first) {
            synchronized (second) {
                from.subtract(amount);
                to.add(amount);
            }
        }
    }
}
