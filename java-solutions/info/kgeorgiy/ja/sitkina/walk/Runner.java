package info.kgeorgiy.ja.sitkina.walk;

public class Runner {
    public static void run(String[] args, int depth) {
        try {
            Walker.walk(args, depth, true);
        } catch (WorkFilesException | IllegalInputException e) {
            System.err.println(e.getMessage());
        }
    }
}
