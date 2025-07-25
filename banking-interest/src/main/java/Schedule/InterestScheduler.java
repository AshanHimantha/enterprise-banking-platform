package Schedule;
import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import service.InterestService;
import java.time.LocalDate;

@Singleton
@Startup
public class InterestScheduler {

    @EJB
    private InterestService interestService;

    @Schedule(hour = "2", minute = "0", second = "0", persistent = true, timezone = "Asia/Colombo")
    public void performDailyInterestTasks() {
        System.out.println("INTEREST SCHEDULER: Starting daily tasks at " + java.time.LocalDateTime.now());

        interestService.accrueDailyInterestForAllEligibleAccounts();
         LocalDate today = LocalDate.now();
         System.out.println("INTEREST SCHEDULER: It's payout day! Initiating interest payout...");
         interestService.payoutInterestForAllEligibleAccounts();
         System.out.println("INTEREST SCHEDULER: Interest payout completed for all eligible accounts.");

        System.out.println("INTEREST SCHEDULER: Daily tasks complete.");
    }
}
