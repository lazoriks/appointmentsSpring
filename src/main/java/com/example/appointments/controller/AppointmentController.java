package com.example.appointments.controller;

import com.example.appointments.dto.AppointmentCreateDto;
import com.example.appointments.dto.AvailableDayDto;
import com.example.appointments.entity.*;
import com.example.appointments.repository.*;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/appointments")
@CrossOrigin(origins = "https://glamlimerick.com")
public class AppointmentController {

    private final AppointmentRepository appointmentRepo;
    private final ClientRepository clientRepo;
    private final ServiceRepository serviceRepo;

    public AppointmentController(AppointmentRepository appointmentRepo,
                                 ClientRepository clientRepo,
                                 ServiceRepository serviceRepo) {
        this.appointmentRepo = appointmentRepo;
        this.clientRepo = clientRepo;
        this.serviceRepo = serviceRepo;
    }

    // Створення нового запису
    @PostMapping
    public Appointment createAppointment(@RequestBody AppointmentCreateDto dto) {
        LocalDateTime dt = LocalDateTime.parse(dto.getDatetime());

        Client client = clientRepo.findByMobile(dto.getClientMobile())
                .orElseGet(() -> {
                    Client newClient = new Client();
                    newClient.setFirstName(dto.getClientName());
                    newClient.setMobile(dto.getClientMobile());
                    newClient.setEmail(dto.getClientEmail());
                    return clientRepo.save(newClient);
                });

        Master master = new Master();
        master.setId(dto.getMasterId());

        List<Service> services = serviceRepo.findAllById(dto.getServiceIds());
        BigDecimal total = services.stream()
                .map(s -> BigDecimal.valueOf(s.getPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Service mainService = serviceRepo.findById(dto.getServiceId())
                .orElseThrow(() -> new RuntimeException("Main service not found"));

        Appointment appointment = new Appointment();
        appointment.setDatatime(dt);
        appointment.setClient(client);
        appointment.setMaster(master);
        appointment.setService(mainService); // старе поле
        appointment.setServices(services);   // список
        appointment.setSumm(total);

        return appointmentRepo.save(appointment);
    }

    // Пошук вільних слотів
    @GetMapping("/available")
    public List<AvailableDayDto> getAvailableSlots(
            @RequestParam("masterId") Integer masterId,
            @RequestParam("serviceIds") List<Integer> serviceIds
    ) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = now.plusDays(7).withHour(23).withMinute(59);

        List<Appointment> existing = appointmentRepo.findConflictingAppointments(
                masterId, now, endDate, serviceIds
        );

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
