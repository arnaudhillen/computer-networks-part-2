package client;

import java.io.*;
import java.net.Socket;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * A class representing the connection on the client's side.
 */
public class Connection {

    private Socket socket;
    private final String host;
    
    private final int port;

    private final DataOutputStream outputStream;
    private final DataInputStream inputStream;

    /**
     * The constructor for the connection class. It requires the host name and port number and it creates a socket for this connection.
     * @param host
     * @param port
     * @throws IOException
     */
    public Connection(String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        this.socket = new Socket(this.host, this.port);
        this.outputStream = new DataOutputStream(this.socket.getOutputStream());
        this.inputStream = new DataInputStream(this.socket.getInputStream());
    }

    /**
     * Returns the response to the given request.
     * @param request
     * @return
     * @throws Exception
     */
    public Response get(Request request) throws Exception {
        if (! this.socket.isConnected()) {
            this.socket = new Socket(this.host, this.port);
        }
        byte[] requestBytes = request.toBytes();
        this.outputStream.write(requestBytes);
        Response response = new Response(request);

        // Read character by character because:
        //  1) inputStream.readLine() is deprecated
        while (! response.headFinished()) {
            char nextChar = (char) inputStream.read();
            String line = "";
            while (nextChar != '\n') {
                line += nextChar;
                nextChar = (char) inputStream.read();
            }
            response.interpretHead(line);
        }
        // the head is finished, now we interpret the body
        if (! response.isFinished()) {
        	// Chunked body
        	if (response.isChunked()){        		
        		StringBuilder returnLine = new StringBuilder();
        		while (true){
	                //get the chunk size as a hexadecimal string.
        			String hexaChunkSize = getHexaChunkSize();
        			returnLine.append(hexaChunkSize)
        						.append("\r\n");
                    Integer chunkSize = Integer.parseInt(hexaChunkSize, 16);
                    if (chunkSize.equals(0)){
                    	returnLine.append("0\r\n")
                    				.append("\r\n");
                    	break;
                    }
                    // get the chunk 
                    String chunk = getChunk(chunkSize);
                    returnLine.append(chunk + "\r\n");
        		}
        		byte[] byteData = returnLine.toString().getBytes();
        		response.setData(byteData);
        		
        	// Normal body with given content length
        	}else {
        		final int contentLength = response.getContentLength();
                byte[] data = new byte[contentLength];
                this.inputStream.readFully(data, 0, contentLength);
                response.setData(data);
        	}
        }
        return response;
    }

    public String getHexaChunkSize() throws IOException {
    	String line = "";
        char nextChar = (char) inputStream.read();
    	while (nextChar != '\n') {
               line += nextChar;
            nextChar = (char) inputStream.read();
        }
    	//System.out.println("1)");
    	System.out.println(line);
    	return line.replace("\\r\\n", "");
    }
    
    
    public String getChunk(int chunkSize) throws IOException {
        char nextChar = (char) inputStream.read();
		String line = "";
		//int size = "\r\n".length();
        for (int i=0 ; i < chunkSize + 2 ; i++) {
        	line += nextChar;
            nextChar = (char) inputStream.read();
        } 
    	//System.out.println("2)");
    	//System.out.println(line);
        return line.replaceAll("\\r\\n", "");
    }
    
    /**
     * Closes the socket of this connection.
     * @throws IOException
     */
    public void close() throws IOException {
        this.socket.close();
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
