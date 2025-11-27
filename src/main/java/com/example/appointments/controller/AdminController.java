package com.example.appointments.controller;

import com.example.appointments.entity.*;
import com.example.appointments.repository.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = { "https://glamlimerick.com", "http://localhost:3000" })
public class AdminController {

    private final AppointmentRepository appointmentRepo;
    private final ServiceRepository serviceRepo;
    private final MasterRepository masterRepo;
    private final ClientRepository clientRepo;
    private final GroupServiceRepository groupServiceRepo;

    public AdminController(AppointmentRepository appointmentRepo,
                           ServiceRepository serviceRepo,
                           MasterRepository masterRepo,
                           ClientRepository clientRepo,
                           GroupServiceRepository groupServiceRepo) {
        this.appointmentRepo = appointmentRepo;
        this.serviceRepo = serviceRepo;
        this.masterRepo = masterRepo;
        this.clientRepo = clientRepo;
        this.groupServiceRepo = groupServiceRepo;
    }

    // --- APPOINTMENTS ---
    @GetMapping("/appointments")
    public List<Appointment> getAppointments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        LocalDateTime start = from != null ? from.atStartOfDay() : LocalDate.now().atStartOfDay();
        LocalDateTime end = to != null ? to.atTime(23, 59) : LocalDate.now().atTime(23, 59);
        return appointmentRepo.findByDatatimeBetween(start, end);
    }

    // ✅ ДОДАНО: створення / редагування Appointment
    @PostMapping("/appointments")
    public Appointment saveAppointment(@RequestBody Appointment appointment) {
        // якщо appointment.id == null → створить новий, інакше оновить існуючий
        return appointmentRepo.save(appointment);
    }

    // ✅ ДОДАНО: видалення Appointment
    @DeleteMapping("/appointments/{id}")
    public void deleteAppointment(@PathVariable Integer id) {
        appointmentRepo.deleteById(id);
    }

    // --- SERVICES ---
    @GetMapping("/services")
    public List<Service> getAllServices() {
        return serviceRepo.findAll();
    }

    @PostMapping("/services")
    public Service saveService(@RequestBody Service service) {
        return serviceRepo.save(service);
    }

    @DeleteMapping("/services/{id}")
    public void deleteService(@PathVariable Integer id) {
        serviceRepo.deleteById(id);
    }

    // --- MASTERS ---
    @GetMapping("/masters")
    public List<Master> getAllMasters() {
        return masterRepo.findAll();
    }

    @PostMapping("/masters")
    public Master saveMaster(@RequestBody Master master) {
        return masterRepo.save(master);
    }

    @DeleteMapping("/masters/{id}")
    public void deleteMaster(@PathVariable Integer id) {
        masterRepo.deleteById(id);
    }

    // --- CLIENTS ---
    @GetMapping("/clients")
    public List<Client> getAllClients() {
        return clientRepo.findAll();
    }

    @DeleteMapping("/clients/{id}")
    public void deleteClient(@PathVariable Integer id) {
        clientRepo.deleteById(id);
    }

    // NEW: Get all appointments for a specific client
    @GetMapping("/clients/{id}/appointments")
    public List<Appointment> getClientAppointments(@PathVariable Integer id) {
        return appointmentRepo
                .findByClientIdOrderByDatatimeDesc(id);
    }

    // --- groups ---
    @GetMapping("/groups")
    public List<GroupService> getAllGroups() {
        return groupServiceRepo.findAll();
    }

    @GetMapping("/groups/{id}")
    public GroupService getGroup(@PathVariable Integer id) {
        return groupServiceRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Group not found with id: " + id));
    }

    @PostMapping("/groups")
    public GroupService saveGroup(@RequestBody GroupService group) {
        return groupServiceRepo.save(group);
    }

    @DeleteMapping("/groups/{id}")
    public void deleteGroup(@PathVariable Integer id) {
        groupServiceRepo.deleteById(id);
    }
}
