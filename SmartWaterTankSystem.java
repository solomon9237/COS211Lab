import java.util.Scanner;

/**
 * IFT 211 Mid-Semester Lab Assessment
 * Smart Water Tank Monitoring & Control System
 *
 * Educational Java simulation of the digital logic.
 * Sensor model: one-hot (exactly one of S0, S1, S2, S3 is HIGH).
 *
 * Compile: javac SmartWaterTankSystem.java
 * Run:     java SmartWaterTankSystem
 */
public class SmartWaterTankSystem {
    private static final Scanner INPUT = new Scanner(System.in);

    // Current physical sensor state (one-hot inputs).
    private final boolean[] sensors = new boolean[4];

    // Two D flip-flops used as the level register.
    private int storedLevel = 0; // Q1 Q0

    // Two-bit saturating counter used to count FULL clock edges.
    private int fullCounter = 0; // Q1 Q0, states 00, 01, 10, 11

    private boolean resetActive = false;
    private long clockEdges = 0;

    public static void main(String[] args) {
        SmartWaterTankSystem system = new SmartWaterTankSystem();
        system.run();
    }

    private void run() {
        System.out.println("==============================================");
        System.out.println(" IFT 211: SMART WATER TANK MONITORING SYSTEM");
        System.out.println(" Java digital-logic simulation");
        System.out.println("==============================================");

        boolean running = true;
        while (running) {
            printMenu();
            int choice = readInt("Choose an option: ");

            switch (choice) {
                case 1:
                    chooseWaterLevel();
                    break;
                case 2:
                    clockTick();
                    break;
                case 3:
                    resetSystem();
                    break;
                case 4:
                    showStatus();
                    break;
                case 5:
                    runAutomatedTests();
                    break;
                case 6:
                    showLogicEquations();
                    break;
                case 7:
                    running = false;
                    System.out.println("Program ended. Goodbye.");
                    break;
                default:
                    System.out.println("Invalid option. Enter a number from 1 to 7.");
            }
        }
    }

    private void printMenu() {
        System.out.println("\n--------------- MAIN MENU ----------------");
        System.out.println("1. Set water level / sensor inputs");
        System.out.println("2. Apply one clock pulse");
        System.out.println("3. Reset system");
        System.out.println("4. View system status");
        System.out.println("5. Run automated tests");
        System.out.println("6. Show Boolean equations");
        System.out.println("7. Exit");
        System.out.println("------------------------------------------");
    }

    private void chooseWaterLevel() {
        System.out.println("\nSelect the simulated sensor state:");
        System.out.println("0. Empty (S0=1)  -> binary 00");
        System.out.println("1. 25%   (S1=1)  -> binary 01");
        System.out.println("2. 50%   (S2=1)  -> binary 10");
        System.out.println("3. Full  (S3=1)  -> binary 11");

        int level = readInt("Enter level (0-3): ");
        if (level < 0 || level > 3) {
            System.out.println("Invalid level. No sensor changes were made.");
            return;
        }

        // Enforce one-hot sensor input: exactly one sensor is HIGH.
        for (int i = 0; i < sensors.length; i++) {
            sensors[i] = (i == level);
        }

        System.out.println("Sensor inputs updated.");
        System.out.println("S0=" + bit(sensors[0]) + " S1=" + bit(sensors[1])
                + " S2=" + bit(sensors[2]) + " S3=" + bit(sensors[3]));
        System.out.println("Note: the register changes on a clock pulse, not immediately.");
        showStatus();
    }

    private void clockTick() {
        clockEdges++;

        if (resetActive) {
            storedLevel = 0;
            fullCounter = 0;
            resetActive = false;
            System.out.println("Reset was active; register and counter remain cleared.");
            showStatus();
            return;
        }

        // Combinational encoder outputs from current sensor inputs.
        int encodedLevel = encodeLevel();
        boolean full = isFull(encodedLevel);

        // D flip-flop register: capture encoder bits at the active clock edge.
        storedLevel = encodedLevel;

        // Synchronous saturating counter:
        // FULL=0 -> 00; FULL=1 -> advance 00->01->10->11 and hold at 11.
        if (!full) {
            fullCounter = 0;
        } else if (fullCounter < 3) {
            fullCounter++;
        }

        System.out.println("Rising clock edge #" + clockEdges + " applied.");
        showStatus();
    }

