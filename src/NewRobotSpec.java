/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author kbern
 */
public class NewRobotSpec {
   
    public String robottype;
    public String controlsystem;
    public double x;
    public double y;
    public double theta;
    public String forecolor;
    public String backcolor;
    public int    visionclass;
    
    public NewRobotSpec (String robottype, String controlsystem, double x, double y, double theta, 
            String forecolor, String backcolor, int visionclass) {
        this.robottype = robottype;
        this.controlsystem=controlsystem;
        this.x=x;
        this.y=y;
        this.theta=theta;
        this.forecolor=forecolor;
        this.backcolor=backcolor;
        this.visionclass=visionclass;
    }
}
