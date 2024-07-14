package info.kgeorgiy.ja.sitkina.bank;

/**
 * Class for Local Account.
 */
public class LocalAccount extends AbstractAccount {

    public LocalAccount(String id) {
        super(id);
    }

    public LocalAccount(String id, long amount) {
        super(id, amount);
    }
}
