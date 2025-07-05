package com.example.appointments.controller;

import com.example.appointments.dto.AppointmentInfoDto;
import com.example.appointments.entity.Appointment;
import com.example.appointments.entity.Client;
import com.example.appointments.entity.Service;
import com.example.appointments.repository.AppointmentRepository;
import com.example.appointments.repository.ClientRepository;
import com.example.appointments.repository.ServiceRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/appointments")
@CrossOrigin(origins = "*")
public class AppointmentController {
    private final AppointmentRepository repo;
    private final ClientRepository clientRepo;
    private final ServiceRepository serviceRepo;

    public AppointmentController(AppointmentRepository repo,
                                 ClientRepository clientRepo,
                                 ServiceRepository serviceRepo) {
        this.repo = repo;
        this.clientRepo = clientRepo;
        this.serviceRepo = serviceRepo;
    }

    // GET with filters
    @GetMapping("/filter")
    public List<AppointmentInfoDto> getAppointmentsByDateAndService(
            @RequestParam("datatime") String datatimeStr,
            @RequestParam("service_id") Integer serviceId) {
        LocalDateTime datatime = LocalDateTime.parse(datatimeStr);

        List<Appointment> appointments = repo.findByDatatimeAndService_Id(datatime, serviceId);

        return appointments.stream()
                .map(a -> new AppointmentInfoDto(
                        a.getId(),
                        a.getClient().getId(),
                        a.getClient().getFirstName(),
                        a.getClient().getMobile(),
                        a.getService().getId()
                ))
                .collect(Collectors.toList());
    }

    // POST to save appointment
    @PostMapping
    public Appointment createAppointment(@RequestBody Appointment newAppointment) {
        // або якщо ти хочеш просто приймати JSON з id
        // і знаходити потрібні обʼєкти:
        Service service = serviceRepo.findById(newAppointment.getService().getId())
                .orElseThrow(() -> new RuntimeException("Service not found"));
        Client client = clientRepo.findById(newAppointment.getClient().getId())
                .orElseThrow(() -> new RuntimeException("Client not found"));

        Appointment appointment = new Appointment();
        appointment.setDatatime(newAppointment.getDatatime());
        appointment.setService(service);
        appointment.setClient(client);

        return repo.save(appointment);
    }
}