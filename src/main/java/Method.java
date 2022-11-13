import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.security.*;
import java.util.*;

public class Method {
    public static void OKStatus(BufferedWriter out, int length, String content, boolean FileOrNot) throws IOException {
        out.write("HTTP/1.1 200 OK\r\n");
        out.write("Content-Type: " + content + "; charset=utf-8\r\n");
        if(FileOrNot){ out.write("X-Content-Type-Options: nosniff"); }
        out.write("Content-Length: " + length + "\r\n");
        out.write("\r\n");
        out.flush();
    }
    public static void Redirect(BufferedWriter out, String link) throws IOException {
        out.write("HTTP/1.1 301 Moved Permanently\r\n");
        out.write("Content-Type: text/plain\r\n");
        out.write("Content-Length: 0\r\n");
        out.write("Location: " + link + "\r\n");
        out.write("\r\n");
        out.flush();
    }
    public static void Created(BufferedWriter out, int length) throws IOException {
        out.write("HTTP/1.1 201 Created\r\n");
        out.write("Content-Type: application/json; charset=utf-8\r\n");
        out.write("Content-Length: " + length + "\r\n");
        out.write("\r\n");
        out.flush();
    }
    public static void NoContent(BufferedWriter out) throws IOException {
        out.write("HTTP/1.1 204 No Content\r\n");
        out.write("Content-Type: text/plain\r\n");
        out.write("Content-Length: 0\r\n");
        out.write("\r\n");
        out.flush();
    }
    public static void NotFound(BufferedWriter out) throws  IOException{
        String FZF = "404\n";
        String NF = "The content was not found!\n";
        int NumLen = FZF.getBytes().length;
        int StrLen = NF.getBytes().length;
        int length = NumLen + StrLen;
        out.write("HTTP/1.1 404 Not Found\r\n");
        out.write("Content-Type: text/plain\r\n");
        out.write("Content-Length: " + length + "\r\n");
        out.write("\r\n");
        out.write(FZF);
        out.write(NF);
        out.flush();
    }
    public static void Forbidden(BufferedWriter out) throws IOException{
        String message = "403 Forbidden\nAccess Denied!\n";
        out.write("HTTP/1.1 403 Forbidden\r\n");
        out.write("Content-Type: text/plain\r\n");
        out.write("Content-Length: " + message.getBytes().length + "\r\n");
        out.write("\r\n");
        out.write(message);
        out.flush();
    }
    public static void SwitchingProtocols(BufferedWriter out, String Base64) throws IOException{
        out.write("HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: " + Base64 + "\r\n\r\n");
        out.flush();
    }
    public static void SendFile(BufferedWriter out, File file) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(file));
        String str;
        while ((str= br.readLine()) != null){ out.write(str + "\n"); }
        out.flush();
    }
    public static Map<String, String> GetUserInform(String message){
        Map<String, String> userInform = new HashMap<>();
        boolean NextInform = false;
        boolean InformDone = false;
        boolean key = true;
        String KeyString = "";
        StringBuilder inform = new StringBuilder();
        for (int i = 0; i< message.length(); i++){
            if (message.charAt(i) == '"'){
                if (NextInform){
                    NextInform = false;
                    InformDone = true;
                }
                else { NextInform = true; }
            }
            else if (NextInform){ inform.append(message.charAt(i)); }
            if (InformDone){
                if (key) {
                    KeyString = inform.toString();
                    key = false;
                }
                else {
                    key = true;
                    userInform.put(KeyString, inform.toString());
                }
                inform = new StringBuilder();
                InformDone = false;
            }
        }
        return userInform;
    }
    public static List<String> ReadComment(String[] lines) {
        List<String> comments = new ArrayList<>();
        boolean CommentFind = false;
        for (int idx=0;idx<lines.length;idx++){
            if (lines[idx].contains("Content-Disposition: form-data; name=\"comment\"")){
                CommentFind = true;
                idx++;
            }
            else if (lines[idx].contains("------WebKitFormBoundary")){
                CommentFind = false;
            }
            else if(CommentFind){
                StringBuilder newString = new StringBuilder();
                for (int i=0;i<lines[idx].length();i++){
                    if (lines[idx].charAt(i) == '<'){
                        newString.append("&lt");
                    }
                    else if (lines[idx].charAt(i) == '>'){
                        newString.append("&gt");
                    }
                    else if(lines[idx].charAt(i) == '&'){
                        newString.append("&amp");
                    }
                    else {
                        newString.append(lines[idx].charAt(i));
                    }
                }
                comments.add(newString + "<br>\n");
            }
        }
        return comments;
    }
    public static String AddXSRFToken(File html) throws Exception {
        // Create token (size 8 - 20)
        StringBuilder token = new StringBuilder();
        Random r = new Random();
        for (int i=0;i<r.nextInt(13)+8;i++){
            int a = r.nextInt(62);
            if (a<=25){
                token.append((char)(a + 'a'));
            }
            else if (a<=51){
                token.append((char)((a-26) + 'A'));
            }
            else {
                token.append(a-52);
            }
        }
        //
        Database.AddToken(token.toString());
        // edit html
        StringBuilder file = new StringBuilder();
        BufferedReader br = new BufferedReader(new FileReader(html));
        String text = br.readLine();
        while (text != null){
            file.append(text).append("\n");
            if (text.contains("<form action=\"/image-upload\" id=\"image-form\" method=\"post\" enctype=\"multipart/form-data\">")){
                file.append("    <input value=\"").append(token).append("\" name=\"xsrf_token\" hidden>\n");
            }
            text=br.readLine();
        }
        return file.toString();
    }
    public static String GetImageName(String[] lines){
        StringBuilder ImageName = new StringBuilder();
        for (String line : lines) {
            if (line.contains("Content-Disposition: form-data; name=\"upload\"; filename=")) {
                for (int i = 57; i < line.length() - 2; i++) {
                    ImageName.append(line.charAt(i));
                }
            }
        }
        return ImageName.toString();
    }
    public static String GetBase64Image(String[] lines){
        StringBuilder data = new StringBuilder();
        boolean ImageFind = false;
        for (int idx=0;idx<lines.length;idx++){
            if (lines[idx].contains("Content-Disposition: form-data; name=\"ImageBase64\"")){
                ImageFind = true;
                idx++;
            }
            else if (lines[idx].contains("------WebKitFormBoundary")){
                ImageFind = false;
            }
            else if(ImageFind){
                data.append(lines[idx]);
            }
        }
        return data.toString();
    }
    public static String KeyToBase64(String key) {
        try {
            // append String is GUID
            String KG = key.substring(0,key.lastIndexOf("=")+1) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
            byte[] md = MessageDigest.getInstance("SHA-1").digest(KG.getBytes());
            return Base64.getEncoder().encodeToString(md);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }
    public static int[] GetByte(InputStream IS) throws IOException {
        int maxSize = Integer.MAX_VALUE-5;
        byte[] data = new byte[maxSize];
        // read the data in byte array
        IS.read(data);
        // Get the size of data, remove the zeros;
        for (int i=maxSize-1;i>-1;i--){
            if (data[i] != 0){
                maxSize = i+1;
                break;
            }
        }
        int[] rs = new int[maxSize];
        // this change byte from -128-127 to 0-255
        for (int i=0;i<rs.length;i++){
            rs[i] = (data[i]&0xff);
        }
        return rs;
    }
    public static int[] GetBits(int[] bytes) {
        int[] bits = new int[(bytes.length*8)];
        int idx = 0;
        for (int i=0;i<bytes.length;i++){
            for (int j=8;j>0;j--){
                bits[idx] = (byte)bytes[i] >> (j-1)&1;
                idx++;
            }
        }
        return bits;
    }
    public static int[] GetMask(int[] bits, int StartIDX){
        int[] mask = new int[32];
        for (int i=0;i<32;i++){
            mask[i] = bits[StartIDX+i];
        }
        return mask;
    }
    public static int[] GetPayload(int[] bits, int StartIDX){
        int size = bits.length-StartIDX;
        int[] Payload = new int[size];
        for (int i=0;i<size;i++){
            Payload[i] = bits[StartIDX+i];
        }
        return Payload;
    }
    public static int[] UnmaskPayload(int[] mask, int[] payload){
        int[] newPayload = new int[payload.length];
        if(payload.length < 32){
            for (int i=0;i<payload.length;i++){
                newPayload[i] = mask[i] ^ payload[i];
            }
        }
        else{
            int idx = 0;
            for (int i=0;i<payload.length;i++){
                newPayload[i] = payload[i] ^ mask[idx];
                idx++;
                if (idx == 32){
                    idx = 0;
                }
            }
        }
        return newPayload;
    }
    public static JSONObject BitsToData(int[] payload) throws JSONException {
        JSONObject data = new JSONObject();
        int idx = 0;
        StringBuilder rs = new StringBuilder();
        for (int j=0;j<payload.length/8;j++){
            StringBuilder BinaryLetter = new StringBuilder();
            for (int i=idx;i<idx+8;i++){
                BinaryLetter.append(payload[i]);
            }
            idx+=8;
            rs.append((char)Integer.parseInt(BinaryLetter.toString(),2));
        }
        boolean content = false;
        StringBuilder c = new StringBuilder();
        String key = "";
        for (int i=0;i<rs.length();i++){
            if (rs.charAt(i) == '"'){
                if (content) {
                    if (rs.charAt(i+1) == ':'){
                        key = c.toString();
                        c = new StringBuilder();
                        content = false;
                    }
                    else if (rs.charAt(i+1) == ',' || rs.charAt(i+1) == '}'){
                        data.put(key, c.toString());
                        c = new StringBuilder();
                        content = false;
                    }
                }
                else {
                    content = true;
                }
            }
            else if(content){
                if (rs.charAt(i) == '<'){
                    c.append("&lt");
                }
                else if (rs.charAt(i) == '>'){
                    c.append("&gt");
                }
                else if(rs.charAt(i) == '&'){
                    c.append("&amp");
                }
                else if(rs.charAt(i) == '\\'){
                    if(rs.charAt(i+1) == '"'){
                        c.append("\"");
                        i++;
                    }
                    else if (rs.charAt(i+1) == '\\'){
                        c.append("\\");
                        i++;
                    }
                }
                else {
                    c.append(rs.charAt(i));
                }
            }
        }
        return data;
    }
    public static JSONObject StringToJson (String Message) throws JSONException{
        JSONObject message = new JSONObject();
        boolean content = false;
        StringBuilder c = new StringBuilder();
        String key = "";
        for (int i=0;i<Message.length();i++) {
            if (Message.charAt(i) == '"') {
                if (content) {
                    if (Message.charAt(i + 1) == ':') {
                        key = c.toString();
                        c = new StringBuilder();
                        content = false;
                    }
                    else if (Message.charAt(i + 1) == ',' || Message.charAt(i + 1) == '}') {
                        message.put(key, c.toString());
                        c = new StringBuilder();
                        content = false;
                    }
                }
                else {
                    content = true;
                }
            }
            else if (content){
                c.append(Message.charAt(i));
            }
        }
        return message;
    }
}
