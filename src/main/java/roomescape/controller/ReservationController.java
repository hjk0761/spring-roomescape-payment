package roomescape.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import roomescape.annotation.AuthenticationPrincipal;
import roomescape.controller.request.ReservationRequest;
import roomescape.controller.response.MemberReservationResponse;
import roomescape.controller.response.ReservationResponse;
import roomescape.model.Member;
import roomescape.model.PaymentInfo;
import roomescape.model.Reservation;
import roomescape.service.AuthService;
import roomescape.service.PaymentService;
import roomescape.service.ReservationService;
import roomescape.service.ReservationWaitingService;

import java.net.URI;
import java.util.List;

@RestController
public class ReservationController {

    private final ReservationService reservationService;
    private final AuthService authService;
    private final ReservationWaitingService reservationWaitingService;
    private final PaymentService paymentService;

    public ReservationController(ReservationService reservationService,
                                 AuthService authService,
                                 ReservationWaitingService reservationWaitingService,
                                 PaymentService paymentService) {
        this.reservationService = reservationService;
        this.authService = authService;
        this.reservationWaitingService = reservationWaitingService;
        this.paymentService = paymentService;
    }

    @GetMapping("/reservations")
    public ResponseEntity<List<ReservationResponse>> getReservations() {
        List<Reservation> allReservations = reservationService.findAllReservations();
        List<ReservationResponse> responses = allReservations.stream()
                .map(ReservationResponse::new)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/reservations-mine")
    public ResponseEntity<List<MemberReservationResponse>> getMemberReservations(HttpServletRequest request) {
        Long memberId = authService.getMemberIdByCookie(request.getCookies());
        List<MemberReservationResponse> responses = reservationWaitingService.getAllMemberReservationsAndWaiting(memberId);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/reservations")
    public ResponseEntity<ReservationResponse> createReservation(@RequestBody ReservationRequest request,
                                                                 @AuthenticationPrincipal Member member) {
        paymentService.confirmReservationPayments(request);
        Reservation reservation = reservationService.addReservation(request, member);
        PaymentInfo paymentInfo = paymentService.addPayment(request, reservation);
        ReservationResponse reservationResponse = new ReservationResponse(reservation);
        return ResponseEntity.created(URI.create("/reservations/" + reservation.getId())).body(reservationResponse);
    }

    @DeleteMapping("/reservations/{id}")
    public ResponseEntity<Void> deleteReservation(@PathVariable("id") long id) {
        paymentService.cancelPayment(id);
        reservationWaitingService.deleteReservation(id);
        return ResponseEntity.noContent().build();
    }
}
