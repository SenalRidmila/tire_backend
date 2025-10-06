package com.example.tire_management.model;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "tire_requests")
public class TireRequest {

    @Id
    private String id;

    private String vehicleNo;
    private String vehicleType;
    private String vehicleBrand;
    private String vehicleModel;
    private String userSection;
    private String replacementDate;  // Keep as String for now to maintain compatibility
    private String existingMake;
    private String tireSize;
    private String noOfTires;
    private String noOfTubes;
    private String costCenter;
    private String presentKm;
    private String previousKm;
    private String wearIndicator;
    private String wearPattern;
    private String officerServiceNo;
    private String email;               // Email field included
    private String comments;
    private List<String> tirePhotoUrls;    // Multiple photo URLs support
    private List<String> photoUrls;

    private String status = "pending";          // Default status
    private String rejectionReason;

    // TTO specific fields
    private String ttoApprovalDate;
    private String ttoRejectionDate;
    private String ttoRejectionReason;

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVehicleNo() {
        return vehicleNo;
    }

    public void setVehicleNo(String vehicleNo) {
        this.vehicleNo = vehicleNo;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getVehicleBrand() {
        return vehicleBrand;
    }

    public void setVehicleBrand(String vehicleBrand) {
        this.vehicleBrand = vehicleBrand;
    }

    public String getVehicleModel() {
        return vehicleModel;
    }

    public void setVehicleModel(String vehicleModel) {
        this.vehicleModel = vehicleModel;
    }

    public String getUserSection() {
        return userSection;
    }

    public void setUserSection(String userSection) {
        this.userSection = userSection;
    }

    public String getReplacementDate() {
        return replacementDate;
    }

    public void setReplacementDate(String replacementDate) {
        this.replacementDate = replacementDate;
    }

    public String getExistingMake() {
        return existingMake;
    }

    public void setExistingMake(String existingMake) {
        this.existingMake = existingMake;
    }

    public String getTireSize() {
        return tireSize;
    }

    public void setTireSize(String tireSize) {
        this.tireSize = tireSize;
    }

    public String getNoOfTires() {
        return noOfTires;
    }

    public void setNoOfTires(String noOfTires) {
        this.noOfTires = noOfTires;
    }

    public String getNoOfTubes() {
        return noOfTubes;
    }

    public void setNoOfTubes(String noOfTubes) {
        this.noOfTubes = noOfTubes;
    }

    public String getCostCenter() {
        return costCenter;
    }

    public void setCostCenter(String costCenter) {
        this.costCenter = costCenter;
    }

    public String getPresentKm() {
        return presentKm;
    }

    public void setPresentKm(String presentKm) {
        this.presentKm = presentKm;
    }

    public String getPreviousKm() {
        return previousKm;
    }

    public void setPreviousKm(String previousKm) {
        this.previousKm = previousKm;
    }

    public String getWearIndicator() {
        return wearIndicator;
    }

    public void setWearIndicator(String wearIndicator) {
        this.wearIndicator = wearIndicator;
    }

    public String getWearPattern() {
        return wearPattern;
    }

    public void setWearPattern(String wearPattern) {
        this.wearPattern = wearPattern;
    }

    public String getOfficerServiceNo() {
        return officerServiceNo;
    }

    public void setOfficerServiceNo(String officerServiceNo) {
        this.officerServiceNo = officerServiceNo;
    }

    public String getemail() {
        return email;
    }

    public void setemail(String email) {
        this.email = email;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public List<String> getTirePhotoUrls() {
        return tirePhotoUrls;
    }

    public void setTirePhotoUrls(List<String> tirePhotoUrls) {
        this.tirePhotoUrls = tirePhotoUrls;
    }

    public List<String> getPhotoUrls() {
        return photoUrls;
    }

    public void setPhotoUrls(List<String> photoUrls) {
        this.photoUrls = photoUrls;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getTtoApprovalDate() {
        return ttoApprovalDate;
    }

    public void setTtoApprovalDate(String ttoApprovalDate) {
        this.ttoApprovalDate = ttoApprovalDate;
    }

    public String getTtoRejectionDate() {
        return ttoRejectionDate;
    }

    public void setTtoRejectionDate(String ttoRejectionDate) {
        this.ttoRejectionDate = ttoRejectionDate;
    }

    public String getTtoRejectionReason() {
        return ttoRejectionReason;
    }

    public void setTtoRejectionReason(String ttoRejectionReason) {
        this.ttoRejectionReason = ttoRejectionReason;
    }
}
