package roomescape.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.controller.request.ReservationTimeRequest;
import roomescape.controller.response.IsReservedTimeResponse;
import roomescape.exception.BadRequestException;
import roomescape.exception.DuplicatedException;
import roomescape.exception.NotFoundException;
import roomescape.model.ReservationTime;
import roomescape.repository.ReservationRepository;
import roomescape.repository.ReservationTimeRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Stream;

@Transactional(readOnly = true)
@Service
public class ReservationTimeService {

    private final ReservationTimeRepository reservationTimeRepository;
    private final ReservationRepository reservationRepository;

    public ReservationTimeService(ReservationTimeRepository reservationTimeRepository,
                                  ReservationRepository reservationRepository) {
        this.reservationTimeRepository = reservationTimeRepository;
        this.reservationRepository = reservationRepository;
    }

    public List<ReservationTime> findAllReservationTimes() {
        return reservationTimeRepository.findAll();
    }

    @Transactional
    public ReservationTime addReservationTime(ReservationTimeRequest request) {
        LocalTime startAt = request.startAt();

        validateExistTime(startAt);

        ReservationTime reservationTime = new ReservationTime(startAt);
        return reservationTimeRepository.save(reservationTime);
    }

    private void validateExistTime(LocalTime startAt) {
        boolean exists = reservationTimeRepository.existsByStartAt(startAt);
        if (exists) {
            throw new DuplicatedException("이미 존재하는 시간입니다.");
        }
    }

    public ReservationTime findReservationTime(long id) {
        return findById(id);
    }

    public List<IsReservedTimeResponse> getIsReservedTime(LocalDate date, long themeId) {
        List<ReservationTime> allTimes = reservationTimeRepository.findAll();
        List<ReservationTime> bookedTimes = reservationTimeRepository.findAllReservedTimes(date, themeId);

        List<ReservationTime> notBookedTimes = filterNotBookedTimes(allTimes, bookedTimes);
        List<IsReservedTimeResponse> bookedResponse = mapToResponse(bookedTimes, true);
        List<IsReservedTimeResponse> notBookedResponse = mapToResponse(notBookedTimes, false);

        return concat(notBookedResponse, bookedResponse);
    }

    @Transactional
    public void deleteReservationTime(long id) {
        validateNotExistReservationTime(id);
        validateReservedTime(id);

        reservationTimeRepository.deleteById(id);
    }

    private void validateReservedTime(long id) {
        ReservationTime time = findById(id);

        boolean exists = reservationRepository.existsByTime(time);
        if (exists) {
            throw new BadRequestException("해당 시간에 예약이 존재하여 삭제할 수 없습니다.");
        }
    }

    private void validateNotExistReservationTime(long id) {
        findById(id);
    }

    private List<ReservationTime> filterNotBookedTimes(List<ReservationTime> times, List<ReservationTime> bookedTimes) {
        return times.stream()
                .filter(time -> !bookedTimes.contains(time))
                .toList();
    }

    private List<IsReservedTimeResponse> mapToResponse(List<ReservationTime> times, boolean isBooked) {
        return times.stream()
                .map(time -> new IsReservedTimeResponse(time.getId(), time.getStartAt(), isBooked))
                .toList();
    }

    private List<IsReservedTimeResponse> concat(List<IsReservedTimeResponse> notBookedTimes,
                                                List<IsReservedTimeResponse> bookedTimes) {
        return Stream.concat(notBookedTimes.stream(), bookedTimes.stream()).toList();
    }

    private ReservationTime findById(Long id) {
        return reservationTimeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("아이디가 %s인 예약 시간이 존재하지 않습니다.".formatted(id)));
    }
}
