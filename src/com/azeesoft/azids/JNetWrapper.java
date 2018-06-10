package com.azeesoft.azids;

import javafx.application.Platform;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import org.jnetpcap.*;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.PcapPacketHandler;
import org.jnetpcap.protocol.network.Ip4;
import org.jnetpcap.protocol.tcpip.Http;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by aziz titu2 on 10/20/2016.
 */
public class JNetWrapper {

    static int snaplen = 64 * 1024;           // Capture all packets, no trucation
    static int flags = Pcap.MODE_PROMISCUOUS; // capture all packets
    static int timeout = 10 * 1000;           // 10 seconds in millis

    static Pcap pcap, pcap_d;
    static List<PcapIf> alldevs;
    static PcapIf selectedDevice;
    static PcapDumper pcapDumper;
    static boolean isCapturing = false, capPaused = false, dumpCAP = true, savePacketInfo = true, detectSSLStripping = true;
    static String capPath, capPath2, sslStripListPath = "ssl_strip_list.txt";

    static int packetCount = 0;

    static OnPacketCaptured onPacketCaptured;

    static StringBuilder capData = new StringBuilder();
    static BufferedWriter capInfoWriter;
    static BufferedReader sslListReader;

    static List<String> sslList = new ArrayList<String>();
    static List<String> sslDomainList = new ArrayList<String>();

    public static void init() {
        alldevs = new ArrayList<PcapIf>(); // Will be filled with NICs
        StringBuilder errbuf = new StringBuilder(); // For any error msgs


        int r = Pcap.findAllDevs(alldevs, errbuf);
        if (r == Pcap.NOT_OK || alldevs.isEmpty()) {
            System.err.printf("Can't read list of devices, error is %s", errbuf
                    .toString());
            return;
        }

        System.out.println("Network devices found:");

//        Scanner scanner=new Scanner(System.in);
        /*int i = 0;
        for (PcapIf device : alldevs) {
            String description =
                    (device.getDescription() != null) ? device.getDescription()
                            : "No description available";
            System.out.printf("#%d: %s [%s]\n\n\n", i++, device.getName(), description*//*,device.toString()*//*);

        }

        System.out.print("Choose a network device: ");

        PcapIf device=alldevs.get(scanner.nextInt());*/


       /* pcap =  Pcap.openLive(selectedDevice.getName(), snaplen, flags, timeout, errbuf);

        if (pcap == null) {
            System.err.printf("Error while opening device for capture: "
                    + errbuf.toString());
            return;
        }*/

        /*PcapPacketHandler<String> jpacketHandler = new PcapPacketHandler<String>() {

            public void nextPacket(PcapPacket packet, String user) {

                System.out.printf("Received packet at %s caplen=%-4d len=%-4d %s\n",
                        new Date(packet.getCaptureHeader().timestampInMillis()),
                        packet.getCaptureHeader().caplen(),  // Length actually captured
                        packet.getCaptureHeader().wirelen(), // Original length
                        user                                 // User supplied object
                );

                System.out.println("Data: "+packet.toString()+"\n\n\n");
            }
        };*/

//        pcap.loop(100, jpacketHandler, "!!!AZIDS!!!");
    }

    public static List<PcapIf> getAlldevs() {
        return alldevs;
    }

    public static void selectDevice(int i) {
        selectedDevice = alldevs.get(i);
    }

    public static void selectDevice(PcapIf device) {
        selectedDevice = device;
    }

    public static void startCapturing(OnPacketCaptured onPacketCaptured, boolean dump, boolean save, boolean detectSSLStrip) {
        setOnPacketCaptured(onPacketCaptured);
        startCapturing(dump, save, detectSSLStrip);
    }

