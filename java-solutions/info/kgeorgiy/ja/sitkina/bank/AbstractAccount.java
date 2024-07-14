package info.kgeorgiy.ja.sitkina.bank;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Abstract class for Account implementation.
 */
public abstract class AbstractAccount implements Account {
    private final String id;
    private final AtomicLong amount;

    /** Constructs account instance with amount = 0.
     *
     * @param id account id
     * */
    public AbstractAccount(final String id) {
        this(id, 0);
    }

    /** Constructs account instance.
     *
     * @param id account id
     * @param amount initial account amount
     * */
    public AbstractAccount(final String id, long amount) {
        this.id = id;
        this.amount = new AtomicLong(amount);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public long getAmount() {
        return amount.get();
    }

    @Override
    public void setAmount(final long amount) throws IllegalRequestException {
        if (amount < 0) {
            throw new IllegalRequestException("Negative amount");
        }
        this.amount.set(amount);
    }

    @Override
    public void add(final long toAdd) throws IllegalRequestException {
        if (toAdd < 0) {
            throw new IllegalRequestException("Argument must be positive");
        }
        amount.addAndGet(toAdd);
    }

    @Override
    public void subtract(final long toSubtract) throws IllegalRequestException {
        if (toSubtract < 0) {
            throw new IllegalRequestException("Argument must be positive");
        }
        if (amount.get() < toSubtract) {
            throw new IllegalRequestException("Not enough money on the account");
        }
        amount.addAndGet(-toSubtract);
    }
}
