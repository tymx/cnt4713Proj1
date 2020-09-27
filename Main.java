package com.ftpclient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class Main {

    private static String calculatePASV(String o){

        String number, ipAddress, portString, output;
        int portNumber;

        number = o.substring(o.lastIndexOf("(") + 1, o.length() - 2);

        int portStart = 0;

        int count = 0;
        for(int i = 0; i < number.length(); i++) {
            if(number.charAt(i) == ','){
                count++;
            }
            if(count == 4){
                portStart = i + 1;
                break;
            }
        }

        ipAddress = number.substring(0, portStart - 1);
        ipAddress = ipAddress.replaceAll(",", ".");
        portString = number.substring(portStart);
        portNumber = (Integer.parseInt(portString.substring(0, portString.indexOf(","))) * 256) + Integer.parseInt(portString.substring(portString.indexOf(",") + 1));

        //System.out.println("IP: " + ipAddress);
        //System.out.println("Port: " + portNumber);

        output = ipAddress + "/" + portNumber;

        return output;
    }

    public static void main(String[] args) throws IOException {

        String serverName = "";
        String input, output, command, parameter, pasvInput, cal, ipAddress = "", si;
        int serverCode = 0;
        int portNumber = 21;
        int pasvport = 0;
        BufferedReader in =  new BufferedReader(new InputStreamReader(System.in));
        String username, password;
        Socket clientSocket, pasvSocket;
        BufferedReader pasvOutput;


        boolean loginSuccessful = false, user = false, pass = false, pasv = false;

        //Retrieves the server name from the command line.
        if(args.length == 0) {
            System.out.println("Did not provide a server name.");
            return;
        } else {
            serverName = args[0];
            serverName = serverName.replace("\n", "");
        }

        if(serverName.compareTo("") == 0) {
            System.out.println("Did not provide a server name.");
            return;
        }

        clientSocket = new Socket(serverName /*"inet.cs.fiu.edu"*/, portNumber);
        PrintWriter socketOutput = new PrintWriter(clientSocket.getOutputStream(), true);
        BufferedReader socketInput = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        System.out.println(socketInput.readLine());
        socketOutput.println("OPTS UTF8 ON");
        System.out.println(socketInput.readLine());

        System.out.print("Enter the FTP username:\nmyftp> ");

        while ((input = in.readLine()) != null){
            //Quit Command
            if(input.compareTo("quit") == 0){
                socketOutput.println(input);
                output = socketInput.readLine();
                System.out.println(output);

                System.out.println("Closing Streams.");
                clientSocket.close();
//                in.close();
//                socketOutput.close();
//                socketInput.close();

                return;
            }

            //Login Attempts
            if(!loginSuccessful){
                if(!user){
                    socketOutput.println("USER " + input);
                    output = socketInput.readLine();
                    System.out.print(output + "\nmyftp> ");
                    user = true;
                    continue;
                }
                if(!pass){
                    socketOutput.println("PASS " + input);
                    output = socketInput.readLine();
                    serverCode = Integer.parseInt(output.substring(0,3));
                    System.out.println(output);
                    pass = true;
                }
                if(serverCode == 230){
                    loginSuccessful = true;
                }
                if(serverCode == 530){
                    user = false;
                    pass = false;
                    continue;
                }
            }

            if(loginSuccessful){
                // The CD Command
                if(input.lastIndexOf("cd ") == 0){
                    //System.out.println("The CD command was called.");
                    parameter = input.substring(3);
                    socketOutput.println("CWD " + parameter);
                    output = socketInput.readLine();
                    System.out.println(output);
                }

                // The DELETE Command
                if(input.lastIndexOf("delete ") == 0){
                    //System.out.println("The DELETE command was called.");
                    parameter = input.substring(7);
                    socketOutput.println("DELE " + parameter);
                    output = socketInput.readLine();
                    System.out.println(output);
                }

                // The GET Command
                if(input.lastIndexOf("get ") == 0){
                    //System.out.println("The GET command was called.");
                    parameter = input.substring(4);
                    //Start PASV socket
                    if(!pasv){
                        socketOutput.println("PASV");
                        output = socketInput.readLine();
                        System.out.println(output);
                        socketOutput.println("TYPE I");
                        System.out.println(socketInput.readLine());
                        cal = calculatePASV(output);
                        ipAddress = cal.substring(0,cal.indexOf("/"));
                        pasvport = Integer.parseInt(cal.substring(cal.indexOf("/") + 1));
//                      System.out.println("ipAddress: " + ipAddress);
                        pasv = true;
                    }

                    socketOutput.println("RETR " + parameter);
                    pasvSocket = new Socket(ipAddress, pasvport);
                    pasvOutput = new BufferedReader(new InputStreamReader(pasvSocket.getInputStream()));

                    System.out.println(socketInput.readLine());
                    System.out.println(socketInput.readLine());

                    System.out.println(pasvOutput.readLine());

                    pasvOutput.close();
                    pasvSocket.close();

                    pasv = false;
                }

                //LS COMMAND
                if(input.lastIndexOf("ls") == 0) {
                //System.out.println("The LS command was called.");
                //Start PASV socket
                    if (!pasv) {
                        socketOutput.println("PASV");
                        output = socketInput.readLine();
                        System.out.println(output);
                        socketOutput.println("TYPE I");
                        System.out.println(socketInput.readLine());
                        cal = calculatePASV(output);
                        ipAddress = cal.substring(0, cal.indexOf("/"));
                        pasvport = Integer.parseInt(cal.substring(cal.indexOf("/") + 1));
    //                      System.out.println("ipAddress: " + ipAddress);
                        pasv = true;
                    }

                    socketOutput.println("LIST ");
                    pasvSocket = new Socket(ipAddress, pasvport);
                    System.out.println(socketInput.readLine());
                    pasvOutput = new BufferedReader(new InputStreamReader(pasvSocket.getInputStream()));

                    while (pasvOutput.ready()) {
                        System.out.println(pasvOutput.readLine());
                    }

                    System.out.println(socketInput.readLine());

                    pasvOutput.close();
                    pasvSocket.close();

                    pasv = false;
                }

                //PUT COMMAND
                if(input.lastIndexOf("put ") == 0) {
                    //System.out.println("The PUT command was called.");
                    parameter = input.substring(4);
                    //Start PASV socket
                    if (!pasv) {
                        socketOutput.println("PASV");
                        output = socketInput.readLine();
                        System.out.println(output);
                        socketOutput.println("TYPE I");
                        cal = calculatePASV(output);
                        ipAddress = cal.substring(0, cal.indexOf("/"));
                        pasvport = Integer.parseInt(cal.substring(cal.indexOf("/") + 1));
//                      System.out.println("ipAddress: " + ipAddress);
                        pasv = true;
                    }

                    socketOutput.println("STOR " + parameter);
                    pasvSocket = new Socket(ipAddress, pasvport);
                    pasvOutput = new BufferedReader(new InputStreamReader(pasvSocket.getInputStream()));

                    System.out.println(socketInput.readLine());
                    System.out.println(socketInput.readLine());

                    while(socketInput.ready()){
                        System.out.println(socketInput.readLine());
                    }

                    while (pasvOutput.ready()) {
                        System.out.println(pasvOutput.readLine());
                    }

                    //System.out.println(pasvOutput.readLine());

                    pasvOutput.close();

                    pasv = false;
                }

            }
            System.out.print("myftp> ");
        }
    }
}
