package io.proj3ct.SpringLawandaFarmBot.model;

import java.sql.*;
import java.util.Objects;

public class BotDBConnection {
    private static Connection getConnection(){
        try {
            Class.forName("org.postgresql.Driver");
            return DriverManager.getConnection(
                    "jdbc:postgresql://localhost:5432/LawandaFarmDB",
                    "postgres", "Sany38622,");
        } catch (ClassNotFoundException | SQLException throwables) {
            throwables.printStackTrace();
        }
        return null;
    }

    public static ResultSet SELECT(String query){
        ResultSet rs = null;
        try {
            Connection connection = getConnection();
            Statement statement = Objects.requireNonNull(connection).createStatement();
            rs = statement.executeQuery(query);

//            statement.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return rs;
    }

    public static void POST(String query){
        try {
            Connection connection = getConnection();
            Statement statement = Objects.requireNonNull(connection).createStatement();
            statement.executeUpdate(query);

            statement.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }
}
