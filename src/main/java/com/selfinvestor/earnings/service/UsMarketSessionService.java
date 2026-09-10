package com.selfinvestor.earnings.service;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;

/**
 * 美国股票常规交易时段判断服务。
 */
@Service
public class UsMarketSessionService {
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private static final LocalTime OPEN = LocalTime.of(9, 30);
    private static final LocalTime CLOSE = LocalTime.of(16, 0);
    private Clock clock = Clock.systemUTC();

    /**
     * 判断当前是否处于美股常规交易时段。
     *
     * @return 是否开市
     */
    public boolean isRegularSessionNow() {
        return isRegularSession(clock.instant());
    }

    /**
     * 使用纽约时区判断指定时刻是否处于常规交易时段。
     *
     * @param instant UTC 时刻
     * @return 是否开市
     */
    public boolean isRegularSession(Instant instant) {
        ZonedDateTime newYorkTime = instant.atZone(NEW_YORK);
        LocalDate date = newYorkTime.toLocalDate();
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY || isHoliday(date)) {
            return false;
        }
        LocalTime time = newYorkTime.toLocalTime();
        LocalTime close = isEarlyClose(date) ? LocalTime.of(13, 0) : CLOSE;
        return !time.isBefore(OPEN) && time.isBefore(close);
    }

    /**
     * 判断是否为主要美国市场休市日。
     *
     * @param date 纽约日期
     * @return 是否休市
     */
    private boolean isHoliday(LocalDate date) {
        int year = date.getYear();
        return date.equals(observed(LocalDate.of(year, Month.JANUARY, 1)))
                || date.equals(nthWeekday(year, Month.JANUARY, DayOfWeek.MONDAY, 3))
                || date.equals(nthWeekday(year, Month.FEBRUARY, DayOfWeek.MONDAY, 3))
                || date.equals(easterSunday(year).minusDays(2))
                || date.equals(LocalDate.of(year, Month.MAY, 31).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
                || date.equals(observed(LocalDate.of(year, Month.JUNE, 19)))
                || date.equals(observed(LocalDate.of(year, Month.JULY, 4)))
                || date.equals(nthWeekday(year, Month.SEPTEMBER, DayOfWeek.MONDAY, 1))
                || date.equals(nthWeekday(year, Month.NOVEMBER, DayOfWeek.THURSDAY, 4))
                || date.equals(observed(LocalDate.of(year, Month.DECEMBER, 25)));
    }

    /**
     * 判断常见提前收市日。
     *
     * @param date 纽约日期
     * @return 是否下午一点收市
     */
    private boolean isEarlyClose(LocalDate date) {
        int year = date.getYear();
        LocalDate thanksgiving = nthWeekday(year, Month.NOVEMBER, DayOfWeek.THURSDAY, 4);
        return date.equals(thanksgiving.plusDays(1))
                || (date.getMonth() == Month.DECEMBER && date.getDayOfMonth() == 24
                && date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY)
                || (date.getMonth() == Month.JULY && date.getDayOfMonth() == 3
                && date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY);
    }

    /**
     * 计算固定日期节日的工作日补休日期。
     *
     * @param date 固定日期
     * @return 观察日期
     */
    private LocalDate observed(LocalDate date) {
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY) {
            return date.minusDays(1);
        }
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return date.plusDays(1);
        }
        return date;
    }

    /**
     * 计算某月第几个指定星期。
     *
     * @param year 年份
     * @param month 月份
     * @param dayOfWeek 星期
     * @param ordinal 序号
     * @return 日期
     */
    private LocalDate nthWeekday(int year, Month month, DayOfWeek dayOfWeek, int ordinal) {
        return LocalDate.of(year, month, 1).with(TemporalAdjusters.dayOfWeekInMonth(ordinal, dayOfWeek));
    }

    /**
     * 使用 Meeus/Jones/Butcher 算法计算复活节。
     *
     * @param year 年份
     * @return 复活节日期
     */
    private LocalDate easterSunday(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = (h + l - 7 * m + 114) % 31 + 1;
        return LocalDate.of(year, month, day);
    }
}
