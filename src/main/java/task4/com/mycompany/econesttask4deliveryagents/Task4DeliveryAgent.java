package com.mycompany.econesttask4deliveryagents;

import java.io.Serializable;

/** Delivery personnel and their vehicle information. */
public class Task4DeliveryAgent implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String agentId;
    private String name;
    private String phone;
    private String email;
    private String vehicleRegistrationNo;
    private String vehicleType;
    private String vehicleModel;

    public Task4DeliveryAgent(String agentId, String name, String phone, String email,
            String vehicleRegistrationNo, String vehicleType, String vehicleModel) {
        this.agentId = agentId;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.vehicleRegistrationNo = vehicleRegistrationNo;
        this.vehicleType = vehicleType;
        this.vehicleModel = vehicleModel;
    }

    public String getAgentId() {
        return agentId;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getVehicleRegistrationNo() {
        return vehicleRegistrationNo;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public String getVehicleModel() {
        return vehicleModel;
    }

    void updateDetails(String name, String phone, String email,
            String vehicleRegistrationNo, String vehicleType, String vehicleModel) {
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.vehicleRegistrationNo = vehicleRegistrationNo;
        this.vehicleType = vehicleType;
        this.vehicleModel = vehicleModel;
    }
}