    public static void startCapturing(boolean dump, boolean save, boolean detectSSLStrip) {
        dumpCAP = dump;
        savePacketInfo = save;
        detectSSLStripping = detectSSLStrip;
        capData = new StringBuilder();
        sendToConsole("Opening network device for capture...");
        StringBuilder errbuf = new StringBuilder(); // For any error msgs
        pcap = Pcap.openLive(selectedDevice.getName(), snaplen, flags, timeout, errbuf);
        pcap_d = Pcap.openLive(selectedDevice.getName(), snaplen, flags, timeout, errbuf);

        if (pcap == null) {
            sendToConsole("Error while opening device for capture: "
                    + errbuf.toString(), "");
            return;
        }

        if (pcap_d == null) {
            sendToConsole("Error while opening device for dumping: "
                    + errbuf.toString(), "");
            return;
        }

        sendToConsole("Network device opened...");

        isCapturing = true;
        File file = new File("dumps/");
        file.mkdirs();

        String dt = "" + Calendar.getInstance().get(Calendar.MONTH) + Calendar.getInstance().get(Calendar.DAY_OF_MONTH) + Calendar.getInstance().get(Calendar.YEAR) + Calendar.getInstance().getTimeInMillis();

        capPath = "dumps/azids_packet_dump_" + dt + ".cap";
        capPath2 = "cap_info/azids_packet_info_" + dt + ".txt";
        File file2 = new File(capPath2);
        if (file2.exists())
            file2.delete();

        if (savePacketInfo)
            try {
                File tmpFile = new File("cap_info/");
                if (!tmpFile.exists())
                    tmpFile.mkdirs();

                file2.createNewFile();
                capInfoWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file2)));
                capData = new StringBuilder();
            } catch (IOException e) {
                e.printStackTrace();
            }

        if (detectSSLStripping) {
            File sslListFile = new File(sslStripListPath);
            try {
                if (!sslListFile.exists())
                    sslListFile.createNewFile();

                sslListReader = new BufferedReader(new InputStreamReader(new FileInputStream(sslListFile)));
                sslList.clear();
                sslDomainList.clear();
                String s;
                while ((s = sslListReader.readLine()) != null) {
                    if (s.startsWith("#") || s.trim().isEmpty())
                        continue;

                    String[] sslItem = s.split("#");
                    sslList.add(sslItem[0].trim());

                    if (sslItem[1] == null)
                        sslItem[1] = "";

                    sslDomainList.add(sslItem[1]);
                }
                sslListReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        pcapDumper = pcap_d.dumpOpen(capPath);

        sendToConsole("Initiating packet handlers...");

        PcapHandler<PcapDumper> dumpHandler = new PcapHandler<PcapDumper>() {

            public void nextPacket(PcapDumper dumper, long seconds, int useconds,
                                   int caplen, int len, ByteBuffer buffer) {

                if (dumpCAP)
                    dumper.dump(seconds, useconds, caplen, len, buffer);
            }
        };

        PcapPacketHandler<String> packetHandler = new PcapPacketHandler<String>() {
            @Override
            public void nextPacket(PcapPacket packet, String user) {
                if (!capPaused)
                    Platform.runLater(new AZRunnable<PcapPacket>(packet) {
                        @Override
                        public void run() {
                            PcapPacket packet = getParam();
//                        System.out.println("Received Packet");
                            Ip4 ip = new Ip4();
//                        if(packet.hasHeader(Http.ID) && packet.hasHeader(ip))
                            if (!capPaused && onPacketCaptured != null) {
                                packetCount++;
                                String info = "Received packet at " + new Date(packet.getCaptureHeader().timestampInMillis()) +
                                        "caplen=" + packet.getCaptureHeader().caplen() + " len=" + packet.getCaptureHeader().wirelen() + "\n";

                                String data = "Packet Data: " + packet.toString() + "\n\n\n";

                                capData.append("\n").append(info).append("\n").append(data);

                                if (savePacketInfo)
                                    try {
                                        capInfoWriter.append("\n").append(info).append("\n").append(data);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                if (isCapturing && detectSSLStripping) {
                                    if (packet.hasHeader(Http.ID) && packet.hasHeader(ip)) {
                                        //TODO: DETECT SSL STRIPPING AND ALERT
                                        byte[] sIP = new byte[4];
                                        sIP = packet.getHeader(ip).source();

                                        String sourceIP = org.jnetpcap.packet.format.FormatUtils.ip(sIP);

                                        int sslIndex = sslList.indexOf(sourceIP);
                                        if (sslIndex != -1) {
                                            capPaused = true;
//                                        stopCapturing();
                                            String domain = sslDomainList.get(sslIndex);
                                            if (!domain.isEmpty())
                                                domain = " (" + domain + ")";

                                            Alert alert = new Alert(Alert.AlertType.WARNING, "SSL Stripping detected for packets from IP: " + sourceIP + domain);
                                            onPacketCaptured.onCapture("SSL Stripping detected from IP:" + sourceIP, "");
                                            if (savePacketInfo)
                                                try {
                                                    capInfoWriter.append("\n").append("SSL Stripping detected for packets from IP: ").append(sourceIP).append(domain);
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                            alert.showAndWait();
                                            capPaused = false;

                                        }
                                    }
                                }

                                onPacketCaptured.onCapture(info, data);
                            }
//                        System.out.println("Writing data");
                        }
                    });
            }
        };

        AZTask<PcapHandler<PcapDumper>, PcapPacketHandler<String>> task = new AZTask<PcapHandler<PcapDumper>, PcapPacketHandler<String>>(dumpHandler, packetHandler) {
            @Override
            protected Object call() {
                while (isCapturing) {
                    try {
                        pcap_d.dispatch(10, getParam(), pcapDumper);
                        pcap.dispatch(10, getParam2(), "");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
//                    updateProgress(0, 0);
                }
                return true;
            }
        };

        /*task.progressProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {

            }
        });*/

        task.setOnFailed(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {

                sendToConsole("Capture fail...");
            }
        });

        task.setOnCancelled(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                sendToConsole("Capture cancelled...");
            }
        });

        task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                sendToConsole("Ending capture mode...");

                File file = new File(capPath);
                File file2 = new File(capPath2);

                capData = new StringBuilder();

                if (savePacketInfo)
                    try {
                        capInfoWriter.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                String msg1 = "";
                if (dumpCAP)
                    msg1 = "Successfully dumped the packet data at \"" + file.getAbsolutePath();
                String msg2 = "";
                if (savePacketInfo) {
                    if (!msg1.isEmpty())
                        msg2 = "\n\n";
                    msg2 += "Successfully created the info file at" + file2.getAbsolutePath() + "\"";
                }

                String msg = msg1 + msg2;

                sendToConsole(packetCount + " Packets captured successfully!!!");
                sendToConsole(msg);
                sendToConsole("=================================");
                sendToConsole("\nClick \"Start Capturing\" to continue...");

                Alert alert = new Alert(Alert.AlertType.INFORMATION, msg);
                alert.show();
            }
        });

        Executor executor = Executors.newCachedThreadPool();

        sendToConsole("Capturing packets...");

        executor.execute(task);


//        while(isCapturing){
//        }


    }

    public static void stopCapturing() throws PcapClosedException {

        isCapturing = false;
        if (pcap != null)
            pcap.close();
        if (pcap_d != null)
            pcap_d.close();
        sendToConsole("Closing network device...");
    }

    public static void setOnPacketCaptured(OnPacketCaptured onPacketCaptured) {
        JNetWrapper.onPacketCaptured = onPacketCaptured;
    }

    public static void sendToConsole(String s1) {
        sendToConsole(s1, "");
    }

    public static void sendToConsole(String s1, String s2) {
        if (onPacketCaptured != null)
            onPacketCaptured.onCapture(s1, s2);
    }

    public interface OnPacketCaptured {
        void onCapture(String info, String data);
    }
}

