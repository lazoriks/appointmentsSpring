package com.example.appointments.controller;

import com.example.appointments.dto.AppointmentInfoDto;
import com.example.appointments.dto.AvailableDayDto;
import com.example.appointments.entity.Appointment;
import com.example.appointments.entity.Client;
import com.example.appointments.entity.Service;
import com.example.appointments.repository.AppointmentRepository;
import com.example.appointments.repository.ClientRepository;
import com.example.appointments.repository.ServiceRepository;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.*;
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

    @GetMapping("/filter")
    public List<AppointmentInfoDto> getAppointmentsByDateAndService(
            @RequestParam("datatime") String datatimeStr,
            @RequestParam("service_id") Integer serviceId) {
        LocalDateTime datatime = LocalDateTime.parse(datatimeStr);

        return repo.findByDatatimeAndService_Id(datatime, serviceId).stream()
                .map(a -> new AppointmentInfoDto(
                        a.getId(),
                        a.getClient().getId(),
                        a.getClient().getFirstName(),
                        a.getClient().getMobile(),
                        a.getService().getId()
                ))
                .collect(Collectors.toList());
    }

    @PostMapping
    public Appointment createAppointment(@RequestBody Appointment newAppointment) {
        Service service = serviceRepo.findById(newAppointment.getService().getId())
                .orElseThrow(() -> new RuntimeException("Service not found"));

        Client client = clientRepo.findById(newAppointment.getClient().getId())
                .orElseThrow(() -> new RuntimeException("Client not found"));

        Appointment appointment = new Appointment();
        appointment.setDatatime(newAppointment.getDatatime());
        appointment.setService(service);
        appointment.setClient(client);
        appointment.setMaster(newAppointment.getMaster()); // don't forget master

        return repo.save(appointment);
    }

    @GetMapping("/available")
    public List<AvailableDayDto> getAvailableSlots(@RequestParam("masterId") Integer masterId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = now.plusDays(7).withHour(23).withMinute(59);

        List<Appointment> existing = repo.findByMasterIdAndDatatimeBetween(masterId, now, endDate);

        Set<String> bookedSlots = existing.stream()
                .map(a -> a.getDatatime().toLocalDate() + "T" + a.getDatatime().toLocalTime().withSecond(0).withNano(0))
                .collect(Collectors.toSet());

        List<AvailableDayDto> result = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            LocalDate date = now.toLocalDate().plusDays(i);
            DayOfWeek dow = date.getDayOfWeek();

            int startHour, endHour;
            switch (dow) {
                case SUNDAY -> { startHour = 11; endHour = 16; }
                case SATURDAY -> { startHour = 9; endHour = 17; }
                default -> { startHour = 9; endHour = 19; }
            }

            List<String> availableTimes = new ArrayList<>();

            for (int hour = startHour; hour < endHour; hour++) {
                for (int min = 0; min < 60; min += 15) {
                    LocalDateTime slot = date.atTime(hour, min);
                    String slotKey = slot.toLocalDate() + "T" + slot.toLocalTime().withSecond(0).withNano(0);
                    if (!bookedSlots.contains(slotKey)) {
                        availableTimes.add(String.format("%02d:%02d", hour, min));
                    }
                }
            }

            if (!availableTimes.isEmpty()) {
                result.add(new AvailableDayDto(date, availableTimes));
            }
        }

        return result;
    }
}
