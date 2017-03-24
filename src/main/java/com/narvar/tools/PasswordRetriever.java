/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.narvar.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author Sam Sharaf at Narvar, Inc.
 */
public class PasswordRetriever {

    public static String encyptionPasswordFromGlobalProperties(Connection pgConnection) {
        try {
            PreparedStatement st = pgConnection.prepareStatement("SELECT key from global_properties LIMIT 2");
            ResultSet rs = st.executeQuery();
            rs.next();
            String password = rs.getString(1);
//        System.out.println("Password= " + password);
            return password;
        } catch (SQLException ex) {
            System.out.println("Encyption password retrieval from global_properties Failed! Check output console for the StackTrace");
            ex.printStackTrace();
        }
        return null;
    }
}
