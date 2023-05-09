package cs739.gossiper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.jsoniter.output.JsonStream;

import cs739.gossiper.messages.Message;

public class SendMessage implements Runnable {
	private static final Logger logger = LogManager.getLogger(SendMessage.class);

	private final Address address;
	private final Message message;
	
	
	public SendMessage(Address address, Message message) {
		this.address = address;
		this.message = message;
	}

	public static long computeCrc(byte[] bytes) {
		CRC32 crc = new CRC32();
		crc.update(bytes);
		return crc.getValue();		
	}
	
	public byte[] read(InputStream is, int size) throws IOException {
		byte[] result = new byte[size];
		int offset = 0;
		int remaining = size;
		while(remaining > 0) {
			int count = is.read(result, offset, remaining);
			if(count<0) throw new IOException("premute end of stream");
			offset += count;
			remaining -= offset;
		}
		return result;
	}
	
    public static void send(OutputStream os, Message message) throws IOException {
        String result = JsonStream.serialize(message);
        byte[] body = result.getBytes(StandardCharsets.UTF_8);
        byte[] header = new byte[16];
        ByteBuffer buffer = ByteBuffer.wrap(header);
        buffer.putInt(message.getType().getValue());
        buffer.putInt(body.length);
        buffer.putLong(computeCrc(body));
        os.write(header);
        os.write(body);
    }


	@Override
	public void run() {
	    logger.info("creating socket");
        
		try(Socket socket = new Socket(address.ipAddress, address.port)) {
	        logger.info("socket created -- sending message");
		    send(socket.getOutputStream(), message);					
		} catch(Throwable t) {
			logger.error("failed to send to "+address, t);
		}
	}}
