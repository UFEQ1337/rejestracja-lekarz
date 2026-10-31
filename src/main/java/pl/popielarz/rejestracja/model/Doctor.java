package pl.popielarz.rejestracja.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "doctor")
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 60)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;

    @Column(name = "license_number", nullable = false, unique = true, length = 7)
    private String licenseNumber;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(length = 10)
    private String room;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToMany
    @JoinTable(name = "doctor_specialization",
            joinColumns = @JoinColumn(name = "doctor_id"),
            inverseJoinColumns = @JoinColumn(name = "specialization_id"))
    @OrderBy("name")
    private Set<Specialization> specializations = new LinkedHashSet<>();

    @OneToMany(mappedBy = "doctor")
    @OrderBy("startTime")
    private List<TimeSlot> slots = new ArrayList<>();

    protected Doctor() {
    }

    public Doctor(String firstName, String lastName, String licenseNumber, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.licenseNumber = licenseNumber;
        this.email = email;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public void replaceSpecializations(Set<Specialization> newSpecializations) {
        specializations.clear();
        specializations.addAll(newSpecializations);
    }

    public void deactivate() {
        this.active = false;
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public boolean isActive() {
        return active;
    }

    public Set<Specialization> getSpecializations() {
        return specializations;
    }

    public List<TimeSlot> getSlots() {
        return slots;
    }
}
