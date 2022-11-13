import org.json.*;
import java.sql.*;
import java.util.*;
public class Database {
    public static void SetupDatabase(){
        try {
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/", "root", "231850793");
//            Connection con = DriverManager.getConnection("jdbc:mysql://mysql:3306/?allowPublicKeyRetrieval=true&useSSL=false", "root", "231850793");
            if (con != null){
                String query = "show databases like 'cse312'";
                PreparedStatement PStatement = con.prepareStatement(query);
                ResultSet set = PStatement.executeQuery();
                if (!set.next()){
                    System.out.println("Database doesn't exists, Creating Database.....");

                    query = "create database cse312";
                    Statement s = con.createStatement();
                    s.execute(query);
                    query = "use cse312";
                    s.execute(query);
                    query = "create table userinform(id int auto_increment, email varchar(255), username varchar(255), PRIMARY KEY (id))";
                    s.execute(query);
                    query = "create table inputdata(Comment mediumtext, ImageIDX int)";
                    s.execute(query);
                    query = "create table xsrftokens(XSRFToken tinytext)";
                    s.execute(query);
                    query = "create table chathistory(Message longtext)";
                    s.execute(query);
                }
                else {
                    System.out.println("Database: \"" + set.getString(1) + "\" exists");
                }
                con.close();
            }
            else{
                System.out.println("Can't access MYSQL");
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
    public static Connection BuildConnectionToDatabase (){
        try{
            return DriverManager.getConnection("jdbc:mysql://localhost:3306/cse312", "root", "231850793");
//            return DriverManager.getConnection("jdbc:mysql://mysql:3306/cse312?allowPublicKeyRetrieval=true&useSSL=false", "root", "231850793");
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public static String AddUser(Map<String, String> UserInform) throws SQLException {
        Connection con = BuildConnectionToDatabase();
        Map<String, String> user = new HashMap<>();
        if (con != null){
            String query = " insert into userinform (email, username)" + " values (?, ?)";
            PreparedStatement PStatement = con.prepareStatement(query);
            PStatement.setString(2, UserInform.get("username"));
            PStatement.setString(1, UserInform.get("email"));
            PStatement.execute();
            String GetLast = "SELECT * FROM userinform ORDER BY ID DESC LIMIT 1";
            Statement s = con.createStatement();
            ResultSet set = s.executeQuery(GetLast);
            while (set.next()){
                user.put("id", String.valueOf(set.getInt("id")));
                user.put("email",set.getString("email"));
                user.put("username",set.getString("username"));
            }
            con.close();
        }
        else {
            System.out.println("Can't access Database");
        }
        return new JSONObject(user).toString();
    }
    public static String GetTable() throws SQLException{
        List<Map<String,String>> table = new ArrayList<>();
        Connection con = BuildConnectionToDatabase();
        if (con != null){
            String query = "SELECT * FROM userinform";
            Statement s = con.createStatement();
            ResultSet set = s.executeQuery(query);
            while (set.next()){
                Map<String, String> UserInform = new HashMap<>();
                UserInform.put("id", String.valueOf(set.getInt("id")));
                UserInform.put("email",set.getString("email"));
                UserInform.put("username",set.getString("username"));
                table.add(UserInform);
            }
            con.close();
        }
        else {
            System.out.println("Can't access Database");
        }
        JSONArray JArray = new JSONArray();
        for (Map<String, String> userinform : table) {
            JArray.put(new JSONObject(userinform));
        }
        return JArray.toString();
    }
    public static Map<String,String> FindUser(int id) throws SQLException {
        Map<String, String> UserInform = new HashMap<>();
        Connection con = BuildConnectionToDatabase();
        if (con != null){
            String query = "SELECT * FROM userinform WHERE id = ?";
            PreparedStatement PStatement = con.prepareStatement(query);
            PStatement.setInt(1,id);
            ResultSet set = PStatement.executeQuery();
            while (set.next()){
                UserInform.put("id", String.valueOf(set.getInt("id")));
                UserInform.put("email",set.getString("email"));
                UserInform.put("username",set.getString("username"));
            }
            PStatement.close();
            con.close();
        }
        else {
            System.out.println("Can't access Database");
        }
        return UserInform;
    }
    public static String UpdateUser(int id, Map<String, String> userInform) throws SQLException {
        Connection con = BuildConnectionToDatabase();
        if (con != null){
            String query = "UPDATE userinform SET email = ?, username = ? WHERE id = ?";
            PreparedStatement PStatement = con.prepareStatement(query);
            PStatement.setString(1,userInform.get("email"));
            PStatement.setString(2,userInform.get("username"));
            PStatement.setInt(3,id);
            PStatement.execute();
            PStatement.close();
            con.close();
        }
        else {
            System.out.println("Can't access Database");
        }
        return new JSONObject(FindUser(id)).toString();
    }
    public static void DeleteUser(int id) throws SQLException {
        Connection con = BuildConnectionToDatabase();
        if (con != null){
            String query = "DELETE FROM userinform WHERE id = ?";
            PreparedStatement PStatement = con.prepareStatement(query);
            PStatement.setInt(1,id);
            PStatement.execute();
            PStatement.close();
            con.close();
        }
        else {
            System.out.println("Can't access Database");
        }
    }
    public static void AddToken(String token) throws SQLException{
        Connection con = BuildConnectionToDatabase();
        if (con != null){
            String query = " insert into xsrftokens(XSRFToken)" + " values (?)";
            PreparedStatement PStatement = con.prepareStatement(query);
            PStatement.setString(1, token);
            PStatement.execute();
            con.close();
        }
        else {
            System.out.println("Can't access Database");
        }
    }
    public static Boolean CheckXSRFToken(String[] lines) throws SQLException{
        Connection con = BuildConnectionToDatabase();
        boolean status = false;
        if (con != null){
            List<String> Tokens = new ArrayList<>();
            String query = "SELECT * FROM xsrftokens";
            Statement s = con.createStatement();
            ResultSet set = s.executeQuery(query);
            while (set.next()){
                Tokens.add(set.getString("XSRFToken"));
            }
            con.close();
            boolean TokenFind = false;
            for (int idx=0;idx<lines.length;idx++){
                if (lines[idx].contains("Content-Disposition: form-data; name=\"xsrf_token\"")){
                    TokenFind = true;
                    idx++;
                }
                else if (lines[idx].contains("------WebKitFormBoundary")){
                    TokenFind = false;
                }
                else if(TokenFind){
                    if(lines[idx].length()-1 <= 20 || lines[idx].length()-1 >= 8 ){
                        for (String t : Tokens){
                            if (t.equals(lines[idx].substring(0, lines[idx].length() - 1))) {
                                status = true;
                                // add 11/9 after hw2
                                break;
                            }
                        }
                    }
                }
            }
        }
        else {
            System.out.println("Can't access Database");
        }
        return status;
    }
    public static void AddInput(List<String> ListOfComment, int ImageIdx) throws SQLException {
        Connection con = BuildConnectionToDatabase();
        if (con != null){
            for (int idx=0;idx<ListOfComment.size();idx++) {
                String query = " insert into inputdata(Comment,ImageIDX)" + " values (?,?)";
                PreparedStatement PStatement = con.prepareStatement(query);
                PStatement.setString(1, ListOfComment.get(idx));
                // this make use image is not repeat for every line of commend
                if (idx == ListOfComment.size()-1){
                    // at image to last line of comment
                    PStatement.setInt(2,ImageIdx);
                }
                else {
                    //other line of comment no image
                    PStatement.setInt(2,-1);
                }
                PStatement.execute();
            }
            con.close();
        }
        else {
            System.out.println("Can't access Database");
        }
    }
    public static String GetInputData() throws SQLException {
        StringBuilder Data = new StringBuilder();
        Connection con = BuildConnectionToDatabase();
        if (con != null){
            String query = "SELECT * FROM inputdata";
            Statement s = con.createStatement();
            ResultSet set = s.executeQuery(query);
            while (set.next()){
                Data.append(set.getString("Comment"));
                if(set.getInt("ImageIDX") != -1){
                    Data.append("<img src=\"src/main/UploadImage/Image").append(set.getInt("ImageIDX")).append(".jpg\" alt=\"Image").append(set.getInt("ImageIDX")).append(".jpg\"><br>\n");
                    TCPServer.ImageIDX = set.getInt("ImageIDX");
                }
            }
            con.close();
        }
        else {
            System.out.println("Can't access Database");
        }
        return Data.toString();
    }
    public static void SaveChat(String Message) throws SQLException {
        Connection con = BuildConnectionToDatabase();
        if (con != null){
            String query = "insert into chathistory(Message)" + "value (?)";
            PreparedStatement PStatement = con.prepareStatement(query);
            PStatement.setString(1, Message);
            PStatement.execute();
            con.close();
        }
        else {
            System.out.println("Can't access database");
        }
    }
    public static List<String> GetChatHistory() throws SQLException {
        List<String> ChatHistory = new ArrayList<>();
        Connection con = BuildConnectionToDatabase();
        if (con != null){
            String query = "SELECT * FROM chathistory";
            Statement s = con.createStatement();
            ResultSet set = s.executeQuery(query);
            while (set.next()){
                ChatHistory.add(set.getString("Message"));
            }
            con.close();
        }
        else {
            System.out.println("Can't access Database");
        }
        return ChatHistory;
    }
}
