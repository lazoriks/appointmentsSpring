package com.example.appointments.controller;

import com.example.appointments.dto.AppointmentCreateDto;
import com.example.appointments.dto.AvailableDayDto;
import com.example.appointments.entity.*;
import com.example.appointments.repository.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private static final int DEFAULT_SERVICE_DURATION_MINUTES = 30;
    private static final int BOOKING_WINDOW_DAYS = 30;
    private static final int SLOT_STEP_MINUTES = 30;

    private static final int WEEKDAY_START_HOUR = 9;
    private static final int WEEKDAY_END_HOUR = 19;
    private static final int SATURDAY_START_HOUR = 9;
    private static final int SATURDAY_END_HOUR = 17;
    private static final int SUNDAY_START_HOUR = 11;
    private static final int SUNDAY_END_HOUR = 16;

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
    @Transactional(
        rollbackFor = Exception.class,
        timeout = 5
    )
    public Appointment createAppointment(@Valid @RequestBody AppointmentCreateDto dto) {

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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Main service not found"));

        // ----- CONFLICT CHECK -----
        int newDurationMinutes = resolveDurationMinutes(services, mainService);
        LocalDateTime newEnd = dt.plusMinutes(newDurationMinutes);

        LocalDateTime dayStart = dt.toLocalDate().atStartOfDay();
        LocalDateTime dayEnd = dt.toLocalDate().atTime(LocalTime.MAX);

        // Locks the master's appointments for that day for the duration of this
        // transaction, so two concurrent bookings can't both pass this check.
        List<Appointment> sameDayAppointments = appointmentRepo
                .lockByMasterIdAndDatatimeBetween(dto.getMasterId(), dayStart, dayEnd);

        boolean hasConflict = sameDayAppointments.stream().anyMatch(existing -> {
            int existingDuration = resolveDurationMinutes(existing.getServices(), existing.getService());
            LocalDateTime existingEnd = existing.getDatatime().plusMinutes(existingDuration);
            return dt.isBefore(existingEnd) && existing.getDatatime().isBefore(newEnd);
        });

        if (hasConflict) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This time slot is already booked for the selected master");
        }

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
    // TOTAL DURATION OF AN APPOINTMENT
    // ------------------------
    private int resolveDurationMinutes(List<Service> services, Service mainService) {
        if (services != null && !services.isEmpty()) {
            return services.stream()
                    .mapToInt(s -> s.getPeriod() != null ? s.getPeriod() : DEFAULT_SERVICE_DURATION_MINUTES)
                    .sum();
        }
        if (mainService != null && mainService.getPeriod() != null) {
            return mainService.getPeriod();
        }
        return DEFAULT_SERVICE_DURATION_MINUTES;
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
    @Transactional(readOnly = true)
    public List<AvailableDayDto> getAvailableSlots(
            @RequestParam("masterId") Integer masterId,
            @RequestParam(value = "serviceIds", required = false) List<Integer> serviceIds
    ) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = now.plusDays(BOOKING_WINDOW_DAYS).withHour(23).withMinute(59);

        int requestedDurationMinutes = resolveDurationMinutes(
                serviceIds != null && !serviceIds.isEmpty() ? serviceRepo.findAllById(serviceIds) : null,
                null
        );

        // EXISTING APPOINTMENTS
        List<Appointment> existing = appointmentRepo
                .findByMasterIdAndDatatimeBetween(masterId, now, endDate);

        // HOLIDAYS OF THIS MASTER
        List<Holiday> holidays = holidayRepo.findByMasterId(masterId);

        List<AvailableDayDto> result = new ArrayList<>();

        for (int i = 0; i < BOOKING_WINDOW_DAYS; i++) {
            LocalDate date = now.toLocalDate().plusDays(i);

            // SKIP HOLIDAYS FOR CLIENTS
            if (isHoliday(date, holidays)) {
                continue;
            }

            DayOfWeek dow = date.getDayOfWeek();
            int startHour, endHour;

            switch (dow) {
                case SUNDAY -> { startHour = SUNDAY_START_HOUR; endHour = SUNDAY_END_HOUR; }
                case SATURDAY -> { startHour = SATURDAY_START_HOUR; endHour = SATURDAY_END_HOUR; }
                default -> { startHour = WEEKDAY_START_HOUR; endHour = WEEKDAY_END_HOUR; }
            }

            LocalDateTime closing = date.atTime(endHour, 0);
            List<String> availableTimes = new ArrayList<>();

            for (int hour = startHour; hour < endHour; hour++) {
                for (int min = 0; min < 60; min += SLOT_STEP_MINUTES) {

                    LocalDateTime slotStart = date.atTime(hour, min).withSecond(0).withNano(0);
                    LocalDateTime slotEnd = slotStart.plusMinutes(requestedDurationMinutes);

                    // the service wouldn't fit before closing time
                    if (slotEnd.isAfter(closing)) {
                        continue;
                    }

                    boolean conflict = existing.stream().anyMatch(a -> {
                        int existingDuration = resolveDurationMinutes(a.getServices(), a.getService());
                        LocalDateTime existingEnd = a.getDatatime().plusMinutes(existingDuration);
                        return slotStart.isBefore(existingEnd) && a.getDatatime().isBefore(slotEnd);
                    });

                    if (!conflict) {
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
