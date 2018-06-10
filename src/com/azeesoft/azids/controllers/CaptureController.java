package com.azeesoft.azids.controllers;

import com.azeesoft.azids.JNetWrapper;
import com.azeesoft.azids.Main;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import org.jnetpcap.PcapClosedException;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Created by aziz titu2 on 10/21/2016.
 */
public class CaptureController implements Initializable {

    @FXML
    Button captureBtn;

    @FXML
    Label console, selDevLabel;

    @FXML
    CheckBox cbDumpCAP,cbSavePacketInfo,cbDetectSSLStripping;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        idleMode();
        selDevLabel.setText("("+ Main.selectedDevString+"  )");
    }

    public void captureMode(){
        captureBtn.setText("Stop Capturing");
        captureBtn.setStyle("-fx-background-color: red");
        captureBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                stopCapturing();
            }
        });

        cbDumpCAP.setDisable(true);
        cbSavePacketInfo.setDisable(true);
        cbDetectSSLStripping.setDisable(true);
    }

    public void idleMode(){
        captureBtn.setText("Start Capturing");
        captureBtn.setStyle("-fx-background-color: teal");
        captureBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                startCapturing();
            }
        });

        cbDumpCAP.setDisable(false);
        cbSavePacketInfo.setDisable(false);
        cbDetectSSLStripping.setDisable(false);
    }

    @FXML
    public void startCapturing(){

        captureMode();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        JNetWrapper.startCapturing(new JNetWrapper.OnPacketCaptured() {
            @Override
            public void onCapture(String info, String data) {
                writeToConsole(info);
                writeToConsole(data);
            }
        },cbDumpCAP.isSelected(),cbSavePacketInfo.isSelected(),cbDetectSSLStripping.isSelected());

        /*new PcapPacketHandler<String>() {
            @Override
            public void nextPacket(PcapPacket packet, String user) {

                Platform.runLater(new AZRunnable<PcapPacket>(packet) {
                    @Override
                    public void run() {
                        PcapPacket packet=getParam();
                        System.out.println("Received Packet");
                        writeToConsole("Received packet at "+new Date(packet.getCaptureHeader().timestampInMillis())+
                                "caplen="+packet.getCaptureHeader().caplen()+" len="+packet.getCaptureHeader().wirelen()+"\n");


                        System.out.println("Writing data");

                        writeToConsole("Data: "+packet.toString()+"\n\n\n");
                    }
                });
            }
        }*/

    }

    public void stopCapturing() {
        try {
            JNetWrapper.stopCapturing();
        } catch (PcapClosedException e) {
            e.printStackTrace();
        }
        idleMode();
    }

    public void writeToConsole(String s){
        console.setText(console.getText()+"\n"+s);
    }
}