    private void resetSystem() {
        resetActive = true;
        // Model reset as an immediate reset of the sequential components.
        storedLevel = 0;
        fullCounter = 0;
        resetActive = false;
        System.out.println("RESET activated: level register and overflow counter cleared.");
        showStatus();
    }

    private int encodeLevel() {
        // Boolean equations:
        // L1 = S2 OR S3
        // L0 = S1 OR S3
        int l1 = (sensors[2] || sensors[3]) ? 1 : 0;
        int l0 = (sensors[1] || sensors[3]) ? 1 : 0;
        return (l1 << 1) | l0;
    }

    private boolean isFull(int level) {
        int l1 = (level >> 1) & 1;
        int l0 = level & 1;
        // FULL = L1 AND L0
        return (l1 & l0) == 1;
    }

    private boolean isPumpOn() {
        // PUMP = NOT(FULL)
        return !isFull(encodeLevel());
    }

    private boolean isAlarmOn() {
        int q1 = (fullCounter >> 1) & 1;
        int q0 = fullCounter & 1;
        // ALARM = Q1 AND Q0
        return (q1 & q0) == 1;
    }

    private void showStatus() {
        int currentLevel = encodeLevel();
        boolean full = isFull(currentLevel);

        System.out.println("\n--------------- SYSTEM STATUS -------------");
        System.out.println("Sensors: S0=" + bit(sensors[0])
                + " S1=" + bit(sensors[1])
                + " S2=" + bit(sensors[2])
                + " S3=" + bit(sensors[3]));
        System.out.println("Encoded current level: " + binary2(currentLevel)
                + " (" + currentLevel + ")");
        System.out.println("Tank status: " + levelName(currentLevel));
        System.out.println("Stored register Q1Q0: " + binary2(storedLevel)
                + " (" + storedLevel + ")");
        System.out.println("FULL signal: " + bit(full));
        System.out.println("Pump: " + (isPumpOn() ? "ON" : "OFF"));
        System.out.println("Full counter Q1Q0: " + binary2(fullCounter)
                + " (" + fullCounter + "/3)");
        System.out.println("Overflow alarm: " + (isAlarmOn() ? "ON" : "OFF"));
        System.out.println("Seven-segment digit (stored level): " + storedLevel);
        System.out.println("Segments a b c d e f g: " + sevenSegment(storedLevel));
        System.out.println("-------------------------------------------");
    }

    private void showLogicEquations() {
        System.out.println("\n----------- BOOLEAN EQUATIONS -------------");
        System.out.println("L1 = S2 OR S3");
        System.out.println("L0 = S1 OR S3");
        System.out.println("FULL = L1 AND L0");
        System.out.println("PUMP = NOT(FULL)");
        System.out.println("Register: D1=L1, D0=L0; capture on rising clock edge");
        System.out.println("Counter when FULL=0: next state = 00");
        System.out.println("Counter when FULL=1: 00->01->10->11->11");
        System.out.println("D_Q0 = FULL AND (Q1 OR NOT Q0)");
        System.out.println("D_Q1 = FULL AND (Q1 OR Q0)");
        System.out.println("ALARM = Q1 AND Q0");
        System.out.println("Seven-segment equations (common cathode, active HIGH):");
        System.out.println("a = Q1 OR NOT Q0");
        System.out.println("b = 1");
        System.out.println("c = NOT Q1 OR Q0");
        System.out.println("d = Q1 OR NOT Q0");
        System.out.println("e = NOT Q0");
        System.out.println("f = NOT Q1 AND NOT Q0");
        System.out.println("g = Q1");
        System.out.println("-------------------------------------------");
    }

