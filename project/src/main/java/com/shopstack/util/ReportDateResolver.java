package com.shopstack.util;

import com.shopstack.dto.ReportFilter;
import com.shopstack.exception.ReportException;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Resolves a {@link ReportFilter} into an absolute {@code [start, end]} window and
 * a SQL date-format string used by grouped revenue queries.
 *
 * Presets: when both fromDate and toDate are null, defaults to "monthly" for the
 * current month. When only a preset is requested via the status-less filter, the
 * controller passes an explicit preset. Otherwise the explicit from/to wins.
 */
public final class ReportDateResolver {

    public static final String FMT_DAILY = "YYYY-MM-DD";
    public static final String FMT_WEEKLY = "IYYY-IW";
    public static final String FMT_MONTHLY = "YYYY-MM";
    public static final String FMT_YEARLY = "YYYY";

    private ReportDateResolver() {
    }

    public static Resolved resolve(ReportFilter filter, Preset preset) {
        if (filter == null) {
            filter = new ReportFilter();
        }
        if (filter.getFromDate() != null && filter.getToDate() != null) {
            if (filter.getToDate().isBefore(filter.getFromDate())) {
                throw new ReportException("toDate must not be before fromDate");
            }
            LocalDateTime start = filter.getFromDate().atStartOfDay();
            LocalDateTime end = filter.getToDate().plusDays(1).atStartOfDay();
            String fmt = guessFormat(filter.getFromDate(), filter.getToDate(), preset);
            return new Resolved(start, end, fmt);
        }
        if (preset == null) {
            preset = Preset.MONTHLY;
        }
        LocalDate today = LocalDate.now();
        return switch (preset) {
            case DAILY -> {
                LocalDate day = filter.getFromDate() != null ? filter.getFromDate() : today;
                yield new Resolved(day.atStartOfDay(), day.plusDays(1).atStartOfDay(), FMT_DAILY);
            }
            case WEEKLY -> {
                LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1L);
                yield new Resolved(weekStart.atStartOfDay(), weekStart.plusDays(7).atStartOfDay(), FMT_WEEKLY);
            }
            case MONTHLY -> {
                LocalDate monthStart = today.withDayOfMonth(1);
                yield new Resolved(monthStart.atStartOfDay(),
                        monthStart.plusMonths(1).atStartOfDay(), FMT_MONTHLY);
            }
            case YEARLY -> {
                LocalDate yearStart = today.withDayOfYear(1);
                yield new Resolved(yearStart.atStartOfDay(),
                        yearStart.plusYears(1).atStartOfDay(), FMT_YEARLY);
            }
            case CUSTOM -> {
                LocalDate from = filter.getFromDate() != null ? filter.getFromDate() : today.minusMonths(1);
                LocalDate to = filter.getToDate() != null ? filter.getToDate() : today;
                if (to.isBefore(from)) {
                    throw new ReportException("toDate must not be before fromDate");
                }
                yield new Resolved(from.atStartOfDay(), to.plusDays(1).atStartOfDay(), FMT_DAILY);
            }
        };
    }

    private static String guessFormat(LocalDate from, LocalDate to, Preset preset) {
        long days = java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1;
        if (preset != null) {
            return switch (preset) {
                case DAILY -> FMT_DAILY;
                case WEEKLY -> FMT_WEEKLY;
                case MONTHLY -> FMT_MONTHLY;
                case YEARLY -> FMT_YEARLY;
                case CUSTOM -> days <= 31 ? FMT_DAILY : FMT_MONTHLY;
            };
        }
        if (days <= 1) return FMT_DAILY;
        if (days <= 7) return FMT_DAILY;
        if (days <= 366) return FMT_MONTHLY;
        return FMT_YEARLY;
    }

    public enum Preset {
        DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM
    }

    public record Resolved(LocalDateTime start, LocalDateTime end, String format) {
    }
}
