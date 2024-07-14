package info.kgeorgiy.ja.sitkina.bank;

import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * Class for Local Person. Uses serialization when getting from bank, then all modifications remain local.
 */

public class LocalPerson extends AbstractPerson {
    private final ConcurrentMap<String, LocalAccount> accounts;

    /**
     * Constructs LocalPerson with specified name, surname, passport and accounts.
     *
     * @param name     name of a person
     * @param surname  surname of a person
     * @param passport passport of a person
     * @param accounts accounts of a person
     */
    public LocalPerson(String name, String surname, String passport,
                       ConcurrentMap<String, LocalAccount> accounts) {
        super(name, surname, passport);
        this.accounts = accounts;
    }

    @Override
    public Account getAccount(String id) {
        if (accounts.containsKey(id)) {
            return accounts.get(id);
        }
        return null;
    }

    // :NOTE: local account expected
    @Override
    public Account createAccount(String id) {
        return accounts.computeIfAbsent(id, t -> new LocalAccount(id));
    }

    @Override
    public List<? extends Account> getAccounts() {
        return accounts.values().stream().toList();
    }
}
