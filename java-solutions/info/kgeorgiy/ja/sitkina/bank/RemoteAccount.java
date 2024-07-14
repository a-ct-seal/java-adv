package info.kgeorgiy.ja.sitkina.bank;

/**
 * Class for Remote Account.
 */
public class RemoteAccount extends AbstractAccount {

    public RemoteAccount(String id) {
        super(id);
    }

    public RemoteAccount(String id, long amount) {
        super(id, amount);
    }
}
