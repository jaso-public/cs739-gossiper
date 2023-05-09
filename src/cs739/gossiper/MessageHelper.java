package cs739.gossiper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

import com.jsoniter.JsonIterator;
import com.jsoniter.output.JsonStream;

import cs739.gossiper.messages.BootstrapReply;
import cs739.gossiper.messages.BootstrapRequest;
import cs739.gossiper.messages.Gossip;
import cs739.gossiper.messages.Message;
import cs739.gossiper.messages.MessageType;
import cs739.gossiper.messages.Rumor;

public class MessageHelper {
    
    public static long computeCrc(byte[] bytes) {
        CRC32 crc = new CRC32();
        crc.update(bytes);
        return crc.getValue();      
    }
    
    public static byte[] readHeader(InputStream is, int size) throws IOException {
        byte[] result = new byte[size];
        int offset = 0;
        int remaining = size;
        while(remaining > 0) {
            int count = is.read(result, offset, remaining);
            if(count<0) {
                if(offset==0) return null;
                throw new IOException("premute end of stream");
            }
            offset += count;
            remaining -= offset;
        }
        return result;
    }

    public static byte[] read(InputStream is, int size) throws IOException {
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
    
    public static Message readMessage(InputStream is) throws IOException {
        byte[] header = readHeader(is, 16);
        ByteBuffer buffer = ByteBuffer.wrap(header);
        int messageType = buffer.getInt();
        int messageLength = buffer.getInt();
        long messageCrc = buffer.getLong();
        
        byte[] body = read(is, messageLength);
        if(body==null) throw new IOException("message doesn't have a body");
        if(messageCrc != computeCrc(body)) throw new IOException("message crc mismatch");
        
        if(messageType == MessageType.BootstrapRequest.getValue()) {
            return JsonIterator.deserialize(body, BootstrapRequest.class);
        }
        
        if(messageType == MessageType.BootstrapReply.getValue()) {
            return JsonIterator.deserialize(body, BootstrapReply.class);
        }
        
        if(messageType == MessageType.Gossip.getValue()) {
            return JsonIterator.deserialize(body, Gossip.class);
        }
        
        if(messageType == MessageType.Rumor.getValue()) {
            return JsonIterator.deserialize(body, Rumor.class);
        }
        
//        if(messageType == MessageType.Heartbeat.getValue()) {
//            return JsonIterator.deserialize(body, Heartbeat.class);
//        }
//        
        throw new IOException("don't know how to handle messageType:"+messageType);
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

}
