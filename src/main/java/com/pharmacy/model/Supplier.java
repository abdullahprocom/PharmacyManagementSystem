package com.pharmacy.model;

public class Supplier {
    private int supplierId;
    private String supplierName;
    private String phone;
    private String address;
    private boolean active;

    public Supplier() {}

    public Supplier(int supplierId, String supplierName, String phone, String address, boolean active) {
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.phone = phone;
        this.address = address;
        this.active = active;
    }

    public int getSupplierId() { return supplierId; }
    public void setSupplierId(int supplierId) { this.supplierId = supplierId; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() {
        return supplierName;
    }
}
