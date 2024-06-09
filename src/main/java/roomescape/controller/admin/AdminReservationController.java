package roomescape.controller.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import roomescape.controller.request.AdminReservationRequest;
import roomescape.controller.response.ReservationResponse;
import roomescape.model.Reservation;
import roomescape.service.ReservationReadService;
import roomescape.service.ReservationWriteService;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
public class AdminReservationController {

    private final ReservationReadService reservationReadService;
    private final ReservationWriteService reservationWriteService;

    public AdminReservationController(ReservationReadService reservationReadService, ReservationWriteService reservationWriteService) {
        this.reservationReadService = reservationReadService;
        this.reservationWriteService = reservationWriteService;
    }

    @GetMapping("/admin/reservations")
    public ResponseEntity<List<ReservationResponse>> searchReservations(@RequestParam(value = "themeId", required = false, defaultValue = "0") Long themeId,
                                                                        @RequestParam(value = "memberId", required = false, defaultValue = "0") Long memberId,
                                                                        @RequestParam(value = "dateFrom", required = false) LocalDate dateFrom,
                                                                        @RequestParam(value = "dateTo", required = false) LocalDate dateTo) {
        List<Reservation> reservations = reservationReadService.filterReservation(themeId, memberId, dateFrom, dateTo);
        List<ReservationResponse> responses = reservations.stream()
                .map(ReservationResponse::new)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/admin/reservations")
    public ResponseEntity<ReservationResponse> createReservation(@RequestBody AdminReservationRequest request) {
        Reservation reservation = reservationWriteService.addReservation(request);
        ReservationResponse response = new ReservationResponse(reservation);
        return ResponseEntity.created(URI.create("/reservations/" + reservation.getId())).body(response);
    }
}
