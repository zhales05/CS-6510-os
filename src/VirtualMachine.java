import java.util.Arrays;
import java.util.Scanner;

public class VirtualMachine {
    public void startShell() {
        final String PROMPT = "MYVM-> ";
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print(PROMPT);
            String userInput = scanner.nextLine();
            String[] inputs = Arrays.stream(userInput.split(" "))
                    .map(String::toLowerCase)
                    .toArray(String[]::new);

            switch (inputs[0]) {
                case "load":
                    System.out.println("You entered load");
                    break;
                case "stop":
                    scanner.close();
                    return;
                default:
                    System.out.println("Unknown input -  please try again.");
                    break;
            }
        }
    }
}
