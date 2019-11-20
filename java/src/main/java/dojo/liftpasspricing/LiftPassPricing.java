package dojo.liftpasspricing;

import java.sql.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.function.Supplier;

public class LiftPassPricing {
    static String getPrices(PricingParameters pricingParameters, MysqlPricingRepository mysqlPricingRepository) throws SQLException, ParseException {
        Connection connection = MysqlPricingRepository.getConnection();
        ResultSet result = mysqlPricingRepository.getGetPricesFunc().apply(connection, pricingParameters.getType());
        result.next();

        int reduction;
        boolean isHoliday = false;

        if (pricingParameters.getAge() != null && pricingParameters.getAge() < 6) {
            return "{ \"cost\": 0}";
        } else {
            reduction = 0;

            if (!pricingParameters.getType().equals("night")) {
                return getNightPricing(pricingParameters.getAge(), pricingParameters.getDate(), result, reduction, isHoliday, mysqlPricingRepository.getGetHolidaysFunc());
            } else {
                return getDayPricing(pricingParameters.getAge(), result);
            }
        }
    }

    private static String getDayPricing(Integer age, ResultSet result) throws SQLException {
        if (age != null && age >= 6) {
            if (age > 64) {
                return "{ \"cost\": " + (int) Math.ceil(result.getInt("cost") * .4) + "}";
            } else {
                return "{ \"cost\": " + result.getInt("cost") + "}";
            }
        } else {
            return "{ \"cost\": 0}";
        }
    }

    private static String getNightPricing(Integer age, String date, ResultSet result, int reduction, boolean isHoliday, Supplier<ResultSet> getHolidaysFunc) throws SQLException, ParseException {
        DateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd");

        ResultSet holidays = getHolidaysFunc.get();

        while (holidays.next()) {
            Date holiday = holidays.getDate("holiday");
            if (date != null) {
                Date d = isoFormat.parse(date);
                if (d.getYear() == holiday.getYear() && //
                        d.getMonth() == holiday.getMonth() && //
                        d.getDate() == holiday.getDate()) {
                    isHoliday = true;
                }
            }
        }

        if (date != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(isoFormat.parse(date));
            if (!isHoliday && calendar.get(Calendar.DAY_OF_WEEK) == 2) {
                reduction = 35;
            }
        }

        // TODO apply reduction for others
        if (age != null && age < 15) {
            return "{ \"cost\": " + (int) Math.ceil(result.getInt("cost") * .7) + "}";
        } else {
            if (age == null) {
                double cost = result.getInt("cost") * (1 - reduction / 100.0);
                return "{ \"cost\": " + (int) Math.ceil(cost) + "}";
            } else {
                if (age > 64) {
                    double cost = result.getInt("cost") * .75 * (1 - reduction / 100.0);
                    return "{ \"cost\": " + (int) Math.ceil(cost) + "}";
                } else {
                    double cost = result.getInt("cost") * (1 - reduction / 100.0);
                    return "{ \"cost\": " + (int) Math.ceil(cost) + "}";
                }
            }
        }
    }

}
