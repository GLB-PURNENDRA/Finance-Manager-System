package com.financemanager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DirectDbDump {
    public static void main(String[] args) throws Exception {
        Class.forName("org.h2.Driver");
        try (Connection conn = DriverManager.getConnection("jdbc:h2:file:./data/financedb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE", "sa", "")) {
            Statement stmt = conn.createStatement();
            
            System.out.println("--- USERS ---");
            ResultSet rsUsers = stmt.executeQuery("SELECT id, email, display_name, role FROM users");
            while (rsUsers.next()) {
                System.out.println("User: id=" + rsUsers.getLong("id") + ", email=" + rsUsers.getString("email") + ", name=" + rsUsers.getString("display_name") + ", role=" + rsUsers.getString("role"));
            }

            System.out.println("--- FAMILY LINKS ---");
            ResultSet rsLinks = stmt.executeQuery("SELECT id, parent_id, student_id, status, approved_at FROM family_links");
            while (rsLinks.next()) {
                System.out.println("Link: id=" + rsLinks.getLong("id") + ", parent=" + rsLinks.getLong("parent_id") + ", student=" + rsLinks.getLong("student_id") + ", status=" + rsLinks.getString("status") + ", approved=" + rsLinks.getString("approved_at"));
            }

            System.out.println("--- TRANSACTIONS ---");
            ResultSet rsTx = stmt.executeQuery("SELECT id, user_id, type, amount, category, description, transaction_date, source FROM transactions");
            while (rsTx.next()) {
                System.out.println("Tx: id=" + rsTx.getLong("id") + ", userId=" + rsTx.getLong("user_id") + ", type=" + rsTx.getString("type") + ", amount=" + rsTx.getBigDecimal("amount") + ", date=" + rsTx.getDate("transaction_date") + ", desc=" + rsTx.getString("description") + ", source=" + rsTx.getString("source"));
            }
        }
    }
}
