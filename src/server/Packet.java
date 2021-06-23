package server;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Packet represents a simulated network packet.
 * As we don't have unsigned types in Java, we can achieve this by using a larger type.
 */
public class Packet {

    public static final int MIN_LEN = 0;
    public static final int MAX_LEN = 11 + 1024;

    private int type;
    private long sequenceNumber;
    private InetAddress peerAddress;
    private int peerPort;
    private byte[] payload;

    public Packet(){

    }

    public Packet(int type, long sequenceNumber, InetAddress peerAddress, int peerPort, byte[] payload) {
        this.type = type;
        this.sequenceNumber = sequenceNumber;
        this.peerAddress = peerAddress;
        this.peerPort = peerPort;
        this.payload = payload;
    }

    public int getType() {
        return type;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public InetAddress getPeerAddress() {
        return peerAddress;
    }

    public int getPeerPort() {
        return peerPort;
    }

    public byte[] getPayload() {
        return payload;
    }

    // a packet calls this method to convert itself to ByteBuffer, to be sent
    public ByteBuffer readyToSend(){
        ByteBuffer buf = ByteBuffer.allocate(MAX_LEN).order(ByteOrder.BIG_ENDIAN);
        buf.put((byte) type);
        buf.putInt((int) sequenceNumber);
        buf.put(peerAddress.getAddress());
        buf.putShort((short) peerPort);
        buf.put(payload);
        buf.flip();
        return buf;
    }

    //create a new packet, then use it to call this method, to put the ByteBuffer inside
    public void receivePacket(ByteBuffer buf)throws Exception{
        if (buf.limit() < MIN_LEN || buf.limit() > MAX_LEN) {
            throw new IOException("Invalid length");
        }

        this.type = Byte.toUnsignedInt(buf.get());
        this.sequenceNumber = Integer.toUnsignedLong(buf.getInt());

        byte[] host = new byte[]{buf.get(), buf.get(), buf.get(), buf.get()};

        this.peerAddress = Inet4Address.getByAddress(host);
        this.peerPort = Short.toUnsignedInt(buf.getShort());

        byte[] payload = new byte[buf.remaining()];
        buf.get(payload);

        this.payload = payload;
    }
}
