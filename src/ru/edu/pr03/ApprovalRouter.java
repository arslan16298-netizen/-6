package ru.edu.pr03;

/** Выбор маршрута согласования служебной заявки по правилам R1–R5 (вариант 2). */
public class ApprovalRouter {

    public static final long FINANCE_LIMIT = 500_000L;

    /** Маршрут заявки: правила проверяются строго в порядке приоритета R1 → R5. */
    public ApprovalRoute route(long amount, boolean urgent, RiskLevel risk, boolean containsPersonalData) {
        if (amount < 0) {
            throw new IllegalArgumentException("Ошибка: amount must be >= 0, got " + amount);
        }
        requireRisk(risk);
        if (requiresSecurityReview(containsPersonalData)) {
            return ApprovalRoute.SECURITY_REVIEW;
        }
        if (requiresDirectorReview(risk)) {
            return ApprovalRoute.DIRECTOR_REVIEW;
        }
        if (requiresFinanceReview(amount)) {
            return ApprovalRoute.FINANCE_REVIEW;
        }
        if (requiresFastTrack(amount, urgent)) {
            return ApprovalRoute.FAST_TRACK;
        }
        return ApprovalRoute.STANDARD;
    }

    /** Вариант 2, часть Б: вес риска LOW = 0, MEDIUM = 50, HIGH = 100. */
    public int addRiskWeight(RiskLevel risk) {
        requireRisk(risk);
        return switch (risk) {
            case LOW -> 0;
            case MEDIUM -> 50;
            case HIGH -> 100;
        };
    }

    private void requireRisk(RiskLevel risk) {
        if (risk == null) {
            throw new IllegalArgumentException("Ошибка: risk is null");
        }
    }

    private boolean requiresSecurityReview(boolean containsPersonalData) {
        return containsPersonalData;
    }

    /** Вариант 2, часть А: R2 срабатывает и для HIGH, и для MEDIUM. */
    private boolean requiresDirectorReview(RiskLevel risk) {
        return risk == RiskLevel.HIGH || risk == RiskLevel.MEDIUM;
    }

    private boolean requiresFinanceReview(long amount) {
        return amount >= FINANCE_LIMIT;
    }

    private boolean requiresFastTrack(long amount, boolean urgent) {
        return urgent && amount < FINANCE_LIMIT;
    }
}
