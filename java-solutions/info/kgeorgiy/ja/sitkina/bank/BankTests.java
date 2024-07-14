package info.kgeorgiy.ja.sitkina.bank;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.io.PrintWriter;
import java.rmi.Naming;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

public class BankTests {
    private static Registry registry;
    private static Bank bank;
    private final static int DEFAULT_PORT = 8888;

    public static void main(final String[] args) {
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(BankTests.class))
                .build();
        LauncherFactory.create().execute(request, listener);
        final TestExecutionSummary summary = listener.getSummary();
        int exitCode = (summary.getTestsFailedCount() == 0) ? 0 : 1;
        summary.printTo(new PrintWriter(System.out));
        System.exit(exitCode);
    }

    @BeforeAll
    public static void getRegistry() throws RemoteException {
        registry = LocateRegistry.getRegistry(Registry.REGISTRY_PORT);
        if (registry == null) {
            registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
        }
    }

    @BeforeEach
    public void createBank() throws RemoteException {
        bank = new RemoteBank(DEFAULT_PORT);
        try {
            UnicastRemoteObject.exportObject(bank, DEFAULT_PORT);
            Naming.rebind("//localhost/bank", bank);
        } catch (Exception ignored) {
        }
        bank.createPerson("kgeorgiy", "info", "001");
        bank.createPerson("andrewzta", "stankevich", "002");
        bank.createPerson("pashka", "i algos", "003");
    }

    @Test
    public void test01_remotePersonInfo() throws RemoteException {
        Person kgeorgiy = bank.getRemotePerson("001");
        Assertions.assertEquals(kgeorgiy.getName(), "kgeorgiy");
        Assertions.assertEquals(kgeorgiy.getSurname(), "info");
        Assertions.assertEquals(kgeorgiy.getPassportNumber(), "001");
        Assertions.assertEquals(kgeorgiy.getAccounts(), List.of());
    }

    @Test
    public void test02_localPersonInfo() throws RemoteException {
        Person kgeorgiy = bank.getLocalPerson("001");
        Assertions.assertEquals(kgeorgiy.getName(), "kgeorgiy");
        Assertions.assertEquals(kgeorgiy.getSurname(), "info");
        Assertions.assertEquals(kgeorgiy.getPassportNumber(), "001");
        Assertions.assertEquals(kgeorgiy.getAccounts(), List.of());
    }

    @Test
    public void test03_incorrectPassport() throws RemoteException {
        Assertions.assertNull(bank.getRemotePerson("011"));
        Assertions.assertNull(bank.getLocalPerson("011"));
    }

    @Test
    public void test04_createAccount() throws RemoteException, IllegalRequestException {
        Account account = bank.createAccount("001", "0");
        Assertions.assertEquals(account.getId(), "0");
        Assertions.assertEquals(account.getAmount(), 0);
    }

    @Test
    public void test05_createAccountViaRemotePersonInterface() throws Exception {
        Person remoteKgeorgiy = bank.getRemotePerson("001");
        Account account = remoteKgeorgiy.createAccount("0");
        account.setAmount(5);
        Assertions.assertEquals(bank.getAccount("001", "0").getId(), account.getId());
        Assertions.assertEquals(bank.getAccount("001", "0").getAmount(), account.getAmount());
    }

    @Test
    public void test06_createLocalPersonAccount() throws Exception {
        Person localKgeorgiy = bank.getLocalPerson("001");
        Account account = localKgeorgiy.createAccount("0");
        account.setAmount(5);
        Assertions.assertEquals(bank.getAccounts("001").size(), 0);
        Assertions.assertEquals(account.getId(), "0");
        Assertions.assertEquals(account.getAmount(), 5);
    }

    @Test
    public void test07_modifyAmount() throws RemoteException, IllegalRequestException {
        Account account = bank.createAccount("001", "0");
        account.setAmount(3);
        Assertions.assertEquals(account.getAmount(), 3);
        account.add(2);
        Assertions.assertEquals(account.getAmount(), 5);
        account.subtract(3);
        Assertions.assertEquals(account.getAmount(), 2);
    }

    @Test
    public void test08_negativeAmounts() throws RemoteException, IllegalRequestException {
        Account account = bank.createAccount("001", "0");
        Assertions.assertThrows(IllegalRequestException.class, () -> account.setAmount(-1));
        Assertions.assertThrows(IllegalRequestException.class, () -> account.add(-1));
        Assertions.assertThrows(IllegalRequestException.class, () -> account.subtract(-1));
        Assertions.assertThrows(IllegalRequestException.class, () -> account.subtract(1));
    }

    @Test
    public void test09_manyAccounts() throws RemoteException, IllegalRequestException {
        bank.createAccount("001", "0");
        bank.createAccount("001", "1");
        bank.createAccount("001", "2");
        Assertions.assertEquals(bank.getRemotePerson("001").getAccounts().size(), 3);
    }

    @Test
    public void test10_incorrectPassportForAccount() {
        Assertions.assertThrows(IllegalRequestException.class, () -> bank.createAccount("011", "0"));
    }

    @Test
    public void test11_remotePersonVisibility() throws RemoteException, IllegalRequestException {
        Person kgeorgiy = bank.getRemotePerson("001");
        Account account = bank.createAccount("001", "0");
        Assertions.assertEquals(kgeorgiy.getAccounts(), List.of(account));
        account.setAmount(3);
        Assertions.assertEquals(kgeorgiy.getAccount("0").getAmount(), 3);
    }

    @Test
    public void test12_localPersonVisibility() throws RemoteException, IllegalRequestException {
        Account account = bank.createAccount("001", "0");
        Person kgeorgiy = bank.getLocalPerson("001");
        account.setAmount(3);
        Assertions.assertEquals(kgeorgiy.getAccounts().size(), 1);
        Assertions.assertEquals(kgeorgiy.getAccount("0").getAmount(), 0);
        kgeorgiy.getAccount("0").setAmount(10);
        Assertions.assertEquals(kgeorgiy.getAccount("0").getAmount(), 10);
        Assertions.assertEquals(bank.getRemotePerson("001").getAccount("0").getAmount(), 3);
    }

    @Test
    public void test13_peopleTransferCheck() throws Exception {
        Person remoteKgeorgiy = bank.getRemotePerson("001");
        Assertions.assertDoesNotThrow(() -> UnicastRemoteObject.unexportObject(remoteKgeorgiy, true));
        Person localKgeorgiy = bank.getLocalPerson("001");
        Assertions.assertThrows(NoSuchObjectException.class, () -> UnicastRemoteObject.unexportObject(localKgeorgiy, true));
    }

    @Test
    public void test14_parallelRequestsBank() throws Exception {
        final int n = 10000;

        bank.createAccount("001", "0");
        Function<Integer, Callable<Void>> getAction = value ->
            () -> {
                try {
                    bank.getRemotePerson("001").getAccount("0").add(value);
                } catch (RemoteException | IllegalRequestException e) {
                    throw new RuntimeException(e);
                }
                return null;
            };
        runParallel(n, getAction);
        Assertions.assertEquals(bank.getRemotePerson("001").getAccount("0").getAmount(), n * (n - 1) / 2);
    }

    @Test
    public void test15_transfer() throws Exception {
        Account kgeorgiyAccount = bank.createAccount("001", "0");
        Account pashkaAccount = bank.createAccount("003", "0");
        kgeorgiyAccount.setAmount(100);

        Assertions.assertEquals(kgeorgiyAccount.getAmount(), 100);
        Assertions.assertEquals(pashkaAccount.getAmount(), 0);

        bank.transfer("001", "0", "003", "0", 75);

        Assertions.assertEquals(kgeorgiyAccount.getAmount(), 25);
        Assertions.assertEquals(pashkaAccount.getAmount(), 75);
    }

    @Test
    public void test16_arabianKgeorgiy() throws Exception {
        String arabianKgeorgiyPassport = "my greatest passport";
        String arabianKgeorgiyAccountId = "kgeorgiy.info";
        Person arabianKgeorgiy = bank.createPerson("جورج", "كورنيف", arabianKgeorgiyPassport);
        Account arabianKgeorgiyAccount = bank.createAccount(arabianKgeorgiyPassport, arabianKgeorgiyAccountId);
        arabianKgeorgiyAccount.add(100);

        Assertions.assertDoesNotThrow(() ->
                bank.transfer(arabianKgeorgiyPassport, arabianKgeorgiyAccountId, arabianKgeorgiyPassport, arabianKgeorgiyAccountId, 100));
        Assertions.assertEquals(arabianKgeorgiyAccount.getAmount(), 100);
        Assertions.assertDoesNotThrow(() ->
                bank.transfer(arabianKgeorgiyPassport, arabianKgeorgiyAccountId, arabianKgeorgiyPassport, arabianKgeorgiyAccountId, 1000));
        Assertions.assertEquals(arabianKgeorgiyAccount.getAmount(), 100);
        Assertions.assertDoesNotThrow(() ->
                bank.transfer(arabianKgeorgiyPassport, arabianKgeorgiyAccountId, arabianKgeorgiyPassport, arabianKgeorgiyAccountId, 0));
        Assertions.assertEquals(arabianKgeorgiyAccount.getAmount(), 100);

        Function<Integer, Callable<Void>> getAction = value ->
                () -> {
                    bank.transfer(arabianKgeorgiyPassport, arabianKgeorgiyAccountId,
                            arabianKgeorgiyPassport, arabianKgeorgiyAccountId, 0);
                    return null;
                };
        runParallel(1000, getAction);
        Assertions.assertEquals(arabianKgeorgiyAccount.getAmount(), 100);
    }

    @Test
    public void test17_circleTransfer() throws Exception {
        int n = 2000;
        int amount = 100;
        for (int i = 0; i < n; i++) {
            String s = Integer.toString(i);
            bank.createPerson(s, s, s);
            Account account = bank.createAccount(s, s);
            account.setAmount(amount);
        }
        Function<Integer, Callable<Void>> getAction = value ->
                () -> {
                    String from = Integer.toString(value);
                    String to = Integer.toString((value + 1) % n);
                    bank.transfer(from, from, to, to, amount);
                    return null;
                };
        runParallel(n, getAction);
        for (int i = 0; i < n; i++) {
            String s = Integer.toString(i);
            Assertions.assertEquals(bank.getAccount(s, s).getAmount(), amount);
        }
    }

    @Test
    public void test18_creatingAlreadyExistingPerson() throws Exception {
        Person person = bank.createPerson("ttt", "sss", "001");
        Assertions.assertEquals(person.getName(), "kgeorgiy");
        Assertions.assertEquals(person.getSurname(), "info");
        Assertions.assertEquals(person.getPassportNumber(), "001");
        Assertions.assertEquals(person.getAccounts(), List.of());
    }

    @Test
    public void test19_clientIllegalInput() {
        Server server = new Server();
        server.start();

        Assertions.assertDoesNotThrow(() -> Client.main(null));
        Assertions.assertDoesNotThrow(() -> Client.main(new String[]{"1", "2"}));
        Assertions.assertDoesNotThrow(() -> Client.main(new String[]{"1", "2", null, "3", "4"}));
        Assertions.assertDoesNotThrow(() -> Client.main(new String[]{"1", "2", "3", "4", "kgeorgiy"}));

        server.close();
    }

    @Test
    public void test20_clientSimpleTest() throws Exception {
        Server server = new Server();
        server.start();
        Bank serverBank = server.getBank();

        Client.main(new String[]{"kgeorgiy", "info", "001", "1", "10"});
        checkKgeorgiyUser(serverBank);

        server.close();
    }

    @Test
    public void test21_invalidRequests() throws Exception {
        Server server = new Server();
        server.start();
        Bank serverBank = server.getBank();

        Client.main(new String[]{"kgeorgiy", "info", "001", "1", "10"});
        Assertions.assertDoesNotThrow(() -> Client.main(new String[]{"andrewzta", "info", "001", "1", "10"}));
        Assertions.assertDoesNotThrow(() -> Client.main(new String[]{"kgeorgiy", "stankevich", "001", "1", "10"}));
        Assertions.assertDoesNotThrow(() -> Client.main(new String[]{"andrewzta", "stankevich", "001", "1", "10"}));
        Assertions.assertDoesNotThrow(() -> Client.main(new String[]{"kgeorgiy", "info", "001", "1", "-10"}));
        checkKgeorgiyUser(serverBank);

        server.close();
    }

    private void checkKgeorgiyUser(Bank serverBank) throws RemoteException {
        Person kgeorgiy = serverBank.getRemotePerson("001");
        Assertions.assertEquals(kgeorgiy.getName(), "kgeorgiy");
        Assertions.assertEquals(kgeorgiy.getSurname(), "info");
        Assertions.assertEquals(kgeorgiy.getPassportNumber(), "001");
        Account account = serverBank.getAccount("001", "1");
        Assertions.assertEquals(account.getAmount(), 10);
    }

    /*@Test
    public void test_fail() {
        Assertions.assertEquals(true, false);
    }*/

    private <E> void runParallel(final int n, Function<Integer, Callable<E>> getAction) throws Exception {
        final ExecutorService executor = Executors.newFixedThreadPool(n);
        try {
            final CompletionService<E> completionService = new ExecutorCompletionService<>(executor);
            for (int i = 0; i < n; i++) {
                completionService.submit(getAction.apply(i));
            }
            for (int i = 0; i < n; i++) {
                completionService.take().get();
            }
        } finally {
            executor.close();
        }
    }
}
