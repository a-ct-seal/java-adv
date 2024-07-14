package info.kgeorgiy.ja.sitkina.bank;

import java.rmi.RemoteException;

/**
 * Abstract class for Person implementation.
 */
public abstract class AbstractPerson implements Person {
    private final String name;
    private final String surname;
    protected final String passport;

    /** Constructs Person object with specified parameters.
     *
     * @param name name of a person
     * @param surname surname of a person
     * @param passport passport of a person
     */
    public AbstractPerson(String name, String surname, String passport) {
        this.name = name;
        this.surname = surname;
        this.passport = passport;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSurname() {
        return surname;
    }

    @Override
    public String getPassportNumber() {
        return passport;
    }
}
