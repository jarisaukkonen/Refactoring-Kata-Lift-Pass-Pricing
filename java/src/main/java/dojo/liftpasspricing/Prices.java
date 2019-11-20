package dojo.liftpasspricing;

import static spark.Spark.after;
import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.put;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Prices {

    public static Connection createApp() throws SQLException {

        final Connection connection = MysqlPricingRepository.getConnection();

        port(4567);

        put("/prices", (req, res) -> {
            int liftPassCost = Integer.parseInt(req.queryParams("cost"));
            String liftPassType = req.queryParams("type");

            try (PreparedStatement stmt = connection.prepareStatement( //
                    "INSERT INTO base_price (type, cost) VALUES (?, ?) " + //
                    "ON DUPLICATE KEY UPDATE cost = ?")) {
                stmt.setString(1, liftPassType);
                stmt.setInt(2, liftPassCost);
                stmt.setInt(3, liftPassCost);
                stmt.execute();
            }

            return "";
        });

        get("/prices", (req, res) -> {
            final Integer age = req.queryParams("age") != null ? Integer.valueOf(req.queryParams("age")) : null;

            return LiftPassPricing.getPrices(new PricingParameters(age, req.queryParams("type"), req.queryParams("date")), new MysqlPricingRepository((c, t) -> MysqlPricingRepository.getPrices(t, c), (c) -> MysqlPricingRepository.getHolidays(c)));
        });

        after((req, res) -> {
            res.type("application/json");
        });

        return connection;
    }

}