    private void runAutomatedTests() {
        System.out.println("\n========== AUTOMATED TESTS ================");
        int passed = 0;
        int total = 0;

        // Test 1: Encoder mapping for all four one-hot sensor states.
        for (int level = 0; level <= 3; level++) {
            setLevelForTest(level);
            int encoded = encodeLevel();
            total++;
            boolean ok = encoded == level;
            if (ok) passed++;
            printTest("Encoder level " + level, ok,
                    "expected " + binary2(level) + ", got " + binary2(encoded));
        }

        // Test 2: Pump should be ON for levels 0,1,2 and OFF at level 3.
        for (int level = 0; level <= 3; level++) {
            setLevelForTest(level);
            boolean expected = level != 3;
            boolean actual = isPumpOn();
            total++;
            boolean ok = actual == expected;
            if (ok) passed++;
            printTest("Pump at level " + level, ok,
                    "expected " + (expected ? "ON" : "OFF")
                            + ", got " + (actual ? "ON" : "OFF"));
        }

        // Test 3: FULL held for three clock edges activates the alarm.
        setLevelForTest(3);
        storedLevel = 0;
        fullCounter = 0;
        clockTickForTest();
        total++;
        boolean firstOk = fullCounter == 1 && !isAlarmOn();
        if (firstOk) passed++;
        printTest("Alarm after full edge 1", firstOk, "counter=" + binary2(fullCounter));

        clockTickForTest();
        total++;
        boolean secondOk = fullCounter == 2 && !isAlarmOn();
        if (secondOk) passed++;
        printTest("Alarm after full edge 2", secondOk, "counter=" + binary2(fullCounter));

        clockTickForTest();
        total++;
        boolean thirdOk = fullCounter == 3 && isAlarmOn();
        if (thirdOk) passed++;
        printTest("Alarm after full edge 3", thirdOk, "counter=" + binary2(fullCounter));

        // Test 4: Leaving FULL resets the counter and alarm.
        setLevelForTest(2);
        clockTickForTest();
        total++;
        boolean resetCounterOk = fullCounter == 0 && !isAlarmOn();
        if (resetCounterOk) passed++;
        printTest("Counter resets below FULL", resetCounterOk,
                "counter=" + binary2(fullCounter) + ", alarm=" + (isAlarmOn() ? "ON" : "OFF"));

        // Test 5: Register captures current level on a clock edge.
        setLevelForTest(1);
        storedLevel = 0;
        clockTickForTest();
        total++;
        boolean registerOk = storedLevel == 1;
        if (registerOk) passed++;
        printTest("Register captures level", registerOk,
                "stored=" + binary2(storedLevel));

        System.out.println("-------------------------------------------");
        System.out.println("Test result: " + passed + "/" + total + " passed.");
        System.out.println("Automated tests use internal clock-edge simulation.");
        System.out.println("===========================================");
    }

    private void setLevelForTest(int level) {
        for (int i = 0; i < sensors.length; i++) {
            sensors[i] = (i == level);
        }
    }

    private void clockTickForTest() {
        int encodedLevel = encodeLevel();
        storedLevel = encodedLevel;
        if (!isFull(encodedLevel)) {
            fullCounter = 0;
        } else if (fullCounter < 3) {
            fullCounter++;
        }
    }

    private void printTest(String name, boolean passed, String detail) {
        System.out.println((passed ? "[PASS] " : "[FAIL] ") + name + " — " + detail);
    }

    private static String sevenSegment(int level) {
        // Segment order a b c d e f g; 1 means illuminated.
        switch (level & 0b11) {
            case 0: return "1111110"; // digit 0
            case 1: return "0110000"; // digit 1
            case 2: return "1101101"; // digit 2
            case 3: return "1111001"; // digit 3
            default: return "0000000";
        }
    }

    private static String levelName(int level) {
        switch (level) {
            case 0: return "EMPTY / LOW";
            case 1: return "25% / LOW";
            case 2: return "50% / MEDIUM";
            case 3: return "FULL";
            default: return "INVALID";
        }
    }

    private static String binary2(int value) {
        return "" + ((value >> 1) & 1) + (value & 1);
    }

    private static int bit(boolean value) {
        return value ? 1 : 0;
    }

    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = INPUT.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException ex) {
                System.out.println("Invalid input. Please enter a whole number.");
            }
        }
    }
}
