package model;

/** Model class for one EcoNest client. */
public class Task2Client {
    private String clientId;
    private String name;
    private String clientType;
    private String phone;
    private String email;
    private String address;

    public Task2Client(String clientId, String name, String clientType, String phone, String email, String address) {
        this.clientId = clientId;
        this.name = name;
        this.clientType = clientType;
        this.phone = phone;
        this.email = email;
        this.address = address;
    }

    public String getClientId() { return clientId; }
    public String getName() { return name; }
    public String getClientType() { return clientType; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getAddress() { return address; }
    public void setName(String name) { this.name = name; }
    public void setClientType(String clientType) { this.clientType = clientType; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setEmail(String email) { this.email = email; }
    public void setAddress(String address) { this.address = address; }
}
