package ru.edu.pr03;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

/**
 * Единственная точка входа.
 * Без аргументов: unit-проверки + data-driven; "csv": только data-driven; "interactive": ввод с консоли.
 */
public class SelfCheck {

    private static final String CSV_PATH = "data/sample.csv";
    private static final ApprovalRouter ROUTER = new ApprovalRouter();
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        String mode = args.length > 0 ? args[0] : "all";
        if (mode.equals("interactive")) {
            interactive();
            return;
        }
        if (!mode.equals("csv")) {
            unitChecks();
        }
        dataDriven();
        System.out.println("Итого: PASS " + passed + ", FAIL " + failed);
    }

    // ---------- Режим 1: unit-проверки ----------

    private static void unitChecks() {
        // route: пересечения правил (приоритеты)
        expectRoute("route personal > urgent", 10_000, true, RiskLevel.LOW, true, ApprovalRoute.SECURITY_REVIEW);
        expectRoute("route HIGH > urgent", 10_000, true, RiskLevel.HIGH, false, ApprovalRoute.DIRECTOR_REVIEW);
        expectRoute("route personal > HIGH", 10_000, false, RiskLevel.HIGH, true, ApprovalRoute.SECURITY_REVIEW);
        expectRoute("route сумма > urgent", 600_000, true, RiskLevel.LOW, false, ApprovalRoute.FINANCE_REVIEW);
        expectRoute("route HIGH > сумма", 600_000, false, RiskLevel.HIGH, false, ApprovalRoute.DIRECTOR_REVIEW);
        expectRoute("route personal + urgent + HIGH", 10_000, true, RiskLevel.HIGH, true, ApprovalRoute.SECURITY_REVIEW);
        // route: базовые маршруты
        expectRoute("route FAST_TRACK", 10_000, true, RiskLevel.LOW, false, ApprovalRoute.FAST_TRACK);
        expectRoute("route FINANCE_REVIEW (граница)", 500_000, false, RiskLevel.LOW, false, ApprovalRoute.FINANCE_REVIEW);
        expectRoute("route STANDARD", 10_000, false, RiskLevel.LOW, false, ApprovalRoute.STANDARD);
        expectRoute("route SECURITY_REVIEW", 10_000, false, RiskLevel.LOW, true, ApprovalRoute.SECURITY_REVIEW);
        expectRoute("route amount = 0", 0, false, RiskLevel.LOW, false, ApprovalRoute.STANDARD);
        // route: границы суммы 499999 / 500000 / 500001
        expectRoute("route 499999 срочная", 499_999, true, RiskLevel.LOW, false, ApprovalRoute.FAST_TRACK);
        expectRoute("route 500000 срочная", 500_000, true, RiskLevel.LOW, false, ApprovalRoute.FINANCE_REVIEW);
        expectRoute("route 500001", 500_001, false, RiskLevel.LOW, false, ApprovalRoute.FINANCE_REVIEW);
        // route: вариант 2, часть А (MEDIUM как HIGH)
        expectRoute("A: MEDIUM -> DIRECTOR_REVIEW", 10_000, false, RiskLevel.MEDIUM, false, ApprovalRoute.DIRECTOR_REVIEW);
        expectRoute("A: personal + MEDIUM -> SECURITY", 10_000, false, RiskLevel.MEDIUM, true, ApprovalRoute.SECURITY_REVIEW);
        expectRoute("A: MEDIUM > urgent", 10_000, true, RiskLevel.MEDIUM, false, ApprovalRoute.DIRECTOR_REVIEW);
        expectRoute("A: MEDIUM > сумма", 600_000, false, RiskLevel.MEDIUM, false, ApprovalRoute.DIRECTOR_REVIEW);
        expectRoute("A: LOW + крупная сумма -> FINANCE", 600_000, false, RiskLevel.LOW, false, ApprovalRoute.FINANCE_REVIEW);
        // route: защитные условия
        expectRouteError("route amount < 0", -1, RiskLevel.LOW, "amount must be >= 0, got -1");
        expectRouteError("route risk == null", 10_000, null, "risk is null");
        // addRiskWeight: вариант 2, часть Б
        expectWeight("B: addRiskWeight LOW", RiskLevel.LOW, 0);
        expectWeight("B: addRiskWeight MEDIUM", RiskLevel.MEDIUM, 50);
        expectWeight("B: addRiskWeight HIGH", RiskLevel.HIGH, 100);
        expectWeightError("B: addRiskWeight null");
        System.out.println("Unit-проверок выполнено: " + (passed + failed));
    }

    private static void expectRoute(String name, long amount, boolean urgent, RiskLevel risk,
                                    boolean personal, ApprovalRoute expected) {
        try {
            ApprovalRoute actual = ROUTER.route(amount, urgent, risk, personal);
            report(name, actual == expected, "ожидалось " + expected + ", получено " + actual);
        } catch (IllegalArgumentException e) {
            report(name, false, "исключение: " + e.getMessage());
        }
    }

    private static void expectRouteError(String name, long amount, RiskLevel risk, String fragment) {
        try {
            ROUTER.route(amount, true, risk, false);
            report(name, false, "исключение не выброшено");
        } catch (IllegalArgumentException e) {
            report(name, e.getMessage().contains(fragment), e.getMessage());
        }
    }

    private static void expectWeight(String name, RiskLevel risk, int expected) {
        int actual = ROUTER.addRiskWeight(risk);
        report(name, actual == expected, "ожидалось " + expected + ", получено " + actual);
    }

    private static void expectWeightError(String name) {
        try {
            ROUTER.addRiskWeight(null);
            report(name, false, "исключение не выброшено");
        } catch (IllegalArgumentException e) {
            report(name, e.getMessage().contains("risk is null"), e.getMessage());
        }
    }

    private static void report(String name, boolean ok, String detail) {
        if (ok) {
            passed++;
            System.out.println("PASS " + name);
        } else {
            failed++;
            System.out.println("FAIL " + name + ": " + detail);
        }
    }

    // ---------- Разбор строки: 4 поля amount;urgent;risk;personal ----------

    /** Разбирает 4 поля и вызывает route; формат ошибок разбора – «Ошибка: …». */
    private static ApprovalRoute routeFor(String amount, String urgent, String risk, String personal) {
        long amountValue;
        try {
            amountValue = Long.parseLong(amount);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Ошибка: amount не является числом: \"" + amount + "\"");
        }
        RiskLevel riskValue = null;
        if (!risk.equals("null")) {
            try {
                riskValue = RiskLevel.valueOf(risk);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Ошибка: risk должен быть LOW, MEDIUM или HIGH: \"" + risk + "\"");
            }
        }
        return ROUTER.route(amountValue, parseBoolean(urgent), riskValue, parseBoolean(personal));
    }

    private static boolean parseBoolean(String value) {
        if (!value.equals("true") && !value.equals("false")) {
            throw new IllegalArgumentException("Ошибка: ожидалось true или false: \"" + value + "\"");
        }
        return value.equals("true");
    }

    // ---------- Режим 2: data-driven (data/sample.csv) ----------

    private static void dataDriven() {
        List<String> lines;
        try {
            lines = Files.readAllLines(Paths.get(CSV_PATH));
        } catch (IOException e) {
            failed++;
            System.out.println("FAIL data: не удалось прочитать " + CSV_PATH);
            return;
        }
        int n = 0;
        int ok = 0;
        boolean headerSkipped = false;
        for (String row : lines) {
            if (row.isEmpty() || row.startsWith("#")) {
                continue;
            }
            if (!headerSkipped) {
                headerSkipped = true;
                continue;
            }
            n++;
            String problem = checkRow(row);
            if (problem == null) {
                ok++;
                passed++;
                System.out.println("PASS data/" + n);
            } else {
                failed++;
                System.out.println("FAIL data/" + n + ": " + problem);
            }
        }
        System.out.println("Data-driven: " + ok + "/" + n + " passed");
    }

    /** Возвращает null, если строка файла выполнена, иначе причину ошибки. */
    private static String checkRow(String row) {
        String[] f = row.split(";", -1);
        if (f.length != 5) {
            return "ожидалось 5 колонок, получено " + f.length;
        }
        try {
            ApprovalRoute actual = routeFor(f[0], f[1], f[2], f[3]);
            return actual.name().equals(f[4]) ? null : "ожидалось " + f[4] + ", получено " + actual;
        } catch (IllegalArgumentException e) {
            return f[4].equals("ERROR") ? null : "исключение: " + e.getMessage();
        }
    }

    // ---------- Режим 3: интерактивный ----------

    private static void interactive() {
        System.out.println("Введите заявку amount;urgent;risk;personal (например 10000;true;LOW;false), exit или пустая строка – выход:");
        Scanner in = new Scanner(System.in, "UTF-8");
        while (in.hasNextLine()) {
            String line = in.nextLine();
            if (line.isEmpty() || line.equals("exit")) {
                break;
            }
            String[] f = line.split(";", -1);
            try {
                if (f.length != 4) {
                    throw new IllegalArgumentException("Ошибка: ожидалось 4 значения, получено " + f.length);
                }
                System.out.println("Маршрут: " + routeFor(f[0], f[1], f[2], f[3]));
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
