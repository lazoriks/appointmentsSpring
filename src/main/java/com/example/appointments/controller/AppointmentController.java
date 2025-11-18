package com.example.appointments.controller;

import com.example.appointments.dto.AppointmentCreateDto;
import com.example.appointments.dto.AvailableDayDto;
import com.example.appointments.entity.*;
import com.example.appointments.repository.*;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/appointments")
@CrossOrigin(origins = { "https://glamlimerick.com", "http://localhost:3000" })
public class AppointmentController {

    private final AppointmentRepository appointmentRepo;
    private final ClientRepository clientRepo;
    private final ServiceRepository serviceRepo;
    private final HolidayRepository holidayRepo;

    public AppointmentController(AppointmentRepository appointmentRepo,
                                 ClientRepository clientRepo,
                                 ServiceRepository serviceRepo,
                                 HolidayRepository holidayRepo) {
        this.appointmentRepo = appointmentRepo;
        this.clientRepo = clientRepo;
        this.serviceRepo = serviceRepo;
        this.holidayRepo = holidayRepo;
    }

    // ------------------------
    // CREATE APPOINTMENT
    // ------------------------
    @PostMapping
    public Appointment createAppointment(@RequestBody AppointmentCreateDto dto) {

        LocalDateTime dt = LocalDateTime
                .parse(dto.getDatetime())
                .withSecond(0)
                .withNano(0);

        // ----- CLIENT -----
        Client client = clientRepo.findByMobile(dto.getClientMobile())
                .orElseGet(() -> {
                    Client newClient = new Client();
                    newClient.setFirstName(dto.getClientName());
                    newClient.setSurname(dto.getClientSurname());
                    newClient.setMobile(dto.getClientMobile());
                    newClient.setEmail(dto.getClientEmail());
                    newClient.setGoogleId(dto.getGoogleId());
                    return clientRepo.save(newClient);
                });

        // ----- MASTER (reference only) -----
        Master master = new Master();
        master.setId(dto.getMasterId());

        // ----- SERVICES -----
        List<Service> services = serviceRepo.findAllById(dto.getServiceIds());
        BigDecimal total = services.stream()
                .map(s -> BigDecimal.valueOf(s.getPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Service mainService = serviceRepo.findById(dto.getServiceId())
                .orElseThrow(() -> new RuntimeException("Main service not found"));

        // ----- SAVE -----
        Appointment appointment = new Appointment();
        appointment.setDatatime(dt);
        appointment.setClient(client);
        appointment.setMaster(master);
        appointment.setService(mainService);     // old single field
        appointment.setServices(services);       // list of services
        appointment.setSumm(total);

        return appointmentRepo.save(appointment);
    }

    // ------------------------
    // CHECK IF DATE IS HOLIDAY
    // ------------------------
    private boolean isHoliday(LocalDate date, List<Holiday> holidays) {
        for (Holiday h : holidays) {
            if (!date.isBefore(h.getStartDate()) && !date.isAfter(h.getFinishDate())) {
                return true;    // date is inside holiday
            }
        }
        return false;
    }

    // ------------------------
    // AVAILABLE SLOTS
    // ------------------------
    @GetMapping("/available")
    public List<AvailableDayDto> getAvailableSlots(
            @RequestParam("masterId") Integer masterId
    ) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = now.plusDays(30).withHour(23).withMinute(59);

        // EXISTING APPOINTMENTS
        List<Appointment> existing = appointmentRepo
                .findByMasterIdAndDatatimeBetween(masterId, now, endDate);

        Set<String> bookedSlots = existing.stream()
                .map(a -> a.getDatatime().truncatedTo(ChronoUnit.MINUTES).toString())
                .collect(Collectors.toSet());

        // HOLIDAYS OF THIS MASTER
        List<Holiday> holidays = holidayRepo.findByMasterId(masterId);

        List<AvailableDayDto> result = new ArrayList<>();

        for (int i = 0; i < 30; i++) {
            LocalDate date = now.toLocalDate().plusDays(i);

            // SKIP HOLIDAYS FOR CLIENTS
            if (isHoliday(date, holidays)) {
                continue;
            }

            DayOfWeek dow = date.getDayOfWeek();
            int startHour, endHour;

            switch (dow) {
                case SUNDAY -> { startHour = 11; endHour = 16; }
                case SATURDAY -> { startHour = 9; endHour = 17; }
                default -> { startHour = 9; endHour = 19; }
            }

            List<String> availableTimes = new ArrayList<>();

            for (int hour = startHour; hour < endHour; hour++) {
                for (int min = 0; min < 60; min += 30) {

                    LocalDateTime slot = date.atTime(hour, min).withSecond(0).withNano(0);

                    String slotKey = slot.truncatedTo(ChronoUnit.MINUTES).toString();

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
