package edu.lehigh.cse280.backend;

import org.springframework.security.crypto.bcrypt.BCrypt;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.net.URI;
import java.net.URISyntaxException;

import java.util.ArrayList;

public class Database {
    /**
     * The connection to the database
     */
    private Connection mConnection;

    /**
     * Some prepared statements
     */

    private PreparedStatement uGmail;
    private PreparedStatement uCreateTable;
    private PreparedStatement uDropTable;
    private PreparedStatement uInsertOne;
    private PreparedStatement uUpdateOne;
    private PreparedStatement uSelectAll;
    private PreparedStatement uSelectOne;
    private PreparedStatement uDeleteOne;
    private PreparedStatement uSelectAllWithQuery;

    /*
    private PreparedStatement cCreateTable;
    private PreparedStatement cDropTable;
    private PreparedStatement cInsertOne;
    private PreparedStatement cSelectOne;
    private PreparedStatement cSelectAll;
    private PreparedStatement cUpdateOne;
    private PreparedStatement cDeleteOne;
    */

    /**
     * The Database constructor is private: we only create Database objects
     * through the getDatabase() method.
     */
    private Database() {

    }

    public void init() {
        try {
            uCreateTable.execute();
            // cCreateTable.execute();
        } catch(Exception e) {
            e.printStackTrace();
            //disconnect();
        }
    }

    /**
     * Connect to the Database
     * 
     * @param ip   The IP address of the database server
     * @param port The port on the database server to which connection requests
     *             should be sent
     * @param user The user ID to use when connecting
     * @param pass The password to use when connecting
     * @return A Database object, or null if we cannot connect properly
     */
    static Database getDatabase(String db_url) {
        Database db = new Database();;
        try {
            Class.forName("org.postgresql.Driver");
            URI dbUri = new URI(db_url);
            String username = dbUri.getUserInfo().split(":")[0];
            String password = dbUri.getUserInfo().split(":")[1];
            String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath();
            Connection conn = DriverManager.getConnection(dbUrl, username, password);
            if (conn == null) {
                System.err.println("Error: DriverManager.getConnection() returned a null object");
                return null;
            }
            db.mConnection = conn;
        } catch (SQLException e) {
            System.err.println("Error: DriverManager.getConnection() threw a SQLException");
            e.printStackTrace();
            return null;
        } catch (ClassNotFoundException cnfe) {
            System.out.println("Unable to find postgresql driver");
            return null;
        } catch (URISyntaxException s) {
            System.out.println("URI Syntax Error");
            return null;
        }
        

        // Create prepared statement
        try {
            db.uCreateTable = db.mConnection
                    .prepareStatement("CREATE TABLE tblUser (uid SERIAL PRIMARY KEY, username VARCHAR(50) "
                            + "NOT NULL, email VARCHAR(500) NOT NULL, gender INTEGER, tidiness INTEGER, "
                            + "noise INTEGER, sleep INTEGER, wake INTEGER, pet INTEGER, visitor INTEGER, hobby VARCHAR(500))");
            db.uDropTable = db.mConnection.prepareStatement("DROP TABLE tblUser");

            // insert only name and email into uTable, uid is auto generated by the database and the rest are null;
            db.uInsertOne = db.mConnection.prepareStatement("INSERT INTO tblUser VALUES (default, ?, ?, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)");
            db.uGmail = db.mConnection.prepareStatement("SELECT * from tblUser WHERE email = ?");
            db.uUpdateOne = db.mConnection
                    .prepareStatement("UPDATE tblUser SET username = ?, gender = ?, tidiness = ?, noise = ?, sleep = ?, wake = ?, pet = ?, "
                            + "visitor = ?, hobby = ? WHERE uid = ?");
            db.uSelectAll = db.mConnection.prepareStatement("SELECT * from tblUser");
            db.uSelectOne = db.mConnection.prepareStatement("SELECT * from tblUser WHERE uid = ?");
            db.uDeleteOne = db.mConnection.prepareStatement("DELETE FROM tblUser WHERE uid = ?");
            db.uSelectAllWithQuery = db.mConnection.prepareStatement("SELECT * from tblUser ?");


            /*
            //这部分应该都不需要
            db.cCreateTable = db.mConnection
                    .prepareStatement("CREATE TABLE tblCounter (cid SERIAL PRIMARY KEY, uid INTEGER "
                            + "NOT NULL, value INTEGER NOT NULL, "
                            + "FOREIGN KEY(uid) REFERENCES tblUser)");
            db.cDropTable = db.mConnection.prepareStatement("DROP TABLE tblCounter");

            // Standard CRUD operations
            db.cDeleteOne = db.mConnection.prepareStatement("DELETE FROM tblCounter WHERE cid = ?");
            db.cInsertOne = db.mConnection.prepareStatement("INSERT INTO tblCounter VALUES (default, ?, ?)");
            db.cSelectOne = db.mConnection.prepareStatement("SELECT * from tblCounter WHERE cid = ?");
            db.cSelectAll = db.mConnection.prepareStatement("SELECT * FROM tblCounter WHERE uid = ?");
            db.cUpdateOne = db.mConnection.prepareStatement("UPDATE tblCounter SET value = ? WHERE cid = ?");
            */
        } catch (SQLException e) {
            System.err.println("Error creating prepared statement");
            e.printStackTrace();
            db.disconnect();
            return null;
        }
        return db;
    }

