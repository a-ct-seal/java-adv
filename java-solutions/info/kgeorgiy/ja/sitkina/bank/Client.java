package info.kgeorgiy.ja.sitkina.bank;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Objects;

/**
 * Client for demonstration application.
 */
public class Client {
    /**
     * Simple code to demonstrate bank.
     * Command line arguments: person's name, surname, passport, account id, amount to set on this account (non-negative int)
     * If person or account don't exist, they will be created during the process.
     * If person exists, it's info will be checked, if info is incorrect, money won't be transferred.
     * After account amount update, prints it in standard output.
     * Prints all info about errors in standard error output.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        Bank bank;
        try {
            bank = (Bank) Naming.lookup("//localhost/bank");
        } catch (MalformedURLException e) {
            System.err.println("Malformed URL");
            return;
        } catch (RemoteException | NotBoundException e) {
            System.err.println("Cannot find bank: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        if (args == null || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Expected non-null args");
            return;
        }
        if (args.length != 5) {
            System.err.println("Illegal number of args");
            return;
        }
        String name = args[0], surname = args[1], passport = args[2], accountId = args[3];
        long diff;
        try {
            diff = Long.parseLong(args[4]);
        } catch (final NumberFormatException e) {
            System.err.println("Illegal integer parameter " + e.getMessage());
            return;
        }

        try {
            Person person = bank.getRemotePerson(passport);
            if (person != null) {
                if (!person.getName().equals(name) || !person.getSurname().equals(surname)) {
                    System.out.println("Incorrect person data");
                    return;
                }
            } else {
                person = bank.createPerson(name, surname, passport);
            }
            Account account = person.createAccount(accountId);
            account.setAmount(diff);
            System.out.println("New account " + passport + ":" + accountId + " amount: " + diff);
        } catch (RemoteException e) {
            System.err.println("Remote exception: " + e.getMessage());
        } catch (IllegalRequestException e) {
            System.out.println("Illegal request: " + e.getMessage());
        }
    }
}
