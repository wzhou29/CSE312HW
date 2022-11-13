import org.json.*;
import javax.imageio.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
public class TCPServer {
    public static int ImageIDX = -1;
    public static boolean NotCheckDatabase = true;
    public static Map<Socket,Integer> clients = new HashMap<>();
    public static Map<Integer, InputStream> IPS = new HashMap<>();
    public static Map<Integer, OutputStream> OPS = new HashMap<>();
    public static void TCPSocket() {
        try (ServerSocket TCPServer = new ServerSocket(8080)) {
            TCPServer.setSoTimeout(1000);
            while (true) {
                // TCP Socket
                try {
                    boolean ws = false;
                    Socket client = TCPServer.accept();
                    InputStream InputS = client.getInputStream();
                    OutputStream clientOP = client.getOutputStream();
                    InputStreamReader ISR = new InputStreamReader(InputS);
                    BufferedReader in = new BufferedReader(ISR);
                    OutputStreamWriter clientOSW = new OutputStreamWriter(clientOP);
                    BufferedWriter out = new BufferedWriter(clientOSW);
                    String str = in.readLine();
                    // read tcp socket message
                    if (str != null){
                        if (NotCheckDatabase){
                            Database.SetupDatabase();
                            NotCheckDatabase = false;
                        }
                        StringBuilder input = new StringBuilder();
                        System.out.println(str);
                        if (str.contains("GET")){
                            for (int i = 5; i < str.length() - 9; i++) { input.append(str.charAt(i)); }
                            if (input.toString().equals("")) {
                                File html = new File("src/main/sample_page/index.html");
                                if (html.exists()){
                                    // Get the Comments From Database
                                    String InputData = Database.GetInputData();
                                    // Change html file add random create token
                                    String newHTML = Method.AddXSRFToken(html);
                                    // Add ok status with token add html size and comments size
                                    Method.OKStatus(out,newHTML.length() + InputData.getBytes().length, "text/html", true);
                                    // send new html
                                    out.write(newHTML);
                                    // send the Data to page.
                                    out.write(InputData);
                                    //exit
                                    out.flush();
                                }else{
                                    System.out.println("HTML File doesn't exist!");
                                    Method.NotFound(out);
                                }
                            }
                            else if (input.toString().equals("style.css")){
                                File css = new File("src/main/sample_page/style.css");
                                if (css.exists()){
                                    Method.OKStatus(out, (int) css.length(), "text/css", true);
                                    Method.SendFile(out, css);
                                }else{
                                    System.out.println("CSS File doesn't exist!");
                                    Method.NotFound(out);
                                }
                            }
                            else if (input.toString().equals("functions.js")) {
                                File js = new File("src/main/sample_page/functions.js");
                                if (js.exists()){
                                    Method.OKStatus(out, (int) js.length(), "text/javascript", true);
                                    Method.SendFile(out, js);
                                }else{
                                    System.out.println("JavaScript File doesn't exist!");
                                    Method.NotFound(out);
                                }
                            }
                            else if (input.toString().contains("src/main/UploadImage/")){
                                StringBuilder PicName = new StringBuilder();
                                for (int i = 21; i < input.length(); i++) {
                                    PicName.append(input.charAt(i));
                                }
                                File image = new File("src/main/UploadImage/" + PicName);
                                if (image.exists() || PicName.toString().contains("/")){
                                    BufferedImage BufImage = ImageIO.read(image);
                                    Method.OKStatus(out, (int)image.length(), "image/jpeg", true);
                                    ImageIO.write(BufImage,"jpeg", clientOP);
                                }
                                else{ Method.NotFound(out); }
                            }
                            else if (input.toString().equals("hello")) {
                                String output = "Hello World!\n";
                                int length = output.getBytes().length;
                                String content = "text/plain";
                                Method.OKStatus(out, length, content, false);
                                out.write(output);
                                out.flush();
                            }
                            else if (input.toString().equals("hi")) {
                                Method.Redirect(out, "hello");
                            }
                            else if (input.toString().contains("image")){
                                StringBuilder PicName = new StringBuilder();
                                for (int i = 6; i < input.length(); i++) {
                                    PicName.append(input.charAt(i));
                                }
                                File image = new File("src/main/sample_page/image/" + PicName);
                                if (image.exists() || PicName.toString().contains("/")){
                                    BufferedImage BufImage = ImageIO.read(image);
                                    Method.OKStatus(out, (int)image.length(), "image/jpeg", true);
                                    ImageIO.write(BufImage,"jpeg", clientOP);
                                }
                                else{ Method.NotFound(out); }
                            }
                            else if (input.toString().contains("users")){
                                if (input.toString().equals("users")){
                                    String json = Database.GetTable();
                                    Method.OKStatus(out,json.getBytes().length,"application/json", false);
                                    clientOSW.write(json);
                                    clientOSW.flush();
                                }
                                else{
                                    StringBuilder UserID = new StringBuilder();
                                    for (int i=6;i<input.toString().length();i++){
                                        UserID.append(input.toString().charAt(i));
                                    }
                                    try {
                                        int id = Integer.parseInt(UserID.toString());
                                        Map<String,String> User = Database.FindUser(id);
                                        if(User.isEmpty()){
                                            Method.NotFound(out);
                                        }
                                        else {
                                            String json = new JSONObject(User).toString();
                                            Method.OKStatus(out,json.getBytes().length,"application/json", false);
                                            clientOSW.write(json);
                                            clientOSW.flush();
                                        }
                                    }
                                    catch (NumberFormatException e) { e.printStackTrace(); }
                                }
                            }
                            else if (input.toString().contains("websocket")){
                                StringBuilder message = new StringBuilder();
                                while (in.ready()){
                                    char[] c = new char[1];
                                    if (in.read(c) != -1){
                                        message.append(c);
                                    }
                                }
                                String[] lines = message.toString().split("\\n");
                                String key = "";
                                for (String l : lines) {
                                    if (l.contains("Sec-WebSocket-Key: ")) {
                                        key = l.substring(l.lastIndexOf(":") + 2);
                                    }
                                }
                                String response = Method.KeyToBase64(key);
                                if (!response.equals("")){
                                    Method.SwitchingProtocols(out, response);
                                }
                                else { System.out.println("error on Key to Accept Response"); }
                                ws = true;
                                // create random uid
                                Random r = new Random();
                                int uid = r.nextInt(Integer.MAX_VALUE-5);
                                while (clients.containsValue(uid)){
                                    uid = r.nextInt(Integer.MAX_VALUE-5);
                                }
                                clients.put(client,uid);
                                IPS.put(uid,InputS);
                                OPS.put(uid,clientOP);
                            }
                            else if (input.toString().contains("chat-history")){;
                                List<String> ChatHistory = Database.GetChatHistory();
                                JSONArray Json= new JSONArray();
                                for (String s : ChatHistory){;
                                    Json.put(Method.StringToJson(s));
                                }
                                Method.OKStatus(out, Json.toString().length(), "application/json",false);
                                out.write(Json.toString());
                                out.flush();
                            }
                            else { Method.NotFound(out); }
                        }
                        else if (str.contains("POST")){
                            for (int i = 6; i < str.length() - 9; i++) { input.append(str.charAt(i)); }
                            if (input.toString().equals("users")){
                                StringBuilder message = new StringBuilder();
                                while (in.ready()){
                                    char[] c = new char[1];
                                    if (in.read(c) != -1){
                                        message.append(c);
                                    }
                                }
                                if (message != null){
                                    String json = Database.AddUser(Method.GetUserInform(message.toString()));
                                    Method.Created(out, json.getBytes().length);
                                    clientOSW.write(json);
                                    clientOSW.flush();
                                }
                                else { System.out.println("Can't Find message"); }
                            }
                            // For comment and image upload
                            else if (input.toString().equals("image-upload")){
                                StringBuilder message = new StringBuilder();
                                while (in.ready()){
                                    char[] c = new char[1];
                                    if (in.read(c) != -1){
                                        message.append(c);
                                    }
                                }
                                String[] lines = message.toString().split("\\n");
                                if (Database.CheckXSRFToken(lines)){
                                    int ImageIdx = -1;
                                    String ImageName = Method.GetImageName(lines);
                                    String FileFormat = ImageName.substring(ImageName.lastIndexOf(".") + 1);
                                    if (FileFormat.equalsIgnoreCase("jpg") || FileFormat.equalsIgnoreCase("jpeg")){
                                        ImageIDX++;
                                        String Image = Method.GetBase64Image(lines);
                                        byte[] data = Base64.getDecoder().decode(Image.substring(23,Image.length()-1));
                                        Files.write(Paths.get("src/main/UploadImage/Image" + ImageIDX + ".jpg"), data);
                                        ImageIdx = ImageIDX;
                                    }
                                    Database.AddInput(Method.ReadComment(lines), ImageIdx);
                                    Method.Redirect(out,"/");
                                }
                                else { Method.Forbidden(out);}
                            }
                            else { Method.NotFound(out);}
                        }
                        else if (str.contains("PUT")){
                            for (int i = 5; i < str.length() - 9; i++) { input.append(str.charAt(i)); }
                            if (input.toString().contains("users")){
                                if (input.toString().equals("users")) {
                                    Method.NotFound(out);
                                    System.out.println("Need User ID");
                                }
                                else {
                                    StringBuilder UserID = new StringBuilder();
                                    for (int i=6;i<input.toString().length();i++){ UserID.append(input.toString().charAt(i)); }
                                    try {
                                        int id = Integer.parseInt(UserID.toString());
                                        StringBuilder message = new StringBuilder();
                                        while (in.ready()){
                                            char[] c = new char[1];
                                            if (in.read(c) != -1){
                                                message.append(c);
                                            }
                                        }
                                        if (message != null){
                                            String json = Database.UpdateUser(id,Method.GetUserInform(message.toString()));
                                            if (json.contains("id")){
                                                Method.OKStatus(out,json.getBytes().length, "application/json", false);
                                                clientOSW.write(json);
                                                clientOSW.flush();
                                            }
                                            else {
                                                Method.NotFound(out);
                                            }
                                        }
                                    }
                                    catch (NumberFormatException e) { e.printStackTrace(); }
                                }
                            }
                            else {
                                Method.NotFound(out);
                            }
                        }
                        else if (str.contains("DELETE")){
                            for (int i = 8; i < str.length() - 9; i++) { input.append(str.charAt(i)); }
                            if (input.toString().contains("users")){
                                if (input.toString().equals("users")) {
                                    Method.NotFound(out);
                                    System.out.println("Need User ID");
                                }
                                else {
                                    StringBuilder UserID = new StringBuilder();
                                    for (int i=6;i<input.toString().length();i++){ UserID.append(input.toString().charAt(i)); }
                                    try {
                                        int id = Integer.parseInt(UserID.toString());
                                        if (Database.FindUser(id).isEmpty()){
                                            Method.NotFound(out);
                                        }
                                        else {
                                            Database.DeleteUser(id);
                                            Method.NoContent(out);
                                        }

                                    }
                                    catch (NumberFormatException e) { e.printStackTrace(); }
                                }
                            }
                            else {
                                Method.NotFound(out);
                            }
                        }
                    }
                    // close socket if client is not a websocket
                    if (!ws){
                        client.close();
                        InputS.close();
                        clientOSW.close();
                        in.close();
                        out.close();
                    }
                }
                catch (Exception e){ e.printStackTrace(); }
                // Websocket
                try{
                    if (!clients.isEmpty()){
                        for (Socket c : clients.keySet()){
                            // close the websocket and delete client from list if client is disconnected
                            if (c.isClosed()){
                                c.close();
                                OPS.remove(clients.get(c));
                                IPS.remove(clients.get(c));
                                clients.remove(c);
                            }
                            // Receive Socket Message
                            else if (IPS.get(clients.get(c)).available() != 0){
                                int[] Bytes = Method.GetByte(IPS.get(clients.get(c)));
                                int[] bits = Method.GetBits(Bytes);
                                // Get opcode
                                String opcode = "";
                                for (int i=4;i<8;i++){
                                    opcode += bits[i];
                                }
                                // close connection, delete client from client list
                                if (opcode.equals("1000")){
                                    c.close();
                                    OPS.remove(clients.get(c));
                                    IPS.remove(clients.get(c));
                                    clients.remove(c);
                                }
                                // text
                                else if(opcode.equals("0001")){
                                    // check for mask
                                    boolean HaveMask = true;
                                    if (bits[8] == 0){
                                        HaveMask = false;
                                    }
                                    // Get payload len
                                    String len = "";
                                    for (int i=9;i<16;i++){
                                        len += bits[i];
                                    }
                                    // init
                                    int[] mask;
                                    int[] payload;
                                    int[] UnMaskPayload;
                                    JSONObject data = null;
                                    // Check if len is less 126 then get data
                                    if (Integer.parseInt(len,2) < 126){
                                        if (HaveMask){
                                            mask = Method.GetMask(bits,16);
                                            payload = Method.GetPayload(bits, 48);
                                            UnMaskPayload = Method.UnmaskPayload(mask,payload);
                                            data = Method.BitsToData(UnMaskPayload);
                                        }
                                        else{
                                            payload = Method.GetPayload(bits, 16);
                                            data = Method.BitsToData(payload);
                                        }
                                    }
                                    // next 16 bit is len, then get data
                                    else if(Integer.parseInt(len,2) == 126){
                                        if (HaveMask){
                                            mask = Method.GetMask(bits,32);
                                            payload = Method.GetPayload(bits, 64);
                                            UnMaskPayload = Method.UnmaskPayload(mask,payload);
                                            data = Method.BitsToData(UnMaskPayload);
                                        }
                                        else{
                                            payload = Method.GetPayload(bits, 32);
                                            data = Method.BitsToData(payload);
                                        }
                                    }
                                    // next 64 bit is len, then get data
                                    else if(Integer.parseInt(len,2) == 127){
                                        if (HaveMask){
                                            mask = Method.GetMask(bits,80);
                                            payload = Method.GetPayload(bits, 112);
                                            UnMaskPayload = Method.UnmaskPayload(mask,payload);
                                            data = Method.BitsToData(UnMaskPayload);
                                        }
                                        else{
                                            payload = Method.GetPayload(bits, 80);
                                            data = Method.BitsToData(payload);
                                        }
                                    }
                                    else{
                                        System.out.println("error on find websocket frame len");
                                    }
                                    // Data type is chat message, Send data
                                    if (data.get("messageType").toString().equals("chatMessage")){
                                        data.put("username","User" + clients.get(c));
                                        byte[] ByteData = data.toString().getBytes();
                                        byte[] SendData;
                                        // data is less than 126
                                        if (Integer.parseInt(len,2) < 126){
                                            SendData = new byte[ByteData.length + 2];
                                            SendData[0] = (byte)129;
                                            SendData[1] = (byte)ByteData.length;
                                            int idx = 2;
                                            for (byte d : ByteData){
                                                SendData[idx] = (byte)(d ^ 0xffffff00);
                                                idx++;
                                            }
                                            for (OutputStream o : OPS.values()){
                                                o.write(SendData);
                                            }
                                            data.remove("messageType");
                                            Database.SaveChat(data.toString());
                                        }
                                        // data is more than 126, less than 65536
                                        else if(Integer.parseInt(len,2) == 126){
                                            SendData = new byte[ByteData.length + 4];
                                            SendData[0] = (byte)129;
                                            SendData[1] = 126;
                                            // 16 bit len into two binary
                                            int[] lenInBit = new int[16];
                                            byte dataLen = (byte)ByteData.length;
                                            int index = 0;
                                            for (int i=15;i>-1;i--){
                                                lenInBit[i] = (dataLen >> index) & 1;
                                                index++;
                                            }
                                            String b = "";
                                            for (int i=0;i<8;i++){
                                                b+=lenInBit[i];
                                            }
                                            SendData[2] = (byte)Integer.parseInt(b,2);
                                            b = "";
                                            for (int i=8;i<16;i++){
                                                b+=lenInBit[i];
                                            }
                                            SendData[3] = (byte)Integer.parseInt(b,2);
                                            // Add payload to websocket frame
                                            int idx = 4;
                                            for (byte d : ByteData){
                                                SendData[idx] = (byte)(d ^ 0xffffff00);
                                                idx++;
                                            }
                                            for (OutputStream o : OPS.values()){
                                                o.write(SendData);
                                            }
                                            data.remove("messageType");
                                            Database.SaveChat(data.toString());
                                        }
                                        // data is more than or equal 65536
                                        else if(Integer.parseInt(len,2) == 127){
                                            SendData = new byte[ByteData.length + 2];
                                            SendData[0] = (byte)129;
                                            SendData[1] = 127;
                                            // 32 bit len into four byte
                                            int[] lenInBit = new int[32];
                                            byte dataLen = (byte)ByteData.length;
                                            int index = 0;
                                            for (int i=31;i>-1;i--){
                                                lenInBit[i] = (dataLen >> index) & 1;
                                                index++;
                                            }
                                            String b = "";
                                            for (int i=0;i<8;i++){
                                                b+=lenInBit[i];
                                            }
                                            SendData[2] = (byte)Integer.parseInt(b,2);
                                            b = "";
                                            for (int i=8;i<16;i++){
                                                b+=lenInBit[i];
                                            }
                                            SendData[3] = (byte)Integer.parseInt(b,2);
                                            b = "";
                                            for (int i=16;i<24;i++){
                                                b+=lenInBit[i];
                                            }
                                            SendData[4] = (byte)Integer.parseInt(b,2);
                                            b = "";
                                            for (int i=24;i<32;i++){
                                                b+=lenInBit[i];
                                            }
                                            SendData[5] = (byte)Integer.parseInt(b,2);
                                            // Add payload to websocket frame
                                            int idx = 6;
                                            for (byte d : ByteData){
                                                SendData[idx] = (byte)(d ^ 0xffffff00);
                                                idx++;
                                            }
                                            for (OutputStream o : OPS.values()){
                                                o.write(SendData);
                                            }
                                            data.remove("messageType");
                                            Database.SaveChat(data.toString());
                                        }
                                        else {
                                            System.out.println("Error on find websocket frame len");
                                        }
                                    }
                                    else{
                                        System.out.println("Unknown Message Type");
                                    }
                                }
                                // unknown opcode
                                else {
                                    System.out.println("Unknown opcode");
                                }
                            }
                        }
                    }
                } catch (Exception e){e.printStackTrace();}
            }
        }
        catch (IOException IOE){System.out.println("Error on setting SocketServer: " + Arrays.toString(IOE.getStackTrace()));}
    }
}