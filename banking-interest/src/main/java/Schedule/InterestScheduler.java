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

    // This schedule runs every day at 1:05 AM Colombo time.
    @Schedule(hour = "2", minute = "51", second = "30", persistent = true, timezone = "Asia/Colombo")
    public void performDailyInterestTasks() {
        System.out.println("INTEREST SCHEDULER: Starting daily tasks at " + java.time.LocalDateTime.now());

        // 1. Accrue daily interest for all eligible accounts.
        interestService.accrueDailyInterestForAllEligibleAccounts();

        LocalDate today = LocalDate.now();
//     if (today.getDayOfMonth() == today.lengthOfMonth()) {
         System.out.println("INTEREST SCHEDULER: It's payout day! Initiating interest payout...");
         interestService.payoutInterestForAllEligibleAccounts();
         System.out.println("INTEREST SCHEDULER: Interest payout completed for all eligible accounts.");
//     }

        System.out.println("INTEREST SCHEDULER: Daily tasks complete.");
    }
}
