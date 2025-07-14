package scheduler;


import dto.ScheduleRequestDTO;
import dto.ScheduledPaymentDTO;
import entity.ScheduledPayment;
import jakarta.ejb.Local;
import java.util.List;

@Local
public interface ScheduledPaymentService {

    ScheduledPaymentDTO scheduleNewPayment(String username, ScheduleRequestDTO dto);
    List<ScheduledPaymentDTO> getScheduledPaymentsForUser(String username);
    void pauseScheduledPayment(String username, Long scheduleId);
    void resumeScheduledPayment(String username, Long scheduleId);
    void cancelScheduledPayment(String username, Long scheduleId);
    List<ScheduledPayment> findDuePayments();
    void reschedulePayment(ScheduledPayment payment);
    void markPaymentAsFailed(ScheduledPayment payment, String reason);

}