    /**
     * Close the current connection to the Database, if one exists.
     * 
     * @return True if the connection was cleanly closed, false otherwise.
     */
    boolean disconnect() {
        if (mConnection == null) {
            System.err.println("Unable to close connection: Connection was null");
            return false;
        }
        try {
            // cDropTable.execute();
            uDropTable.execute();
            mConnection.close();
        } catch (SQLException e) {
            System.err.println("Error: Connection.close() threw a SQLException");
            e.printStackTrace();
            mConnection = null;
            return false;
        }
        mConnection = null;
        return true;
    }

    /**
     * Check if a user with the given Gmail exists
     * 
     * @param id
     * @return
     */
    public DataRowUserProfile matchUsr(String email) {
        DataRowUserProfile res = null;
        try {
            uGmail.setString(1, email);
            ResultSet rs = uGmail.executeQuery();
            if (rs.next())
                res = new DataRowUserProfile(rs.getInt("uid"), rs.getString("username"), rs.getString("email"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
    }

    /**
     * Insert a row into the database
     * 
     * @param email The subject for this new row
     * 
     * @return The number of rows that were inserted
     */
    public int insertRowToUser(String email) {
        int count = 0;
        try {
            // Use email as default user name
            uInsertOne.setString(1, email.split("@")[0]);
            uInsertOne.setString(2, email);
            count += uInsertOne.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }

    public ArrayList<DataRowUserProfile> readAll() {
        //ArrayList<DataRowLite> res = new ArrayList<DataRowLite>();
        ArrayList<DataRowUserProfile> res = new ArrayList<DataRowUserProfile>();
        try {
            ResultSet rs = uSelectAll.executeQuery();
            while (rs.next()) {
                //res.add(new DataRowLite(new DataRow(rs.getInt("id"), rs.getString("subject"), rs.getString("message"), rs.getInt("likes"))));
                //phase 1 used
                //res.add(new DataRow(rs.getInt("id"), rs.getString("subject"), rs.getString("message"), rs.getInt("likes"), rs.getDate("date")));
                //phase 2 with more parameters
                //readAll只需要显示uID uName和uEmail，所以用DataRowUserProfile足够了
                res.add(new DataRowUserProfile(rs.getInt("uid"), rs.getString("username"), rs.getString("email")));
//                res.add(new DataRow(rs.getInt("mid"), rs.getInt("uid"), rs.getString("username"), rs.getString("subject"), rs.getString("message"), rs.getInt("likes"), rs.getInt("dislikes"), rs.getDate("date")));
            }
            rs.close();
            return res;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public ArrayList<DataRowUserProfile> readAll(String where) {
        ArrayList<DataRowUserProfile> res = new ArrayList<DataRowUserProfile>();
        try {
            uSelectAllWithQuery.setString(1, where);
            ResultSet rs = uSelectAllWithQuery.executeQuery();
            while (rs.next()) {
                res.add(new DataRowUserProfile(rs.getInt("uid"), rs.getString("username"), rs.getString("email")));
            }
            rs.close();
            return res;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    //req.uid, req.uName, req.uGender, req.uTidiness, req.uNoise, req.uSleepTime, req.uWakeTime, req.uPet, req.uVisitor, req.uHobby
    public int updateOne(int uid, String username, int gender, int tidiness, int noise, int sleep, int wake, int pet, int visitor, String hobby) {
        try {
            uUpdateOne.setString(1, username);
            uUpdateOne.setInt(2, gender);
            uUpdateOne.setInt(3, tidiness);
            uUpdateOne.setInt(4, noise);
            uUpdateOne.setInt(5, sleep);
            uUpdateOne.setInt(6, wake);
            uUpdateOne.setInt(7, pet);
            uUpdateOne.setInt(8, visitor);
            uUpdateOne.setString(9, hobby);

            uUpdateOne.setInt(10, uid);
            uUpdateOne.execute();
            return uid;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public DataRow readOne(int id) {
        DataRow res = null;
        try {
            uSelectOne.setInt(1, id);
            ResultSet rs = uSelectOne.executeQuery();
            if (rs.next()) {
                res = new DataRow(rs.getInt("uid"), rs.getString("username"), rs.getString("email"), rs.getInt("gender"), rs.getInt("tidiness"), rs.getInt("noise"), rs.getInt("sleep"), rs.getInt("wake"), rs.getInt("pet"), rs.getInt("visitor"), rs.getString("hobby"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
    }

    public boolean deleteOne(int id) {
        try {
            uDeleteOne.setInt(1, id);
            uDeleteOne.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
	}













    /*
    //这部分应该也不需要，但先留着了，以防万一
	public int createCounter(int uid, int value) {
		int count = 0;
        try {
            cInsertOne.setInt(1, uid);
            cInsertOne.setInt(2, value);
            count += cInsertOne.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
	}

	public DataRow cIncrement(int uid, int cid) {
		try {
            cSelectOne.setInt(1, cid);
            ResultSet rs = cSelectOne.executeQuery();
            if (rs.next()) {
                cUpdateOne.setInt(1, rs.getInt("value") + 1);
                cUpdateOne.setInt(2, cid);
                cUpdateOne.execute();
            }
            return selectCounter(cid);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
	}

	public DataRow cDecrement(int uid, int cid) {
		try {
            cSelectOne.setInt(1, cid);
            ResultSet rs = cSelectOne.executeQuery();
            if (rs.next()) {
                cUpdateOne.setInt(1, rs.getInt("value") - 1);
                cUpdateOne.setInt(2, cid);
                cUpdateOne.execute();
            }
            return selectCounter(cid);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
	}

	public DataRow selectCounter(int cid) {
		DataRow res = null;
        try {
            cSelectOne.setInt(1, cid);
            ResultSet rs = cSelectOne.executeQuery();
            if (rs.next()) {
                res = new DataRow(rs.getInt("cid"), rs.getInt("uid"), rs.getInt("value"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
    }
    
    public ArrayList<DataRow> selectAllCounter(int uid) {
		ArrayList<DataRow> res = new ArrayList<DataRow>();
        try {
            cInsertOne.setInt(1, uid);
            ResultSet rs = cSelectAll.executeQuery();
            while (rs.next()) {
                res.add(new DataRow(rs.getInt("cid"), rs.getInt("uid"), rs.getInt("value")));
            }
            rs.close();
            return res;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
	}

	public boolean deleteCounter(int idx) {
        try {
            cDeleteOne.setInt(1, idx);
            cDeleteOne.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    */


}