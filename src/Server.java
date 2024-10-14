import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class Server {

    HashMap<String,Socket> socketMap = new HashMap<>();
    HashMap<String,String[]> groupList = new HashMap<>();

    public void print(String str, Object... o) {
        System.out.printf(str, o);
    }

    public Server(int port) throws IOException {
        ServerSocket srvSocket = new ServerSocket(port);
        initialiseGroupList();

        while(true) {
            print("Listening at port %d...\n", port);
            Socket clientSocket = srvSocket.accept();

            Thread t = new Thread(()-> {
                try {
                    DataInputStream in = new DataInputStream(clientSocket.getInputStream());
                    DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());

                    String username = "";
                    String password = "";
                    try {
                        while(true){
                            username = receiveString(in);
                            password = receiveString(in);
                            boolean ok = userAuthorized(username, password);
                            if (ok) break;
                            String msg = "User not authorized";
                            sendString(msg, out);
                        }
                        String msg = "User authorized";
                        sendString(msg, out);
                        synchronized (socketMap) {
                            socketMap.put(username, clientSocket);
                        }
                        serve(clientSocket);
                    } catch (IOException ex) {
                        print("Connection drop!");
                    }

                    synchronized (socketMap) {
                        socketMap.remove(username, clientSocket);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });
            t.start();

        }


    }

    private void initialiseGroupList() throws IOException {
        BufferedReader readGroupFile = new BufferedReader(new FileReader("groupList.txt"));
        String group;
        while ((group = readGroupFile.readLine()) != null) {
            String[] groupInfo = group.split(":");
            String[] members = groupInfo[1].split(",");
            groupList.put(groupInfo[0],members);
        }
        readGroupFile.close();
    }


    private void serve(Socket clientSocket) throws IOException {
        print("Established a connection to host %s:%d\n\n",
                clientSocket.getInetAddress(), clientSocket.getPort());

        DataInputStream in = new DataInputStream(clientSocket.getInputStream());

        while(true) {
            int msgType = in.readInt();

            if (msgType == 1) {
                //send a text message to an individual
                String sender = receiveString(in);
                String receiver = receiveString(in);
                String msg = receiveString(in);
                forward(sender, receiver, msg);
                System.out.println(sender + ":" + msg);

            }else if (msgType == 2){
                //send group message
                String sender = receiveString(in);
                String group = receiveString(in);
                String msg = receiveString(in);
                forwardGroup(sender,group,msg);

            } else if (msgType == 3) {
                //send a file to a single individual

                String sender = receiveString(in);
                String receiver = receiveString(in);
                String filename = receiveString(in);

                receiveFile(sender, receiver, filename, in);
                if (filename.contains(".png") || filename.contains(".jpg") || filename.contains(".jpeg")) {
                    forwardFile(sender, receiver,filename);
                } else {
                    String msg = "Download-" + filename;
                    forward(sender, receiver, msg);
                }

            } else if (msgType == 4) {
                String sender = receiveString(in);
                String group = receiveString(in);
                String memberList = receiveString(in);
                String[] members = memberList.split(",");
                groupList.put(group,members);
                forwardGroupListUpdate(sender,group,memberList);

            } else if (msgType == 5) {
                String sender = receiveString(in);
                String receiver = receiveString(in);
                String filename = receiveString(in);
                forwardFile(sender, receiver, filename);
            }

        }

    }

    private void forward(String sender, String receiver, String msg){
        synchronized (socketMap) {
            try {
                if (socketMap.containsKey(receiver)){
                    DataOutputStream out = new DataOutputStream(socketMap.get(receiver).getOutputStream());
                    out.writeInt(1);
                    sendString(sender, out);
                    sendString(receiver,out);
                    sendString(msg,out);
                }
            } catch (IOException ex) {
                print("Unable to forward message to %s:%d\n",
                        socketMap.get(receiver).getInetAddress().getHostName(),
                        socketMap.get(receiver).getPort());
            }
        }
    }

    private void forwardGroup(String sender, String group, String msg) {
        if (groupList.containsKey(group)) {
            for (String member: groupList.get(group)) {
                if (!sender.equals(member)) {
                    synchronized (socketMap) {
                        try {
                            if (socketMap.containsKey(member)){
                                DataOutputStream out = new DataOutputStream(socketMap.get(member).getOutputStream());
                                out.writeInt(2);
                                sendString(sender,out);
                                sendString(group,out);
                                sendString(msg,out);
                            }
                        } catch (IOException ex) {
                            print("Unable to forward message to %s:%d\n",
                                    socketMap.get(member).getInetAddress().getHostName(),
                                    socketMap.get(member).getPort());
                        }
                    }
                }

            }
        }
    }

    private void forwardGroupListUpdate(String sender, String group, String memberList) {
        String groupInfo = group + ":" + memberList;
        if (groupList.containsKey(group)) {
            for (String member: groupList.get(group)) {
                if (!sender.equals(member)) {
                    synchronized (socketMap) {
                        try {
                            if (socketMap.containsKey(member)){
                                DataOutputStream out = new DataOutputStream(socketMap.get(member).getOutputStream());
                                out.writeInt(4);
                                sendString(groupInfo,out);
                            }
                        } catch (IOException ex) {
                            print("Unable to forward message to %s:%d\n",
                                    socketMap.get(member).getInetAddress().getHostName(),
                                    socketMap.get(member).getPort());
                        }
                    }
                }

            }
        }
    }


    public void forwardFile(String sender, String receiver, String filename) {
        synchronized (socketMap) {
            try {
                if (socketMap.containsKey(receiver)) {
                    DataOutputStream out = new DataOutputStream(socketMap.get(receiver).getOutputStream());
                    File file = new File(receiver + "\\" + filename);
                    sendFile(sender, receiver,file, out);
                }
            } catch (IOException ex) {
                print("Unable to forward message to %s:%d\n",
                        socketMap.get(receiver).getInetAddress().getHostName(),
                        socketMap.get(receiver).getPort());
            }
        }
    }

    public void sendFile(String sender, String receiver, File file, DataOutputStream out) throws IOException {
        FileInputStream in = new FileInputStream(file);
        out.writeInt(3);
        sendString(sender, out);
        sendString(receiver, out);

        byte[] filename = file.getName().getBytes();
        out.writeInt(filename.length);
        out.write(filename, 0, filename.length);

        long size = file.length();
        out.writeLong(size);

        byte[] buffer = new byte[1024];
        while(size >0) {
            int len = in.read(buffer, 0, (int) Math.min(size, buffer.length));
            out.write(buffer, 0, len);
            size -= len;
        }

        in.close();
        out.flush();


    }


    private void receiveFile(String sender, String receiver,String filename, DataInputStream in) throws IOException {
        byte[] buffer = new byte[1024];
        //check if folder exists
        new File(receiver).mkdir();
        File file = new File( receiver + "\\" + filename);
        FileOutputStream fout = new FileOutputStream(file);

        long size = in.readLong();

        while(size > 0) {
            int len = in.read(buffer, 0, (int) Math.min(size, buffer.length));
            fout.write(buffer, 0, len);
            size -= len;
        }

        fout.flush();
        fout.close();
    }

    public Boolean userAuthorized(String username, String password) throws IOException{
        String fileName = "userAuth.txt";
        String[] userList = new String[4];
        byte[] buffer = new byte[1024];
        File file = new File(fileName);

        if (file.exists()) {
            long size = file.length();
            FileInputStream in = new FileInputStream(file);
            while (size > 0) {
                int len = in.read(buffer, 0, buffer.length);
                size -= len;
                userList = new String(buffer, 0, len).split(";");
            }
            in.close();
        }
        for (String user: userList) {
            if (user.equals(username + "," + password)) {
                return true;
            }
        }
        return false;

    }
    public void sendString(String string, DataOutputStream out) throws IOException {
        int len = string.length();
        out.writeInt(len);
        out.write(string.getBytes(), 0, len);
        out.flush();
    }

    public String receiveString(DataInputStream in) throws IOException {
        byte[] buffer = new byte[1024];
        String str = "";
        int len = in.readInt();
        while (len > 0) {
            int l = in.read(buffer, 0, Math.min(len, buffer.length));
            str += new String(buffer, 0, l);
            len -= l;
        }
        return str;
    }

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(args[0]);
        new Server(port);
    }
}