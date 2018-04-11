package me.maeroso;

import java.io.File;
import java.net.InetAddress;
import java.security.PublicKey;

public class Peer {
    public int unicastPort;
    public InetAddress host;
    PublicKey publicKey;
    File[] files;
    long reputation;
}
