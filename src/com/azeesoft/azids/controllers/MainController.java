package com.azeesoft.azids.controllers;

import com.azeesoft.azids.JNetWrapper;
import com.azeesoft.azids.Main;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.WindowEvent;
import org.jnetpcap.PcapAddr;
import org.jnetpcap.PcapClosedException;
import org.jnetpcap.PcapIf;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Created by aziz titu2 on 10/20/2016.
 */
public class MainController implements Initializable {

    @FXML
    VBox networkDevBox;

    @FXML
    Button continueBtn;

    ToggleGroup toggleGroup=new ToggleGroup();
    @Override
    public void initialize(URL location, ResourceBundle resources) {
//        new ListView().getSelectionModel().
        List<PcapIf> allDevs= JNetWrapper.getAlldevs();

        toggleGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
            @Override
            public void changed(ObservableValue<? extends Toggle> observable, Toggle oldValue, Toggle newValue) {
                continueBtn.setDisable(false);
            }
        });
        int i=0;
        for(PcapIf dev : allDevs){
            List<PcapAddr> addresses=dev.getAddresses();
            String additional="";
            if(addresses.size()>0)
                additional=addresses.get(0).getAddr().toString();
            RadioButton radioButton=new RadioButton("  "+dev.getDescription()+"    "+additional);
            radioButton.setId("NetRB_"+i);
            radioButton.setFont(new Font(13));
            radioButton.setToggleGroup(toggleGroup);
            /*radioButton.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                }
            });*/
            networkDevBox.getChildren().add(radioButton);
            i++;
        }


    }

    @FXML
    public void continueToNext(){
        RadioButton selRB=(RadioButton)toggleGroup.getSelectedToggle();
        JNetWrapper.selectDevice(Integer.parseInt(selRB.getId().replace("NetRB_","")));
        Main.selectedDevString=selRB.getText();
        //Move to next window

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/azeesoft/azids/res/capture_window.fxml"));
        try {
            Parent root = fxmlLoader.load();
            Main.captureController=fxmlLoader.getController();
            Main.mainStage.setScene(new Scene(root));
            Main.mainStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent event) {
                    try {
                        JNetWrapper.stopCapturing();
                    } catch (PcapClosedException e) {
                        e.printStackTrace();
                    }
                    System.exit(0);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
